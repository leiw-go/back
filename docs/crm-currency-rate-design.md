# 外贸 CRM 汇率数据接入设计

> | 项 | 值 |
> | --- | --- |
> | 状态 | **v1.0 定稿** |
> | 分支 | `feature/crm-backend` |
> | 关联工程 | `ProjectInfomationManage`（沿用其技术栈与工程规范） |
> | 上游文档 | [`crm-backend-requirements.md`](./crm-backend-requirements.md)（CRM 后端需求总纲） |
> | 文档定位 | 汇率**数据源选型、交叉比对、入库闸门、表结构与接口**的专项设计 |
> | 规模 | 2 个外部数据源 · 4 张表 · 7 个接口端点 · 21 项配置 |

> **为什么单独成文**：汇率是 CRM 里唯一"数据完全来自外部、且错了会直接算错钱"的模块 ——
> 选型实测、偏差分布、阈值依据、熔断策略、表结构、接口细节加起来体量很大，
> 塞进主文档会淹没 CRM 本身的业务设计。本文是主文档 §3.12 / §5 的**下钻文档**。

---

## 目录

- [1. 背景与目标](#1-背景与目标)
- [2. 数据源选型](#2-数据源选型)
- [3. 角色与接口用法](#3-角色与接口用法)
- [4. 交叉比对与合理性校验](#4-交叉比对与合理性校验)
- [5. 同步机制与降级](#5-同步机制与降级)
- [6. 接口设计](#6-接口设计)
- [7. 数据库设计](#7-数据库设计)
- [8. 配置项](#8-配置项)
- [9. 与 CRM 主流程的关系](#9-与-crm-主流程的关系)
- [10. 实现落地记录](#10-实现落地记录2026-11-09-实测)
- [11. 已定决策](#11-已定决策)

---

## 1. 背景与目标

### 1.1 背景

CRM 涉及多币种：`t_opportunity.amount` / `t_quotation.total_amount` / `t_order.total_amount`
都是**原币**，另有 `*_base` 列存按汇率折算的**本位币快照**（USD）。
这些汇率如果错了，报价的毛利、业绩汇总、应收账龄会全部跟着错，且**事后很难发现**。

汇率不能自己拍脑袋定，也不能只信一个来源：

- **单个免费接口**随时可能返回错值（上游发布事故、字段串位、小数点错位）；
- **两个同源接口**互相比对只能验证"传输没坏"，验证不了"数据对不对"；
- **两个不同源的接口**才有交叉验证的价值 —— 但它们口径本就不同，差异需要**分级容忍**而不是一律拒绝。

### 1.2 目标

1. 用**免 API Key 的免费接口**拿到汇率，不引入商业数据源成本。
2. **双源交叉比对 + 合理性校验**双重闸门后才入库 —— **宁可不更新，也不能写入错值**。
3. 每次同步留**审计底账**（两源原值、偏差、判定结果），任何一条入库汇率都能回溯。
4. 外部接口挂掉时**业务不中断**：读路径只读本地表，永远不实时调外部接口。
5. 币种级的自动熔断：某个币种持续异常只停它自己，不影响其他币种。

### 1.3 范围边界

| 做 | 不做 |
| --- | --- |
| 双源交叉比对、分级阈值、自动熔断 | 银行实时结算汇率、外汇交易报价（点差另议，见 §10） |
| 每日定时同步 + 手动触发 | 实时（分钟级）汇率推送 —— 免费接口本就是每日更新 |
| 本地缓存 + 审计底账 | 把汇率数据再分发给第三方（open.er-api 条款不允许再分发） |
| 按日期取历史汇率（主源支持） | 历史汇率全量回补（需另行评估数据量与合规） |

### 1.4 名词表

| 术语 | 含义 |
| --- | --- |
| 主源 Primary | 入库取值来源 —— Frankfurter v2，各国央行参考价 |
| 比对源 Secondary | 校验来源 —— open.er-api v6，市场中间价快照 |
| 偏差 deviation | 两源汇率的百分比差异 |
| 合理性校验 | 不依赖第二源的单源合理性检查（值域、日期、环比突变等） |
| 熔断 SUSPECT | 某币种连续超标被停用，停止入库、沿用最后成功值 |
| 本位币 | 折算目标币种，本项目固定 **USD** |

---

## 2. 数据源选型

### 2.1 候选接口实测结论

2026-11-08 实际抓取验证（不是查资料）：

| 接口 | Key | 币种数 | 更新频率 | 限流 | 实测 | 结论 |
| --- | --- | --- | --- | --- | --- | --- |
| **Frankfurter v2** `api.frankfurter.dev/v2` | 不需要 | **200 个可取汇率**（币种字典 219 个法定币） | 每日（ECB 等 108 家央行） | 官方未设限流 | 200 OK / 0.3~0.6s | ✅ **主源** |
| **open.er-api v6** `open.er-api.com/v6` | 不需要 | 166 | 每日（响应带 `time_next_update_utc`） | **有限流**：官方建议 ≤1 次/24h，超限 429、约 20 分钟后重置 | 200 OK / 0.3s | ✅ **比对源**（每天 1 次，在限额内） |
| `cdn.jsdelivr.net/npm/@fawazahmed0/currency-api` | 不需要 | 258（含加密货币） | 每日（版本号钉日期，可取历史） | CDN 托管，无限流 | 200 OK / 0.26s | 🔹 第三备源 / 补历史用 |
| `api.exchangerate.host` | **现在必须要 key** | — | — | — | **401 missing_access_key** | ❌ 已排除 |
| Frankfurter **v1** `api.frankfurter.app` | 不需要 | **仅 30 个** | 每日 | 无 | 200 OK | ❌ 币种太少且已 deprecated |

### 2.2 为什么主源选 Frankfurter v2 而不是 v1

v1 只有 ECB 的 30 个币种，**缺 AED / SAR / TWD / VND 等外贸常用币**；
v2 覆盖 108 家央行、219 个法定币种（`/v2/rates` 实测可取 200 个），
`AED / SAR / TWD / CNY / KRW / VND / EGP / TRY / QAR / KWD` 全部命中。

另外 v1 官方已标注 **deprecated**（"will continue to work"，但不再维护），
新接入不应选一个已废弃的版本。

### 2.3 为什么比对源选 open.er-api v6

它是**独立的数据提供方**（市场中间价聚合），和主源（各国央行参考价）**不同源、不同口径**，
交叉比对才有意义 —— 拿两个同源接口互相比对，只能验证"传输没坏"，验证不了"数据对不对"。

它的限流约束（官方建议 ≤1 次/24h）恰好被"每天拉一次做比对"的节奏覆盖。
第三方源（jsDelivr / fawazahmed0）无限流且能钉历史日期，作为**第三备源与补历史**用。

> **合规提醒**：open.er-api 的服务条款要求**注明数据来源**（Rates by Exchange Rate API /
> exchangerate-api.com），且**不允许再分发**原始数据 —— 本项目只落自己的库供内部使用，符合要求。

### 2.4 两源实测偏差分布（2026-11-08 真实抓取）

主源与比对源**本来就不可能完全相等**：主源是各国央行的**参考价**（每日固定），
比对源是市场**中间价快照**（随时变动），二者口径本就不同。实测 158 个重叠币种：

| 指标 | 全部 158 个重叠币种 |
| --- | --- |
| 中位偏差 | **0.17%** |
| p90 | 1.00% |
| p95 | 2.43% |
| 最大偏差 | **17.83%**（SDG 苏丹镑） |

**主要外贸币种**（实测逐个比对，偏差全部在合理区间）：

| 币种 | 主源 | 比对源 | 偏差 | 币种 | 主源 | 比对源 | 偏差 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| AED | 3.6725 | 3.6725 | 0.00% | THB | 33.497 | 33.549 | 0.15% |
| SAR | 3.75 | 3.75 | 0.00% | MYR | 4.0535 | 4.0559 | 0.06% |
| INR | 94.12 | 94.1145 | 0.01% | VND | 25646 | 25602.6 | 0.17% |
| IDR | 18096 | 18097.9 | 0.01% | TRY | 50.098 | 50.172 | 0.15% |
| JPY | 158.18 | 158.318 | 0.09% | ZAR | 16.4495 | 16.5155 | 0.40% |
| HKD | 7.8464 | 7.8396 | 0.09% | MXN | 18.6602 | 18.6524 | 0.04% |
| CNY | 6.6635 | 6.6908 | 0.41% | BRL | 5.4869 | 5.5027 | 0.29% |
| EUR | 0.89169 | 0.895793 | 0.46% | RUB | 81.79 | 81.7445 | 0.06% |
| GBP | 0.74428 | 0.747473 | 0.43% | KRW | 1352.5 | 1355.38 | 0.21% |
| SGD | 1.2789 | 1.281547 | 0.21% | TWD | 32.902 | 32.847 | 0.17% |
| CHF | 0.81704 | 0.819664 | 0.32% | DKK | 6.6725 | 6.702 | 0.44% |
| EGP | 54.115 | 53.902 | 0.39% | AUD | 1.4422 | 1.454069 | 0.82% |
| PLN | 3.9258 | 3.93681 | 0.28% | CAD | 1.4004 | 1.409364 | 0.64% |
| SEK | 9.8194 | 9.889244 | 0.71% | NZD | 1.7899 | 1.805034 | 0.84% |
| **NOK** | 9.3473 | 9.449481 | **1.08%** | | | | |

> **这张表就是阈值设计的依据**：
> 主要外贸币种实测最大偏差 **1.08%**（NOK），因此**核心币种阈值取 1%** 刚好能把"正常口径差"与"真异常"分开。
> 而全部币种的 p95 达 2.43%、最大 17.83%，说明**长尾币种必须用更宽的阈值 + 自动熔断**，不能一刀切。

### 2.5 偏差大 ≠ 某个源"错了"

这是交叉比对最容易误解的地方，实测能说明问题：

| 币种 | 主源(Frankfurter) | 比对源(open.er-api) | 偏差 | 第三方源(jsDelivr) |
| --- | --- | --- | --- | --- |
| SDG 苏丹镑 | 601.13 | 510.19 | **17.83%** | 600.22（与主源仅差 0.15%） |
| IRR 伊朗里亚尔 | 1884777 | 1726408 | **9.17%** | 1893123（与主源仅差 0.44%） |
| SYP 叙利亚镑 | 122.13 | 122.01 | 0.10% | 13014（**差 99%**，平行市场汇率） |

同一个币种，**偏差大的那一侧不一定有问题** —— 只是两个提供方采用了不同的"参考价"口径
（官方固定汇率 vs 市场中间价 vs 平行市场汇率）。

**所以交叉比对的价值不是"选出一个对的"，而是"发现两个口径不一致，把这个币种标出来让人决定"。**
这正是本设计采用"分级阈值 + 自动熔断"而非"一刀切拒绝"的原因：

- 核心币种（正常口径差 < 1%）→ 偏差超标基本等于真异常 → 拒绝入库；
- 长尾币种（正常口径差可达 2~18%）→ 偏差超标只说明"两源口径不同" → 连续 3 天超标才熔断，
  期间**保留上一次成功值**并记录底账，由人决定要不要继续用这个币种。

> 顺带一个实测结论：`ZWL`（津巴布韦元）在主源里**根本没有**，
> 这类"比对源缺该币种"的情况走 `SKIP` 分支 → 单源路径 + 合理性校验 → 不入库。

### 2.6 一个必须处理的坑：两个源的"数据日期"不一致

主源返回的**每个币种有自己的 `rate_date`**（2026-11-08 实测：87 个是 11-06、70 个是 11-07、39 个是 11-08），
因为各国央行发布节奏不同；而比对源是**单一快照**（`time_last_update_utc`）。

| 坑 | 后果 | 处理 |
| --- | --- | --- |
| 主源各币种日期不一致 | 用 11-06 的数据和 11-08 的快照比，天然带 2 天时差误差 | 入库时**按该币种自己的 `rate_date` 存**，不统一成同步日 |
| 比对源只有"最新" | 无法取历史日快照做同日比对 | 比对时记录 `date_gap_days`；差值 > `max-date-gap-days` 的币种，本次**跳过比对**、只做单源合理性校验 |
| 日期差引入的额外偏差 | 正常币种也可能被误判超标 | 日期差 ≤ 阈值时按正常比对；> 阈值时**判 SKIP**（不比对、不入库，走单源路径 + 合理性校验） |

---

## 3. 角色与接口用法

### 3.1 角色定义

| 角色 | 接口 | 取值口径 | 用途 |
| --- | --- | --- | --- |
| **主源 Primary** | Frankfurter v2 | 各国央行参考价 | **入库取值**（一致时以主源为准） |
| **比对源 Secondary** | open.er-api v6 | 市场中间价快照 | **校验主源**；主源不可用时**接管取值**（标记单源） |

> 两个源**每天都调**，不再是"主源挂了才调备源"。
> 比对源每天 1 次调用，正好落在它官方建议的 "≤1 次/24h" 限额内。
> 手动触发同步时，对比对源的调用受 `min-interval-hours`（默认 24h）保护，防止触发 429。

### 3.2 接口用法

```
# ===== 主源 Frankfurter v2 =====
# 1) 拉全部币种对（基准 USD，约 200 条，12KB；每个元素自带 rate_date）
GET https://api.frankfurter.dev/v2/rates?base=USD
→ [{"date":"2026-11-08","base":"USD","quote":"CNY","rate":6.6635}, ...]

# 2) 只拉需要的币种
GET https://api.frankfurter.dev/v2/rates?base=USD&quotes=CNY,EUR,AED,SAR,TWD

# 3) 单个币种对（含历史日期，用于补历史）
GET https://api.frankfurter.dev/v2/rate/USD/CNY
GET https://api.frankfurter.dev/v2/rate/USD/CNY?date=2026-01-15
→ {"date":"2026-01-15","base":"USD","quote":"CNY","rate":6.9652}

# 4) 日期区间（补历史数据）
GET https://api.frankfurter.dev/v2/rates?base=USD&from=2026-01-10&to=2026-01-15

# 5) 币种字典（type=currency 是法定币，crypto / metal 必须过滤掉）
GET https://api.frankfurter.dev/v2/currencies?scope=all

# ===== 比对源 open.er-api v6 =====
# 全量快照（免费版只有 latest，无历史端点 —— 实测 /v6/{date}/USD 返回 404）
GET https://open.er-api.com/v6/latest/USD
→ {"result":"success","base":"USD","rates":{...},"time_last_update_utc":"..."}
```

---

## 4. 交叉比对与合理性校验

### 4.1 交叉比对算法与阈值

```
对每个币种 c（以 USD 为 base）：

  primary_rate   = 主源[c]           （含该币种自己的 rate_date）
  secondary_rate = 比对源[c]         （单一快照日）
  date_gap_days  = |主源日期 - 比对源日期|

  # 百分比差异（对称分母，避免任一源为 0 时放大）
  deviation = |primary_rate - secondary_rate| / ((primary_rate + secondary_rate) / 2) × 100%
```

**阈值（按币种分级，实测数据反推）**

| 级别 | 币种范围 | 偏差阈值 | 超过后 | 依据 |
| --- | --- | --- | --- | --- |
| **CORE** 核心币种 | USD / EUR / CNY / JPY / GBP / HKD / KRW / SGD / TWD / AUD / CAD / CHF / AED / SAR / THB / MYR / INR / RUB / BRL / MXN / VND / ZAR / TRY / PLN / NOK / SEK / DKK / EGP / PHP / IDR | **1.0%** | 拒绝入库 | 实测最大 1.08%（NOK），1% 刚好卡住 |
| **NORMAL** 一般币种 | 其余法定币种 | **3.0%** | 拒绝入库 | 全部币种 p95 = 2.43% |
| 日期差豁免 | `date_gap_days > max-date-gap-days` | **判 `SKIP`，不比对、不入库** | 走单源路径 + 合理性校验 | 避免时差误判（比"放宽阈值"更安全：放宽后仍可能把陈旧价当现价入库） |

- **CORE 币种清单可配置**（`crm.currency.cross-check.core-currencies`），按业务实际常用币调整。
- 偏差**只对"两个源都有"的币种**计算；比对源缺的币种（SYP / ZWL 等）直接判 `SKIP`，
  走单源路径 + 合理性校验，不入库（见 §4.4 决策矩阵）。

### 4.2 合理性校验（plausibility，与交叉比对互补）

交叉比对只能发现"两个源不一致"，发现不了"两个源一起错"。因此再加一层**不依赖第二源**的合理性校验：

| # | 校验项 | 规则 | 违反时 |
| --- | --- | --- | --- |
| 1 | 值域 | `min-rate ≤ rate ≤ max-rate`（默认 0.0001 ~ 100000） | 拒绝入库 |
| 2 | 正数 | `rate > 0`，非 NaN / Infinity | 拒绝入库 |
| 3 | 日期不超前 | `rate_date ≤ 今天` | 拒绝入库 |
| 4 | 日期不太旧 | `today - rate_date ≤ max-stale-days`（默认 7 天） | 拒绝入库（数据过期） |
| 5 | **环比突变** | 与库中该币种**上一条有效汇率**比，日变动幅度 > `max-daily-change`（默认 20%） | 拒绝入库 + 告警（正常汇率不会一天动 20%） |
| 6 | 三角自洽 | 用两源各取一条链，交叉算 A→B 与直接比对的偏差 > 1% | 标记该批次异常 |

> 第 5 条是**抓单源集体错误**的主力：若某天主源整体挂了一个数量级，
> 两个源对比会集体"一致"（若比对源也挂了）但环比会炸 —— 两道闸门互补。
> 环比突变命中时，若交叉比对**也**超标，则两个条件互相印证，直接拒绝并告警。

### 4.3 币种分级与自动熔断

- **静态排除**：`type != currency`（加密货币、贵金属 XAU/XAG/XPT/XPD）一律不入库。
- **动态熔断（SUSPECT）**：某币种**连续 3 个同步日**交叉比对超标 → 自动置为 `SUSPECT`，
  停止入库、沿用最后一次成功值、告警（站内信 + 飞书，见 §5.3）。
- **自动恢复**：置 `SUSPECT` 后**连续 5 个同步日**偏差回到阈值内 → 自动恢复入库。
- 熔断状态落在 `t_currency` 表的 `status` 字段（`NORMAL` / `SUSPECT` / `DISABLED`），
  恢复后历史数据**不清空**，保证历史折算不受影响。
- 熔断是**按币种**的，不影响其他币种 —— 这正是分级阈值 + 熔断比"一刀切拒绝"更实用的原因。

### 4.4 入库决策矩阵

| 交叉比对 | 合理性校验 | 决策 | 落库内容 |
| --- | --- | --- | --- |
| 通过（偏差 ≤ 阈值） | 通过 | ✅ **入库** | 取**主源值**，`source=FRANKFURTER`，`cross_status=MATCH`，记录偏差 |
| 通过 | 环比突变 | ⚠️ **入库但标记** | `cross_status=WARN`，告警，人工复核 |
| 通过 | 其他校验失败 | ❌ 拒绝该币种 | 不写新行，沿用上次成功值，`cross_status=REJECT` + 原因 |
| **超标**（偏差 > 阈值） | — | ❌ 拒绝该币种 | 不写新行，沿用上次成功值，`cross_status=REJECT` + 偏差值 + 原因 |
| **单源**（比对源缺该币种或整体失败） | 通过 | ⚠️ 入库但**未交叉验证** | `source=可用源`，`cross_status=SINGLE`，`deviation_rate=NULL` |
| 单源 | 失败 | ❌ 拒绝 | `cross_status=REJECT` |
| **两源都失败** | — | ❌ 全部不更新 | 保留库里全部旧值，批次标记 FAILED + 告警 |

**入库取值口径**：一致时**以主源（央行参考价）为准**入库，比对源的值只写进比对日志表 ——
不对两个源取平均值，因为平均值没有业务含义，反而掩盖了偏差。

> **"本次超标"与"熔断"是两件事**：本次超标 → 该币种本次不入库（沿用上次成功值）；
> 连续 3 天超标 → 触发熔断（§4.3），该币种**整体停用**并告警。
> 核心币种超标通常意味着真异常（一次就拒）；
> 一般币种超标通常只是口径不同（连续 3 次才熔断，给它机会回到正常）。

---

## 5. 同步机制与降级

### 5.1 同步机制

| 项 | 设计 |
| --- | --- |
| 同步方式 | **拉取**（pull），免费接口不提供 push |
| 定时 | 每天 **07:00** 触发一次；两源均为每日更新，频率无需更高 |

> ⚠️ **时区**：容器默认 **UTC**，若直接写 `0 0 7 * * ?` 会变成北京时间 15:00 才跑。
> 部署时必须显式设置环境变量 `TZ=Asia/Shanghai`，cron 按容器时区触发。
| 手动触发 | `POST /api/crm/currencies/sync`，用于首次初始化、补历史、主源故障后重试 |
| 调用顺序 | **先主源、后比对源**；比对源受 `min-interval-hours`（默认 24h）保护，24h 内重复触发则复用上次比对源快照 |
| 幂等 | 按 `(base_currency, quote_currency, rate_date)` **upsert**；同一日期重复同步覆盖，不产生重复行 |
| 基准币 | 统一以 **USD 为 base** 落库；本位币（折算目标）也是 USD，交叉折算 |
| 落库范围 | 只落法定币种；`SUSPECT` / `DISABLED` 币种跳过 |
| 交叉汇率 | 任意 A→B 通过 USD 三角折算：`rate(A→B) = rate(A→USD) / rate(B→USD)`，不存 N² 条 |
| 单实例 | 定时任务**单实例**执行（多副本部署时加分布式锁），避免同一批数据并发 upsert |

### 5.2 降级与容错

```
拉主源 失败 ──────────────→ 尝试拉比对源
   │                              │
   ↓ 成功                         ↓ 成功
拉比对源 ───→ 两源比对 ───→ 入库决策矩阵
   │                              │
   └────── 失败 → 单源路径 ───────┘
                  │
                  ↓
        两源都失败 → 库里旧值全部保留，批次标 FAILED，告警
```

- HTTP 429（比对源超限）→ **不重试**，直接走单源路径，并在批次日志标记 `RATE_LIMITED`。
- HTTP 5xx / 超时 → 指数退避重试（1min / 5min / 15min，最多 3 次），仍失败才降级。
- 单次同步超时上限 **10s**（主源）、**15s**（比对源）。
- 同步任务**不阻塞业务查询**：所有读接口只读本地表，绝不实时调用外部接口。
- 比对源 24h 限流保护：手动同步若距上次调用比对源不足 24h，**复用缓存的比对源快照**（落 `t_currency_sync_log` 里），
  不重复发起调用 —— 既不浪费额度，也保证手动同步仍然可用。

### 5.3 告警通道

| 通道 | 接收人 | 说明 |
| --- | --- | --- |
| **站内信**（CRM `t_message`） | 全部持有 `crm:access` 的用户 | 业务侧可见、可回溯，类型 `RATE_SYNC_FAILED` / `RATE_CURRENCY_SUSPENDED` |
| **飞书** | 运维群 | 复用现有 `FeishuBotService`；这是**独立的运维告警**，与"站内信是否外推飞书"无关 |

> 站内信本身**不做邮件/飞书外推**（见 CRM 主文档 §3.11），
> 这里的飞书是汇率同步任务直接发出的运维告警，两者互不影响。

---

## 6. 接口设计

基础路径 `/api/crm/currencies`，权限统一为 `crm:access`（CRM 主文档 §2.2）。

> **分页**：列表类端点统一用 **PageHelper**（`startPage` + `PageInfo`），
> 约定见 CRM 主文档 §4.1.1 —— 排序字段走白名单，深分页上限 `page * size ≤ 10000`。
> `sync-status` 与 `verification` 返回的是汇总/明细数组，**不分页**（单批最多 ~200 条）。

| 方法 | 路径 | 说明 | 权限 |
| --- | --- | --- | --- |
| GET | `/currencies` | 币种列表（来自 `t_currency`，含 `status` 熔断状态） | `crm:access` |
| GET | `/currencies/rates` | 汇率列表（可指定基准币与日期；**只读本地表**，不实时调外部接口） | `crm:access` |
| PUT | `/currencies/rates` | 手工维护汇率（`source = MANUAL`；同步任务不覆盖 MANUAL 行） | `crm:access` |
| GET | `/currencies/convert` | 金额折算（金额 + 源币 + 目标币 + 日期 → 折算值与汇率；交叉汇率走 USD 三角折算） | `crm:access` |
| POST | `/currencies/sync` | **手动触发汇率同步**（拉主源 + 比对源 → 交叉比对 → 合理性校验 → 入库） | `crm:access` |
| GET | `/currencies/sync-status` | 最近一次同步批次摘要（两源状态、比对统计、拒绝数、最大偏差 Top5、是否陈旧） | `crm:access` |
| GET | `/currencies/rates/verification` | **交叉比对明细**（可指定日期 / 币种；返回两源原值、偏差、比对结果与拒绝原因） | `crm:access` |

> `POST /currencies/sync` 的返回体（关键字段）：
> ```json
> {
>   "syncId": "…", "status": "SUCCESS",
>   "primary": { "status": "OK", "rows": 158, "date": "2026-11-08" },
>   "secondary": { "status": "OK", "rows": 158, "date": "2026-11-08" },
>   "crossCheck": {
>     "compared": 158, "matched": 151, "warned": 2, "rejected": 5, "singleSource": 0,
>     "maxDeviation": { "currency": "NOK", "rate": 1.0813 },
>     "rejectedList": [ { "currency": "SDG", "deviation": 17.83, "reason": "DEVIATION_EXCEEDED" } ]
>   },
>   "saved": 151, "skipped": 7
> }
> ```

---

## 7. 数据库设计

沿用 CRM 主文档 §5.1 的红线：**MySQL 8 / utf8mb4 / 无物理外键 / 逻辑删除 / 时间戳带 ON UPDATE**。

### 7.1 `t_currency` 币种字典

| 字段 | 类型 | 空 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| id | VARCHAR(36) | N | | 主键 |
| currency_code | CHAR(3) | N | | ISO 4217 三字母码 |
| currency_name | VARCHAR(64) | N | | 币种名称 |
| symbol | VARCHAR(16) | Y | | 符号 |
| numeric_code | CHAR(3) | Y | | ISO 4217 数字码 |
| is_common | TINYINT | N | 0 | 常用币标记（前端下拉排序） |
| currency_level | VARCHAR(16) | N | 'NORMAL' | `CORE` / `NORMAL`（分级阈值，见 §4.1） |
| status | VARCHAR(16) | N | 'NORMAL' | `NORMAL` / `SUSPECT`（熔断中）/ `DISABLED`（停用） |
| suspect_days | INT | N | 0 | 连续超标天数（熔断判定用） |
| recover_days | INT | N | 0 | 连续达标天数（恢复判定用） |

索引：`uk_currency_code(currency_code)` 唯一、`idx_common(is_common)`、`idx_status(status)`

### 7.2 `t_currency_rate` 汇率

| 字段 | 类型 | 空 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| id | VARCHAR(36) | N | | 主键 |
| base_currency | CHAR(3) | N | | 基准币（统一 USD） |
| quote_currency | CHAR(3) | N | | 目标币 |
| rate | DECIMAL(18,8) | N | | 汇率 |
| rate_date | DATE | N | | 汇率日期（**按该币种自己的源日期**，不统一成同步日） |
| source | VARCHAR(16) | N | | `FRANKFURTER` / `OPEN_ER_API` / `MANUAL` |
| cross_source | VARCHAR(16) | Y | | 比对源标识（`OPEN_ER_API` / `FRANKFURTER`） |
| cross_status | VARCHAR(16) | N | | `MATCH` / `WARN` / `SINGLE` / `REJECT` / `MANUAL` |
| deviation_rate | DECIMAL(12,6) | Y | | 与比对源的偏差（%），单源入库时为空 |
| date_gap_days | INT | N | 0 | 两源数据日期差（天） |
| checked_at | DATETIME | Y | | 交叉比对时间 |

索引：`uk_pair_date(base_currency, quote_currency, rate_date)` 唯一、`idx_quote_date(quote_currency, rate_date)`、`idx_cross_status(cross_status)`

> 汇率表由**同步任务**写入，迁移脚本只建表、不灌初始汇率。
> 手工维护的汇率 `source = MANUAL`，同步任务**不覆盖** `MANUAL` 行 —— 应急修数不会被次日同步冲掉。
> 数据量：200 币种 × 每日 1 条 ≈ 200 行/天，一年约 7 万行，`DECIMAL(18,8)` 下完全可接受，保留全量历史以便任意日期折算。
> `cross_status = REJECT` 的行**不落库**（拒绝即不写），拒绝记录在 `t_currency_rate_check` 里。

### 7.3 `t_currency_sync_log` 同步批次日志（一次同步 = 一行）

| 字段 | 类型 | 空 | 说明 |
| --- | --- | --- | --- |
| id | VARCHAR(36) | N | 主键 = syncId |
| trigger_type | VARCHAR(16) | N | `SCHEDULE` / `MANUAL` |
| primary_status | VARCHAR(16) | N | `OK` / `FAILED` / `RATE_LIMITED` / `SKIPPED` |
| primary_rows | INT | N | 主源拉到的条数 |
| primary_date | DATE | Y | 主源数据日期（多日期时取最新） |
| secondary_status | VARCHAR(16) | N | `OK` / `FAILED` / `RATE_LIMITED` / `REUSED`（复用缓存）/ `SKIPPED` |
| secondary_rows | INT | N | 比对源条数 |
| secondary_date | DATE | Y | 比对源快照日期 |
| compared_count | INT | N | 实际参与比对的币种数 |
| matched_count | INT | N | 一致数 |
| warned_count | INT | N | 警告数（入库但标记） |
| rejected_count | INT | N | 拒绝数 |
| single_count | INT | N | 单源数 |
| saved_count | INT | N | 实际入库数 |
| max_deviation | DECIMAL(12,6) | Y | 本次最大偏差（%） |
| max_deviation_currency | CHAR(3) | Y | 最大偏差对应币种 |
| sync_status | VARCHAR(16) | N | `SUCCESS` / `PARTIAL` / `FAILED` |
| error_msg | VARCHAR(1024) | Y | 失败原因 |
| duration_ms | INT | Y | 耗时 |
| started_at | DATETIME | N | 开始时间 |
| finished_at | DATETIME | N | 结束时间 |

索引：`idx_started(started_at)`、`idx_status(sync_status)`

### 7.4 `t_currency_rate_check` 交叉比对明细（一次同步 × 一个币种 = 一行）

| 字段 | 类型 | 空 | 说明 |
| --- | --- | --- | --- |
| id | VARCHAR(36) | N | 主键 |
| sync_id | VARCHAR(36) | N | 关联 `t_currency_sync_log.id` |
| base_currency | CHAR(3) | N | 基准币 |
| quote_currency | CHAR(3) | N | 目标币 |
| primary_rate | DECIMAL(18,8) | Y | 主源值 |
| primary_date | DATE | Y | 主源该币种的日期 |
| secondary_rate | DECIMAL(18,8) | Y | 比对源值 |
| secondary_date | DATE | Y | 比对源快照日期 |
| deviation_rate | DECIMAL(12,6) | Y | 偏差（%） |
| date_gap_days | INT | N | 日期差 |
| threshold | DECIMAL(12,6) | Y | 本次适用的阈值 |
| check_result | VARCHAR(16) | N | `MATCH` / `WARN` / `REJECT` / `SKIP` / `SINGLE` |
| reject_reason | VARCHAR(64) | Y | `DEVIATION_EXCEEDED` / `RATE_OUT_OF_RANGE` / `DATE_IN_FUTURE` / `DATE_TOO_OLD` / `DAILY_CHANGE_TOO_LARGE` / `NOT_IN_SECONDARY` / `SUSPECT_CURRENCY` |
| checked_at | DATETIME | N | 比对时间 |

索引：`uk_sync_currency(sync_id, quote_currency)` 唯一、`idx_quote(quote_currency)`、`idx_result(check_result)`、`idx_checked(checked_at)`

> 保留期：比对明细行数 = 每次同步 ~160 行 × 365 天 ≈ 5.8 万行/年，**全量保留**（审计要求）；
> 若后续要瘦身，按 `checked_at` 归档到冷表而不是删除 —— 历史折算的口径争议要能回溯。

### 7.5 关键索引与查询场景

| 场景 | 命中索引 | 说明 |
| --- | --- | --- |
| 取某日汇率 | `uk_pair_date(base_currency, quote_currency, rate_date)` | 折算本位币时按日期精确命中 |
| 取最新汇率 | `idx_quote_date(quote_currency, rate_date)` | 按日期倒序取第一条 |
| 查某次同步的比对明细 | `uk_sync_currency(sync_id, quote_currency)` | 按批次定位每个币种的比对结果 |
| 查可疑币种（连续超标） | `idx_result(check_result)` | 运维排查"哪些币种被拒了" |
| 熔断状态判定 | `idx_status(status)` + `t_currency.suspect_days` | 连续超标 / 连续达标计数 |

### 7.6 迁移

在 CRM 的 V2 迁移里一并建表（接续现有 `V1__init_project_info_manage.sql`）：

```sql
-- 4 张空表，无物理外键，无初始汇率数据
CREATE TABLE `t_currency` (...) ;
CREATE TABLE `t_currency_rate` (...) ;
CREATE TABLE `t_currency_sync_log` (...) ;
CREATE TABLE `t_currency_rate_check` (...) ;
```

> **汇率初始数据不写进迁移** —— 迁移只建空表；
> 首次同步由 `POST /api/crm/currencies/sync` 触发（或等定时任务 07:00 首跑）。

---

## 8. 配置项

| 配置 | 默认值 | 说明 |
| --- | --- | --- |
| `crm.currency.sync.enabled` | `true` | 是否开启定时同步 |
| `crm.currency.sync.cron` | `0 0 7 * * ?` | 每日 07:00 |
| `crm.currency.sync.base-currency` | `USD` | 基准币种 |
| `crm.currency.sync.primary-url` | `https://api.frankfurter.dev/v2/rates?base=USD` | 主源（汇率） |
| `crm.currency.sync.primary-currency-url` | `https://api.frankfurter.dev/v2/currencies?scope=all` | 主源（币种字典，用于过滤加密货币） |
| `crm.currency.sync.secondary-url` | `https://open.er-api.com/v6/latest/USD` | 比对源（主源失败时接管取值） |
| `crm.currency.sync.timeout-ms` | `10000` | 单次请求超时（汇率模块独立 HTTP 客户端，不复用通用 5s 超时的 RestTemplate） |
| `crm.currency.sync.quotes` | 空（=全部法定币种） | 只同步指定币种时用 |
| `crm.currency.sync.secondary.min-interval-hours` | `24` | 比对源最小调用间隔（防 429） |
| `crm.currency.cross-check.enabled` | `true` | 是否开启交叉比对 |
| `crm.currency.cross-check.core-currencies` | 见 §4.1 的 CORE 清单 | 核心币种（**唯一真相源**；`t_currency.currency_level` 由同步任务按此配置回写，不在迁移里硬编码） |
| `crm.currency.cross-check.threshold-core` | `1.0` | 核心币种偏差阈值（%） |
| `crm.currency.cross-check.threshold-normal` | `3.0` | 一般币种偏差阈值（%） |
| `crm.currency.cross-check.max-date-gap-days` | `3` | 超过则判 SKIP（跳过比对，走单源路径 + 合理性校验） |
| `crm.currency.cross-check.suspect-days` | `3` | 连续超标几天判定 SUSPECT |
| `crm.currency.cross-check.recover-days` | `5` | 连续达标几天解除 SUSPECT |
| `crm.currency.sync.currency-stale-days` | `30` | 币种字典里"最后一次有数据"距今超过该天数视为已废止，不入库 |
| `crm.currency.plausibility.min-rate` | `0.0001` | 汇率下界 |
| `crm.currency.plausibility.max-rate` | `100000` | 汇率上界 |
| `crm.currency.plausibility.max-daily-change` | `20` | 单日环比突变阈值（%） |
| `crm.currency.plausibility.max-stale-days` | `7` | 数据最大可接受陈旧天数 |
| `crm.currency.base-currency` | `USD` | 本位币（`*_base` 列的折算目标） |

> 所有配置走 `application.yml` 的 `${ENV:default}` 占位，由环境变量注入。
> 本工程**没有配置中心**，改这些值需要重启容器 —— 与现有工程约定一致。

---

## 9. 与 CRM 主流程的关系

CRM 主文档里只有三处直接依赖汇率，其余细节都在本文：

| CRM 侧 | 依赖方式 |
| --- | --- |
| **一单一币种** | 报价单 / 订单的币种在**单头**，明细行不存币种（主文档 §5.1 原则 5） |
| **本位币折算** | `t_opportunity.amount_base` / `t_quotation.total_amount_base` / `t_order.total_amount_base` 在**创建时快照汇率**，之后汇率变动**不回溯** |
| **汇率不可用时** | 同步失败时库里保留旧值，`/currencies/convert` 照常返回**最近一次成功汇率** —— 业务不中断（可能不是最新值，需在报表上标注数据日期） |

> **报价自动算价与汇率无关**：报价单价 = 成本价 × (1 + 加价率) × (1 - 折扣率)，
> 成本价与报价单**同币种**时不需要汇率换算（主文档 §3.5.1）；
> 币种不一致时**直接拒绝建明细**（主文档 §1.3 已定案），不做隐式换算。

---

## 10. 实现落地记录（2026-11-09 实测）

> 这一节记录**落地时才发现的、与上文设计有出入的地方** —— 以本节为准。

### 10.1 主源的汇率接口会混入加密货币 ⚠️

§4.3 说"静态排除 `type != currency`"，实测发现**光看币种字典不够**：

| 接口 | 实测结果 |
| --- | --- |
| `GET /v2/rates?base=USD` | **200 条**，其中含 `AAVE` / `ADA` / `ALGO` / `ATOM` / `AVAX` / `HBAR` 等 **4 位码的加密货币** |
| `GET /v2/currencies?scope=all` | 258 条 = 219 `currency` + 35 `crypto` + 4 `metal` |

`t_currency.currency_code` 是 `CHAR(3)`，4 位码直接写不进去（`Data too long for column 'currency_code'`）。

**处理**：同步前先取币种字典，用 `type = currency` 的码集合过滤汇率，
实测 200 条 → **161 条法定币种**入库；字典取不到时退化为"只认 3 位码"的保守规则。

### 10.2 币种字典的 `end_date` 不是废止日期

字典里每个币种都有 `end_date`，**在用币种的 `end_date` 就是"最后一次有数据的日期"（昨天）**，
而不是废止日期；真正废止的币种（如 `ZWL`）的 `end_date` 会是很久以前。

**处理**：用 `crm.currency.sync.currency-stale-days`（默认 30 天）做窗口判定 ——
`end_date` 距今超过该窗口才算废止、不入库。

### 10.3 比对源快照日期的解析

比对源的时间戳有两种形态：`time_last_update_unix`（秒级）与
`time_last_update_utc`（形如 `Sun, 08 Nov 2026 00:02:31 +0000`，**不是** `yyyy-MM-dd`）。
实现上**优先用 unix 时间戳**（UTC 换算日期），文本格式只作兜底。

### 10.4 日期差超限与单源路径的落地口径

§4.1 的"日期差豁免 → 不入库"与 §4.4 的"单源 + 合理性校验通过 → 入库但未交叉验证"
在 §2.5 / §2.6 里各说了一半。**落地按 §4.4 的决策矩阵执行**：

| 交叉比对结果 | 落地行为 | `cross_status` |
| --- | --- | --- |
| 偏差 ≤ 阈值 | 入库 | `MATCH` |
| 偏差 ≤ 阈值但环比突变 | 入库（需人工复核） | `WARN` |
| 偏差 > 阈值 | **不入库**，记录拒绝明细 | `REJECT` |
| 日期差 > 上限 | 走单源路径 + 合理性校验，通过则入库 | `SINGLE` |
| 比对源缺该币种 / 比对源整体失败 | 走单源路径 + 合理性校验，通过则入库 | `SINGLE` |

拒绝 / 跳过 / 单源三种情况都**逐币种**写 `t_currency_rate_check`（审计底账）。

### 10.5 首次同步的实测结果

```
primaryStatus=OK  primaryRows=161      secondaryStatus=OK  secondaryRows=166
compared=158  matched=152  warned=0  rejected=6  single=3  saved=155
maxDeviation=16.366989% (SDG)
```

被拒的 6 个全是长尾币种（`SDG` 16.37%、`IRR` 8.96%、`BWP` 4.59%、`SSP` 4.40%、
`SCR` 4.50%、`PYG` 4.14%），与 §2.4 的实测分布（SDG 17.83%、IRR 9.17%）吻合 ——
**说明偏差公式与阈值分级都是对的**。

---

## 11. 已定决策

> 本模块的所有开放问题已定案（与 CRM 主文档 §1.3 决策速查表保持一致）。

| 议题 | 决定 |
| --- | --- |
| **偏差阈值** | 核心币种 **1%**、一般币种 **3%** —— 依据 §2.4 实测分布反推，不再调整 |
| **核心币种清单** | **只以配置项** `crm.currency.cross-check.core-currencies` 为准（默认 §4.1 的 30 个），`t_currency.currency_level` 由同步任务按此回写，**不在迁移里硬编码** |
| **点差（spread）** | **本期不做** —— 汇率按参考中间价直接使用；对外报价的点差由业务员在**加价率**里体现（主文档 §3.5.1） |
| **本位币** | 固定 **USD**（配置项 `crm.currency.base-currency` 可改，但本期不改；历史数据按 USD 口径） |
| **数据保留期** | `t_currency_rate` 与 `t_currency_rate_check` **全量保留**，不归档、不清理（审计要求，一年各约 7 万 / 5.8 万行） |
| **成本币种不一致** | 直接拒绝建报价明细，**不做隐式换算**（主文档 §1.3） |
