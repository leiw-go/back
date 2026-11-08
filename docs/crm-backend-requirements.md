# 外贸 CRM 后端需求文档（MVP · 可执行版）

> | 项 | 值 |
> | --- | --- |
> | 状态 | **v1.0 定稿**（不再有开放问题，开发照此实施） |
> | 分支 | `feature/crm-backend` |
> | 关联工程 | `ProjectInfomationManage`（沿用其技术栈与工程规范） |
> | 专项文档 | [汇率数据接入设计](./crm-currency-rate-design.md) —— 汇率选型/比对/表结构细节 |
> | 规模 | 12 个业务模块 · 79 个接口端点 · 24 张新表 · 1 个权限点 |

---

## 目录

- [0. 一页速览](#0-一页速览)
- [1. 范围与决策](#1-范围与决策)
- [2. 角色与权限](#2-角色与权限)
- [3. 功能设计](#3-功能设计)
- [4. 接口设计](#4-接口设计)
- [5. 数据库设计](#5-数据库设计)
- [6. 实施顺序](#6-实施顺序)
- [7. 非功能需求](#7-非功能需求)
- [8. 附录](#8-附录)

---

## 0. 一页速览

| 项 | 内容 |
| --- | --- |
| 业务目标 | 把"询盘 → 报价 → 订单 → 发货 → 收款"做成结构化单据链，多币种自动折算，业务事件站内通知 |
| 当前使用者 | **1 名业务员**（因此权限、数据隔离、团队组织全部从简） |
| 技术栈 | Spring Boot 3.2.4 · Spring MVC · MyBatis 3.0.3 + **PageHelper 2.1.1** · **EasyExcel 4.0.3** · MySQL 8 · Flyway 9.22.3 · JWT · springdoc-openapi |
| 接口风格 | `/api/crm/**`（外部访问 `/design/api/crm/**`），统一 `ResponseResult<T>` 包装，HTTP 恒 200 |
| 数据库 | 24 张新表，**无物理外键**（逻辑 mapping），Flyway 版本化迁移 |
| 关键取舍 | 权限只做 1 个点、不做数据隔离、不做跟进活动/任务/客户合并/单据中心、不做多语言、不做细分错误码 |
| 实施顺序 | M1 客户建档 → M2 商机产品 → M3 询盘报价 → M4 订单收款 → M5 邮件汇率站内信 |

---

## 1. 范围与决策

### 1.1 本期做

| 模块 | 内容 |
| --- | --- |
| 客户 | 建档、去重、标签、负责人归属、导出 |
| 联系人 | 邮箱/电话（E.164）、唯一性校验 |
| 商机 | 六阶段漏斗、金额与赢率、转订单 |
| 询盘 | RFQ 记录、时效提醒 |
| 报价 | 版本、一单一币种、**成本价 + 加价率自动算价**、利润汇总 |
| 产品 | 目录、成本价、重量体积、多币种参考价、MOQ |
| 订单 | 状态机、定金/尾款拆分、发货、应收、核销 |
| 邮件 | 模板、群发任务、发送记录 |
| 站内信 | 业务事件通知、未读角标 |
| 汇率 | 双源交叉比对后自动同步（见专项文档） |
| 看板 | 漏斗、趋势、客户分析、账龄、产品排行 |

### 1.2 本期不做（明确砍掉，后续迭代再补）

| 砍掉的 | 原因 / 替代 |
| --- | --- |
| **客户合并** | 重复客户靠 `uk_company_norm` 唯一约束在建档时拦掉 |
| **跟进活动（Activity）** | "最近联系时间"由关联单据的 `update_time` 近似 |
| **任务（Task）** | 待办类需求由站内信 + 询盘时效提醒覆盖 |
| **单据中心（Document）** | 本期只跑通"订单 → 发货 → 应收"；**连带没有文件上传功能**，补单据时要一并补上传规范 |
| **多语言（i18n）** | 系统文案一律中文；仅客户域自由文本可含外文（§8.3） |
| **业务细分错误码** | 沿用全局 HTTP 语义，业务提示走自由中文文案 |
| **团队 / 组织 / 数据隔离** | 只有 1 个用户；`owner_id` 已保留，后续加过滤不改表 |
| **细粒度权限** | 只留 `crm:access` 一个权限点 |
| **异步导出 / 幂等键 / 审计表 / 消息模板表** | 遵循"如无必要，勿增实体"；导出为同步 Excel 且限 1 万行，幂等靠唯一索引 + 状态机，审计写结构化日志 |

### 1.3 关键决策速查表（曾经开放的问题，现已定案）

| 议题 | 决定 |
| --- | --- |
| 加价率口径 | **成本加成法**：`单价 = 成本价 × (1 + 加价率) × (1 - 折扣率)` |
| 成本价币种 | `t_product.cost_currency` 显式声明；**报价时要求成本币种 == 报价单币种**，不等则拒绝 |
| MOQ | 报价明细行数量必须 ≥ 产品 `moq`，否则 400 |
| 阶梯价（`t_product_price.min_quantity`） | 仅作**参考销售价**，**不参与**自动算价 |
| 询盘目标价 `target_price` | 只展示给业务员参考，**不参与**定价 |
| 客户去重 | **强唯一** `uk_company_norm(country_code, company_name_norm, deleted)` |
| E.164 号码唯一性 | **同一客户内唯一**（跨客户允许重复 —— 同一采购代理可能对接多家买家） |
| 收款流水 | **不建流水表**，只维护 `received_amount` 累计值，每次核销写结构化日志 |
| 导出 | **同步导出 Excel `.xlsx`**（EasyExcel 4.0.3 / POI 5.2.5），单次上限 1 万行，超出报 400 提示缩小范围；金额写**数字**而非字符串，便于在 Excel 里直接求和 |
| 订单状态机 | 见 §3.7；`SHIPPED` = 首次发货，`DELIVERED` = 实际到港，`COMPLETED` = 已发货且全额收款 |
| 商机赢单 | 转订单时**自动置 `WON`**，商机**保留**（看板按当前 stage 快照统计赢单率） |
| 询盘 `first_reply_at` | **报价单创建时回填** |
| 应收逾期提醒 | 每天一次，**同一笔应收每天最多一条**提醒（按 `order_id + due_date` 去重） |
| 站内信已读保留 | 90 天后定时清理，未读不清理 |
| 产品分类 | 走字典 `product_category` |
| 排序字段 | **白名单**：每个列表接口显式列出允许排序的列，其余参数忽略 |
| 深分页 | `page × size ≤ 10000`，超出报 400 |
| 现有 `LotteryPeriodMapper` | **一并迁移**到 PageHelper（避免两套分页写法并存） |
| 定时任务并发 | 当前**单副本部署**，不加分布式锁；扩到多副本时再引入 |
| 接口版本化 | 不做版本号；`/api/crm/**` 直接演进 |
| 汇率偏差阈值 | 核心币种 1%、一般币种 3%（依据实测分布反推，见专项文档 §4.1） |
| 汇率核心币种清单 | **只以配置项** `crm.currency.cross-check.core-currencies` 为准 |

---

## 2. 角色与权限

### 2.1 账号体系（复用现有）

CRM **不新建用户表**：

- 登录 / 鉴权 / Token 全部走现有的 `AuthController#login` + `AuthInterceptor` + `JwtUtil`。
- 复用现有 5 张表：`t_user` / `t_role` / `t_permission` / `t_user_role` / `t_role_permission`。
- CRM 侧只往 `t_permission` **追加一个权限点** `crm:access`，再授给业务员所在角色。
- CRM 表里的 `owner_id` / `created_by` 直接存 `t_user.id`。

### 2.2 权限判定

| 用户是否拥有 `crm:access` | 结果 |
| --- | --- |
| 有 | 可调用 `/api/crm/**` 下**任意**接口（读 / 写 / 删除 / 导出，不细分） |
| 无 | 全部返回 403（`ForbiddenException`，文案"无 CRM 使用权限"） |

实现：沿用 `AuthInterceptor`，为 `/api/crm/**` 加一次权限点检查，**不逐接口声明权限注解**。

### 2.3 数据可见性

- `owner_id` 是**业务归属**（这客户归谁跟），第一版**不做隔离** —— 所有接口返回全部数据。
- 后续要隔离时，只在 Service 列表查询里拼 `owner_id` 条件，**不改表结构**。

---

## 3. 功能设计

### 3.1 客户（Customer）

一个买家企业（`t_customer`）+ 其下多个联系人。

- **建档**：公司全称 / 简称、国家地区、地址、网站、行业、来源渠道、客户等级（A/B/C）、负责人。
- **去重**：`company_name` 归一化（去空格 + 转大写）写入 `company_name_norm`，
  与 `country_code` 组成**唯一约束**，重复建档直接被数据库拦下。
- **归属**：必须有负责人；支持转交（改 `owner_id`），**不记录**转交轨迹。
- **标签**：自由标签（行业特征、偏好品类、展会来源等），用于筛选与群发。
- **状态**：`POTENTIAL` 潜在 → `FOLLOWING` 跟进中 → `DEALED` 已成交 → `LOST` 流失。
- **合并**：本期不做。
- `last_contact_at` **预留**，本期由关联单据的 `update_time` 近似回填。

### 3.2 联系人（Contact）

- 归属一个客户，一个客户下多个联系人。
- 字段：姓名、职位、邮箱、电话（E.164）、WhatsApp（E.164）、语言、时区、是否主要联系人。
- **邮箱唯一**：同一客户下邮箱唯一（应用层校验 + `uk_customer_email(customer_id, email, deleted)`）。
- **电话唯一**：同一 E.164 号码**在同一客户内**只能属于一个联系人（应用层校验）。
- `language` 只是沟通语言的**参考标记**，**不驱动任何系统文案切换**。

### 3.3 商机（Opportunity）

- 一个商机属于一个客户：名称、预计金额 + 币种、阶段、赢率、预计成交日、竞争对手、负责人。
- **阶段机**（唯一合法流转方向）：

```
INITIAL_CONTACT 初步接触 → QUALIFIED 需求确认 → QUOTED 已报价
    → NEGOTIATION 商务谈判 → WON 赢单
                       ↘ LOST 丢单（必须填 lost_reason）
```

- 阶段变更**不记录流转轨迹**（本期无活动表），只保留当前 `stage`。
- 赢单时可一键生成订单：自动置商机为 `WON`，携带商机金额与客户。
- 赢单率 = `stage='WON'` 的商机数 ÷ (`WON` + `LOST`) 的商机数，**按当前快照**统计。

### 3.4 询盘（Inquiry / RFQ）

- 记录客户发来的采购需求：客户、联系人、产品、数量、目标价 + 币种、期望交期、来源（邮件 / 展会 / 官网 / 阿里）、原始需求内容。
- 一个询盘可对应多个报价单（不同版本、不同币种）。
- **时效提醒**：创建后超过 24h 仍未产生报价单 → 进入"待处理询盘"列表并发站内信。
- `first_reply_at` 在**报价单创建时**回填。

### 3.5 报价（Quotation）

- **单头**：报价单号、客户、询盘、商机、版本号、币种、贸易术语、加价率、折扣率、有效期、状态、金额与利润汇总。
- **明细**：产品、规格、数量、成本单价快照、单价、金额。
- **状态机**：`DRAFT` → `SENT` → `CONFIRMED` / `REJECTED`；`DRAFT`/`SENT` 过期后视为 `EXPIRED`（查询时按 `valid_until` 判定，不依赖定时任务）。
  - 只有 `DRAFT` 可改；`SENT` 及之后只读。
  - 转 `REJECTED` 必须填 `reject_reason`。
- **版本**：同一询盘下 `version_no` 递增，历史版本保留不覆盖。唯一键 `(inquiry_id, currency_code, version_no, deleted)` —— 含币种是为了允许同一询盘同时报 USD 与 EUR。
- **一单一币种**：币种只在单头，明细行不存币种；换币种要新建报价单。
- **本位币快照**：`total_amount_base` 在**创建时**按当日汇率快照，之后汇率变动不回溯。

#### 3.5.1 自动算价（核心）

报价单价**由成本价 + 加价率算出**，不手工填价。

```
行单价   unit_price    = 成本单价 cost_price × (1 + 加价率 markup_rate/100) × (1 - 折扣率 discount_rate/100)
行金额   amount        = round(unit_price × quantity, 2)
报价总额 total_amount  = Σ 行金额
成本汇总 total_cost    = Σ (cost_price × quantity)
毛利额   profit_amount = total_amount - total_cost
毛利率   profit_rate   = total_amount > 0 ? round(profit_amount / total_amount × 100, 2) : 0
```

| 规则 | 内容 |
| --- | --- |
| 加价率 | 报价单级默认 `markup_rate`，明细可覆盖；0~1000 |
| 折扣率 | 报价单级默认 `discount_rate`，明细可覆盖；0~100 |
| 成本单价 | 建明细时从 `t_product.cost_price` **快照**到明细行，产品改价不影响历史报价 |
| 币种校验 | 建明细时要求 `t_product.cost_currency == 报价单.currency_code`，不等直接 400 |
| 成本价缺失 | `t_product.cost_price` 为空 → 该产品不能参与报价，建明细时拦截 |
| MOQ | 明细数量必须 ≥ `t_product.moq`，否则 400 |
| 手工改价 | 允许覆盖 `unit_price`，但记 `manual_price = 1`，且不再随加价率/折扣重算 |
| 精度 | 单价 `DECIMAL(18,6)`，金额 `DECIMAL(18,2)`，比例 `DECIMAL(8,2)`；舍入规则见 §5.5 |
| **服务端重算** | 提交时服务端**重新计算**每行 `unit_price` / `amount` 与单头汇总，**不信任前端传来的值**（`manual_price=1` 的行除外） |
| 试算 | `POST /quotations/calculate` 只算不落库，供前端实时预览 |

### 3.6 产品与价格（Product）

- 产品：编码、名称、分类（字典 `product_category`）、规格、单位、重量(kg)、体积(CBM)、**成本价 + 成本币种**、默认销售价、默认币种、MOQ、HS 编码、产地、状态。
- **成本价是自动算价的输入**，必须维护；`cost_price` 为空的产品不能参与报价。
- 重量 / 体积是外贸特有字段，用于估算运费与装柜量。
- `t_product_price` 按产品 + 币种维护**参考销售价**（含阶梯起点），用于目录展示与询盘参考价 —— **不是报价取价来源**。
- 停售产品不删除（`product_status = 0`），历史订单仍可正常展示。

### 3.7 订单（Order）

- **单头**：订单号、客户、来源报价单、商机、币种、金额、贸易术语、付款方式、定金比例、状态、发货日期、预计到港日。
- **明细**：产品、名称规格快照、数量、单位、单价、金额（**不存币种**）。
- **一单一币种**：币种只在单头。
- **状态机**：

```
CONFIRMED 已确认 → IN_PRODUCTION 生产中 → SHIPPED 已发货 → DELIVERED 已交付 → COMPLETED 已完成
       └──────────────────────→ CANCELLED 已取消
```

| 状态 | 进入条件 |
| --- | --- |
| `CONFIRMED` | 报价转订单，或直接创建 |
| `IN_PRODUCTION` | 手工推进（本期无生产排程） |
| `SHIPPED` | **首次**登记发货即置（分批发货时订单仍为 `SHIPPED`） |
| `DELIVERED` | 登记实际到港日 |
| `COMPLETED` | 已发货 **且** 关联应收全部 `PAID` |
| `CANCELLED` | 手工取消 |

- **付款方式**：`TT` / `LC` / `DP` / `DA` / `OA` / `PAYPAL`。
- 转订单时明细单价金额**快照**报价行的值，报价后续改价不影响已生成的订单。

### 3.8 发货与应收（Shipment / Receivable）

- **发货**：一条订单可**多次发货**（分批）；记录运单/提单号、方式（海运/空运/快递/陆运）、起运港、目的港、发货日期、预计到港、实际到港、总体积 CBM、总重量 kg、状态。
- **应收**：一条订单按 `deposit_rate` 拆成**定金 + 尾款多条**（无定金则一条），每条独立记录应收金额、约定收款日、已收金额、状态。
  - 状态：`UNPAID` 未收 → `PARTIAL` 部分收 → `PAID` 已收清；超过 `due_date` 未收清显示为 `OVERDUE`（查询时按日期判定）。
  - 币种继承自订单。
- **核销**：登记一笔收款（金额、日期、方式、备注），冲减该条应收的 `received_amount`，状态自动流转。
  - 本期**不建收款流水表**，每次核销写一条结构化日志。

### 3.9 邮件（Email）

- **模板**：主题、正文（变量占位 `{{customer.company_name}}`）、语言分类（人工标签）、类型（开发信/报价跟进/催款）。
- **群发**：选模板 + 选一个范围（一个标签 / 一个负责人 / 一个客户）→ 生成一条群发任务。
- **发送记录**：每个收件人一行（邮箱、状态、发送时间、失败原因）。
- 本期**只落库记录**，SMTP 发送可先同步调用或异步任务，接口一致。

### 3.10 经营看板（Dashboard）

| 看板 | 指标 |
| --- | --- |
| 销售漏斗 | 各阶段商机数与金额（折算本位币）、阶段转化率（按当前 stage 快照） |
| 业绩趋势 | 按月/季的成交额、新增客户数、赢单率 |
| 客户分析 | 按来源渠道 / 国家 / 行业 / 等级 的客户数与贡献金额 |
| 商机健康度 | 长期未更新的商机（`t_opportunity.update_time`）、超期未报价的询盘 |
| 应收账龄 | 0-30 / 31-60 / 61-90 / 90+ 天的未回款金额与笔数 |
| 产品排行 | 按成交金额排名的 Top N 产品 |

### 3.11 站内信（Message）

- **两类来源**：`SYSTEM`（业务事件自动触发）/ `USER`（业务员手动发给同事）。
- **触发事件**：

| 事件 | `message_type` | 级别 | 收件人 |
| --- | --- | --- | --- |
| 报价被拒绝 | `QUOTATION_REJECTED` | IMPORTANT | 报价负责人 |
| 报价被确认 | `QUOTATION_CONFIRMED` | IMPORTANT | 报价负责人 |
| 报价即将过期 | `QUOTATION_EXPIRING` | NORMAL | 报价负责人 |
| 订单已确认 | `ORDER_CONFIRMED` | IMPORTANT | 订单负责人 |
| 订单已发货 | `ORDER_SHIPPED` | NORMAL | 订单负责人 |
| 应收逾期 | `RECEIVABLE_OVERDUE` | URGENT | 订单负责人 |
| 询盘超时未报价 | `INQUIRY_TIMEOUT` | NORMAL | 询盘负责人 |
| 客户转交 | `CUSTOMER_ASSIGNED` | NORMAL | 新负责人 |
| 商机阶段变更 | `OPPORTUNITY_STAGE` | NORMAL | 商机负责人 |
| 汇率同步失败 | `RATE_SYNC_FAILED` | URGENT | 全部持 `crm:access` 的用户 |
| 汇率币种熔断 | `RATE_CURRENCY_SUSPENDED` | IMPORTANT | 全部持 `crm:access` 的用户 |
| 人工消息 | `MANUAL` | NORMAL | 指定用户 |

- **级别只影响站内呈现**：`NORMAL` 进列表；`IMPORTANT` 置顶 + 加粗；`URGENT` 置顶 + 加粗 + 前端弹窗。
  **站内信不做任何外推** —— 不发邮件、不推飞书。汇率同步失败另有独立的飞书运维告警（专项文档 §5.3）。
- **文案**：由代码常量类按 `message_type` 分支生成，支持 `{customerName}` 占位符；**一律中文**；金额用原币 + 币种。
- **未读**：前端每 30~60 秒轮询 `GET /messages/unread-count`。本期不上 WebSocket / SSE。
- **保留期**：已读 90 天后定时清理，未读不清理。

### 3.12 基础数据与汇率

- **字典** `t_dict_type` / `t_dict_item`：来源渠道、客户等级、商机阶段、贸易术语、付款方式、产品分类、消息类型。
- **汇率**：由外部免费接口双源交叉比对后自动同步到本地库，本地只做缓存与兜底。
  完整设计（选型实测、比对算法、熔断、表结构、配置项）见 **[汇率数据接入设计](./crm-currency-rate-design.md)**。

---

## 4. 接口设计

### 4.1 通用约定

| 项 | 约定 |
| --- | --- |
| 基础路径 | `/api/crm/**`（应用 context-path 为 `/design`，外部访问 `/design/api/crm/**`） |
| 鉴权 | `Authorization: Bearer <token>`，复用 `AuthInterceptor`；CRM 路径统一校验 `crm:access`，**不逐接口细分** |
| 响应体 | 统一 `ResponseResult<T>`：`{ code, message, data, errors }`，HTTP 恒 200 |
| 错误码 | **不做业务细分错误码** —— 沿用全局 HTTP 语义（400/401/403/404/500），业务提示放在 `message` / `errors` 的自由中文文案里 |
| 分页 | **PageHelper**：`startPage(page, size)` + `PageInfo<T>`，详见 §4.1.1 |
| 金额 | JSON 传字符串（如 `"1250.50"`）避免浮点误差；服务端 `BigDecimal` |
| 币种 | ISO 4217 三字母码；**一单一币种** —— 币种在单头，明细不存币种 |
| 电话 | E.164 格式（`+14155552671`），入库前校验，非法直接 400（§8.2） |
| 时间 | `yyyy-MM-dd HH:mm:ss`（日期 `yyyy-MM-dd`）；**所有时间按 `Asia/Shanghai`** |
| 幂等 | **不实现** `X-Idempotency-Key`；重复提交由唯一索引 + 状态机兜底 |
| 软删除 | 删除即置 `deleted` = 毫秒时间戳；列表默认过滤 `deleted = 0` |
| 排序 | `sort` 只接受**白名单字段**（每个列表接口在代码里维护自己的白名单常量），其余参数忽略；经 `SqlUtil.escapeOrderBySql` 处理 |
| 导出 | 同步导出 **Excel `.xlsx`**（EasyExcel），**单次上限 1 万行**，超出报 400 提示缩小筛选范围；详见 §4.1.2 |

#### 4.1.1 分页实现（PageHelper）

**依赖**

```xml
<dependency>
    <groupId>com.github.pagehelper</groupId>
    <artifactId>pagehelper-spring-boot-starter</artifactId>
    <version>2.1.1</version>
    <exclusions>
        <!-- 必须排除：starter 传递的是 mybatis-spring-boot-starter 2.3.2（Spring Boot 2.x 时代），
             本工程用 Spring Boot 3.2.4 + mybatis-spring-boot-starter 3.0.3（BOM 管理） -->
        <exclusion>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

> `PageHelperAutoConfiguration` 只引用 MyBatis 核心类（`Interceptor` / `Configuration` / `SqlSessionFactory`），
> 不依赖任何 mybatis-spring 特有 API，与 mybatis-spring-boot 3.0.3 兼容。版本**不写**进 Spring Boot BOM。

**配置**（`application.yml`）

```yaml
pagehelper:
  helper-dialect: mysql
  reasonable: true            # page<=0 查第一页，page>总页数 查最后一页
  support-methods-arguments: true
  params: count=countSql
```

**用法约定**

```java
public PageResponse<CustomerDto> listCustomers(PageRequest req) {
    PageHelper.startPage(req.getPage(), req.getSize());
    List<CustomerDto> rows = customerMapper.selectList(req);   // ← 必须紧挨着 startPage
    PageInfo<CustomerDto> info = new PageInfo<>(rows);
    return PageResponse.of(info);
}
```

| 约定 | 说明 |
| --- | --- |
| `startPage` 位置 | 同线程内、紧挨着目标 Mapper 查询之前；中间插入别的 SQL 会污染分页 |
| `PageInfo` 来源 | `new PageInfo<>(list)` —— PageHelper 把分页元信息挂在返回的 `List` 上 |
| 线程 | 分页 List **不要跨线程传递**（PageHelper 用 ThreadLocal 传参） |
| 深分页 | `page × size ≤ 10000`，超出报 400 |
| count | 默认由 PageHelper 自动生成；复杂 JOIN 的 count 慢时用 `countSql` 自定义 |
| 现有代码 | `LotteryPeriodMapper` 的手写 `LIMIT/OFFSET` **一并迁移**，避免两套写法并存 |


#### 4.1.2 Excel 导出

**依赖**

```xml
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>easyexcel</artifactId>
    <version>4.0.3</version>
    <!-- 传递依赖 poi / poi-ooxml 5.2.5（Java 17 兼容）、commons-csv 1.11.0、ehcache 3.9.11 -->
</dependency>
```

> 版本**不写**进 Spring Boot BOM（EasyExcel 不在 BOM 里）。4.0.3 传递 POI **5.2.5**；
> 3.3.4 传递的是 POI 4.1.2（Java 8 时代），**不要用**。
> Spring Boot 3.2.4 不管理 POI 版本，无冲突。

**约定**

| 项 | 约定 |
| --- | --- |
| 格式 | `.xlsx`（**不用 CSV** —— CSV 中文在 Excel 里乱码、不能多 sheet、不能带样式） |
| 输出 | `EasyExcel.write(outputStream, XxxExportDto.class).sheet(sheetName).doWrite(data)` |
| DTO | 每个导出功能**单独建 ExportDto**，用 `@ExcelProperty("列名")` 标注，**不复用 DB 实体**（避免把内部字段带出去，也避免实体改动影响导出格式） |
| 表头 | EasyExcel 默认表头样式（加粗 + 灰底），列宽自动 |
| **金额列** | 写成**数字**（`BigDecimal`），不是字符串 —— 这样在 Excel 里能直接求和、做透视表 |
| 数量 / 比例 | 同上，写数字；比例列加 `0.00%` 格式 |
| 日期列 | 日期 `yyyy-MM-dd`，时间 `yyyy-MM-dd HH:mm:ss`；**按 `Asia/Shanghai`** |
| 币种列 | **单独一列**放 ISO 4217 三字母码，不写进金额里 |
| 电话列 | E.164 原样导出（`+14155552671`），Excel 识别为文本不转科学计数 |
| Sheet 名 | 中文，**长度 ≤ 31 字符**，不能含 `[ ] : * ? / \` |
| 文件名 | `<业务名>_<yyyyMMdd_HHmmss>.xlsx`，如 `客户列表_20261108_153000.xlsx` |
| 中文文件名 | 必须按 **RFC 5987** 编码：`Content-Disposition: attachment; filename*=UTF-8''%E5%AE%A2%E6%88%B7%E5%88%97%E8%A1%A8.xlsx`（同时给一个 ASCII fallback `filename="customers_20261108_153000.xlsx"`） |
| 响应头 | `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |
| 大数据量 | 用 `ExcelWriter` 分批 `write()`（每批 1000 行）**流式输出**，内存恒定，不一次性装全量 |
| 上限 | **1 万行**；超出直接 400，提示"请缩小筛选范围" |
| 筛选条件 | 导出**复用列表接口的筛选参数**（国家、等级、来源、负责人等），导出内容与所见列表一致 |
| 空结果 | 导出只有表头、无数据行（不报 404） |
| 敏感字段 | 第一版**不做脱敏**，直接导出 |

**Controller 骨架**

```java
@GetMapping("/customers/export")
public void exportCustomers(@Valid CustomerQueryRequest query, HttpServletResponse response) throws IOException {
    List<CustomerExportDto> rows = customerService.exportList(query);   // 已按 1 万行上限截断校验
    String fileName = "客户列表_" + LocalDateTime.now().format(FILE_TS) + ".xlsx";
    response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setHeader("Content-Disposition",
            "attachment; filename=\"customers_" + FILE_TS + ".xlsx\"; filename*=UTF-8''" + URLEncoder.encode(fileName, UTF_8));
    EasyExcel.write(response.getOutputStream(), CustomerExportDto.class)
            .sheet("客户列表")
            .doWrite(rows);
}
```

> **为什么用同步而不是异步任务**：1 万行 EasyExcel 流式写通常在 1~3 秒内完成，
> 引入"导出任务表 + 站内信通知完成"会多出 2 张表和一套状态机，收益不抵成本（§1.2）。
> 将来数据量上来（10 万行以上）再改异步，届时站内信已有现成的通知通道。

### 4.2 客户 `/api/crm/customers`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/customers` | 列表（按名称/国家/等级/来源/负责人筛选） |
| GET | `/customers/{id}` | 详情（含联系人数量、关联商机数、关联订单数） |
| POST | `/customers` | 新建 |
| PUT | `/customers/{id}` | 更新 |
| DELETE | `/customers/{id}` | 逻辑删除（有关联商机/订单时拒绝） |
| POST | `/customers/{id}/transfer` | 转交负责人 |
| GET | `/customers/export` | 导出 Excel `.xlsx`（复用列表筛选条件，上限 1 万行） |
| GET | `/customers/dictionary-options` | 下拉选项（来源渠道、等级等） |

### 4.3 联系人 `/api/crm/customers/{customerId}/contacts`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/contacts` | 列表（可跨客户筛选） |
| GET | `/contacts/{id}` | 详情 |
| POST | `/contacts` | 新建（校验邮箱唯一 + E.164 + 号码客户内唯一） |
| PUT | `/contacts/{id}` | 更新 |
| DELETE | `/contacts/{id}` | 逻辑删除 |

### 4.4 商机 `/api/crm/opportunities`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/opportunities` | 列表（按阶段、负责人、金额区间、客户筛选） |
| GET | `/opportunities/{id}` | 详情 |
| POST | `/opportunities` | 新建 |
| PUT | `/opportunities/{id}` | 更新（含推进阶段） |
| DELETE | `/opportunities/{id}` | 逻辑删除 |
| POST | `/opportunities/{id}/stage` | 推进阶段（置 `LOST` 必须填 `lost_reason`） |
| POST | `/opportunities/{id}/convert-to-order` | 赢单转订单（自动置 `WON`） |
| GET | `/opportunities/pipeline` | 漏斗统计 |

### 4.5 询盘 `/api/crm/inquiries`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/inquiries` | 列表（按状态、客户、负责人、超时未报价） |
| GET | `/inquiries/{id}` | 详情（含关联报价单） |
| POST | `/inquiries` | 新建 |
| PUT | `/inquiries/{id}` | 更新 |
| POST | `/inquiries/{id}/quotations` | 基于询盘创建报价单（新版本） |

### 4.6 报价 `/api/crm/quotations`

> 请求体**不接受** `unit_price` / `amount`（服务端算），除非该行 `manualPrice = true`。

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/quotations` | 列表（按客户、状态、币种、有效期） |
| GET | `/quotations/{id}` | 详情（明细 + 金额汇总 + 利润） |
| POST | `/quotations` | 新建（服务端自动算价） |
| PUT | `/quotations/{id}` | 更新（仅 `DRAFT` 可改；改数量/加价率后服务端重算） |
| POST | `/quotations/calculate` | **试算**：只算不落库，返回每行单价/金额与单头汇总 |
| POST | `/quotations/{id}/recalculate` | 按当前加价率/折扣重算整单（覆盖非 `manual_price` 的行） |
| POST | `/quotations/{id}/submit` | 提交发送（`DRAFT` → `SENT`） |
| POST | `/quotations/{id}/confirm` | 客户确认（`SENT` → `CONFIRMED`） |
| POST | `/quotations/{id}/reject` | 客户拒绝（`SENT` → `REJECTED`，必须填原因） |
| POST | `/quotations/{id}/convert-to-order` | 报价转订单（快照单价金额） |

### 4.7 产品 `/api/crm/products`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/products` | 列表（分类、状态、关键字） |
| GET | `/products/{id}` | 详情（含各币种参考价） |
| POST | `/products` | 新建 |
| PUT | `/products/{id}` | 更新 |
| DELETE | `/products/{id}` | 逻辑删除 |
| GET | `/products/{id}/prices` | 多币种参考价列表 |
| PUT | `/products/{id}/prices` | 批量维护多币种参考价 |

### 4.8 订单 `/api/crm/orders`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/orders` | 列表（状态、客户、负责人、发货日期区间） |
| GET | `/orders/{id}` | 详情（明细、发货、收款） |
| POST | `/orders` | 新建（可带 `quotationId` 由报价生成） |
| PUT | `/orders/{id}` | 更新（仅未发货可改） |
| DELETE | `/orders/{id}` | 逻辑删除（已发货不可删） |
| POST | `/orders/{id}/ship` | 登记发货（首次发货时订单置 `SHIPPED`） |
| POST | `/orders/{id}/receive` | 登记收款（核销指定应收） |

### 4.9 发货 / 收款 `/api/crm/shipments`、`/api/crm/receivables`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/shipments` | 发货列表 |
| POST | `/shipments` | 新建发货单 |
| PUT | `/shipments/{id}` | 更新发货单 |
| GET | `/receivables` | 应收列表（按状态、账龄区间） |
| POST | `/receivables/{id}/writeoff` | 登记收款（核销） |
| GET | `/receivables/aging` | 账龄分析 |

### 4.10 邮件 `/api/crm/emails`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/email/templates` | 模板列表 |
| POST | `/email/templates` | 新建 / 更新模板 |
| DELETE | `/email/templates/{id}` | 删除模板 |
| POST | `/email/templates/{id}/preview` | 预览渲染结果（替换 `{{xxx}}`） |
| GET | `/email/campaigns` | 群发任务列表 |
| POST | `/email/campaigns` | 创建群发（选一个范围） |
| POST | `/email/campaigns/{id}/send` | 执行发送 |
| GET | `/email/campaigns/{id}/logs` | 发送明细 |

### 4.11 看板 `/api/crm/dashboard`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/dashboard/summary` | 顶部指标（本月成交额、活跃客户数、进行中商机、逾期应收） |
| GET | `/dashboard/pipeline` | 销售漏斗 |
| GET | `/dashboard/trend` | 业绩趋势（按月/季） |
| GET | `/dashboard/customer-source` | 客户来源分布 |
| GET | `/dashboard/receivable-aging` | 应收账龄 |
| GET | `/dashboard/top-products` | 产品成交排行 |

### 4.12 基础数据 `/api/crm/dict`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/dict/{type}` | 取字典项 |
| PUT | `/dict/{type}` | 维护字典项 |

> 汇率相关端点（`/currencies`、`/currencies/rates`、`/currencies/convert`、
> `/currencies/sync`、`/currencies/sync-status`、`/currencies/rates/verification`）
> 见 **[汇率数据接入设计](./crm-currency-rate-design.md) §6**。

### 4.13 站内信 `/api/crm/messages`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/messages` | 我的消息列表（按 `isRead` / `messageType` / `level` 筛选，未读优先） |
| GET | `/messages/unread-count` | **未读数**（轻量，前端每 30~60s 轮询） |
| GET | `/messages/{id}` | 详情（**并自动标记已读**） |
| PUT | `/messages/{id}/read` | 标记已读 |
| PUT | `/messages/read-all` | 全部标记已读（可按类型限定） |
| POST | `/messages` | 发送人工消息（`receiverId` + `title` + `content`） |
| DELETE | `/messages/{id}` | 删除（逻辑删除，仅自己的消息） |

---

## 5. 数据库设计

### 5.1 设计原则

1. **MySQL 8**，`utf8mb4` / `utf8mb4_unicode_ci`。
2. **不建物理外键** —— 关联关系由应用层（MyBatis JOIN）维护，即"逻辑 mapping"。
   引用完整性与级联清理由 Service 层负责（校验清单见 §5.4）。
   关联列**必须显式建索引**（没有外键就不会再由 InnoDB 自动建）。
3. **主键** `VARCHAR(36)`，应用层生成 UUID。
4. **金额**用 `DECIMAL`，禁止 `FLOAT` / `DOUBLE`；单价 6 位小数，数量 4 位，金额 2 位，比例 2 位。
5. **多币种**：原币字段 + `currency_code`；另设 `*_base` 列存本位币（USD）快照，创建时固化。
   **一单一币种**：币种只挂在单头上，明细行不存币种。
6. **电话**：`phone` / `whatsapp` 用 **E.164** 格式，`VARCHAR(20)`，入库前在 Service 层校验。
7. **逻辑删除**：统一 `deleted BIGINT NOT NULL DEFAULT 0`
   （**0 = 未删除；删除时写入删除时刻的毫秒时间戳**）。
   > 为什么用 BIGINT 而不是 TINYINT 0/1：带**业务唯一索引**的表（客户 / 联系人 / 报价单 / 订单 …）
   > 逻辑删除后，若唯一索引不含 `deleted`，删掉一个客户就再也建不了同名客户；
   > 若用 TINYINT，`UNIQUE(a,b,deleted)` 又只能保留一条已删除记录（删两次就撞）。
   > 用毫秒时间戳后：未删除记录 `deleted=0` 唯一性照常，已删除记录天然错开、可留任意多条。
   > **代价**：所有业务唯一索引必须带 `deleted` 列，漏一处就出现"删了建不回来"的坑。
   > **例外**：纯关联表（`t_customer_tag_rel`、`t_email_log`）与字典表**物理删除**，不设 `deleted`。
8. **时间**：`create_time` / `update_time` 为 `TIMESTAMP`，`update_time` 带 `ON UPDATE CURRENT_TIMESTAMP`。
9. **枚举**：用 `VARCHAR` 存稳定的英文码（如 `FOLLOWING`），可读文案走 `t_dict_item`；不用数字码。
10. **迁移**：Flyway，命名 `V<N>__<描述>.sql`，**已执行版本禁止修改**，新增字段一律新版本。

### 5.2 表清单（24 张新表）

| 模块 | 表 | 说明 |
| --- | --- | --- |
| 客户 | `t_customer` | 买家公司 |
| 客户 | `t_customer_contact` | 联系人 |
| 客户 | `t_customer_tag` | 客户标签 |
| 客户 | `t_customer_tag_rel` | 客户-标签关联（物理删除） |
| 商机 | `t_opportunity` | 销售商机 |
| 产品 | `t_product` | 产品目录 |
| 产品 | `t_product_price` | 多币种参考价 |
| 询盘报价 | `t_inquiry` | 询盘 / RFQ |
| 询盘报价 | `t_quotation` | 报价单头 |
| 询盘报价 | `t_quotation_item` | 报价明细 |
| 订单 | `t_order` | 销售订单头 |
| 订单 | `t_order_item` | 订单明细 |
| 订单 | `t_shipment` | 发货单 |
| 订单 | `t_receivable` | 应收账款 |
| 邮件 | `t_email_template` | 邮件模板 |
| 邮件 | `t_email_campaign` | 群发任务 |
| 邮件 | `t_email_log` | 发送明细（物理删除） |
| 通知 | `t_message` | 站内信 |
| 基础 | `t_dict_type` | 字典类型 |
| 基础 | `t_dict_item` | 字典项 |
| 基础 | `t_currency` | 币种字典 ᵀ |
| 基础 | `t_currency_rate` | 汇率 ᵀ |
| 基础 | `t_currency_sync_log` | 汇率同步批次 ᵀ |
| 基础 | `t_currency_rate_check` | 两源交叉比对明细 ᵀ |

> ᵀ 4 张汇率表的字段结构、索引与迁移见 **[汇率数据接入设计](./crm-currency-rate-design.md) §7**。

复用现有表：`t_user` / `t_role` / `t_permission` / `t_user_role` / `t_role_permission`
（仅在 `t_permission` 追加 `crm:access`）。

### 5.3 表结构详设

> 每张表都有 `id VARCHAR(36) NOT NULL` + `PRIMARY KEY (id)`，
> 以及 `create_time` / `update_time` / `deleted` 三个公共列，不再逐表重复列出。

#### 5.3.1 `t_customer` 客户

| 字段 | 类型 | 空 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| company_name | VARCHAR(128) | N | | 公司全称 |
| company_name_norm | VARCHAR(128) | N | | 归一化名称（大写去空格），用于去重 |
| short_name | VARCHAR(64) | Y | | 简称 |
| country_code | CHAR(2) | N | | ISO 3166-1 alpha-2，如 US / DE |
| province | VARCHAR(64) | Y | | 省 / 州 |
| city | VARCHAR(64) | Y | | 城市 |
| address | VARCHAR(255) | Y | | 地址（可外文） |
| website | VARCHAR(128) | Y | | 官网 |
| industry | VARCHAR(64) | Y | | 行业 |
| source | VARCHAR(32) | N | | 来源渠道，字典 `customer_source` |
| level | VARCHAR(8) | N | 'B' | 客户等级 A/B/C，字典 `customer_level` |
| customer_status | VARCHAR(16) | N | 'POTENTIAL' | POTENTIAL/FOLLOWING/DEALED/LOST |
| owner_id | VARCHAR(36) | N | | 负责人 → t_user.id |
| first_deal_at | DATETIME | Y | | 首次成交时间 |
| last_contact_at | DATETIME | Y | | 最近联系时间（本期预留） |
| remark | VARCHAR(1024) | Y | | 备注（可外文） |

索引：`uk_company_norm(country_code, company_name_norm, deleted)` 唯一、
`idx_owner(owner_id)`、`idx_status(customer_status)`、`idx_country(country_code)`、`idx_level(level)`、`idx_source(source)`

#### 5.3.2 `t_customer_contact` 联系人

| 字段 | 类型 | 空 | 说明 |
| --- | --- | --- | --- |
| customer_id | VARCHAR(36) | N | 所属客户 → t_customer.id |
| name | VARCHAR(64) | N | 姓名（可外文） |
| position | VARCHAR(64) | Y | 职位（可外文） |
| email | VARCHAR(128) | Y | 邮箱 |
| phone | VARCHAR(20) | Y | 电话，**E.164**（`+14155552671`），入库前校验 |
| whatsapp | VARCHAR(20) | Y | WhatsApp，同为 E.164 |
| language | VARCHAR(16) | Y | 沟通语言标记（en/de），**不驱动任何行为** |
| timezone | VARCHAR(48) | Y | 时区，如 Europe/Berlin |
| is_primary | TINYINT | N | 1 = 主要联系人 |
| remark | VARCHAR(512) | Y | 备注（可外文） |

索引：`idx_customer(customer_id)`、`idx_email(email)`、`idx_phone(phone)`、
`uk_customer_email(customer_id, email, deleted)` 唯一

> **唯一性**：邮箱 —— 同一客户内唯一；电话 —— 同一 E.164 号码在**同一客户内**只能属于一个联系人。
> 跨客户允许重复（同一采购代理可能对接多家买家）。均为应用层校验 + 唯一索引兜底。

#### 5.3.3 `t_customer_tag` / `t_customer_tag_rel`

`t_customer_tag`：`name`(VARCHAR64, N)、`color`(VARCHAR16)、`description`(VARCHAR255)、`owner_id`(VARCHAR36, N)
索引：`uk_name(name, deleted)` 唯一、`idx_owner(owner_id)`

`t_customer_tag_rel`：`customer_id`(N)、`tag_id`(N) —— **物理删除，不设 `deleted`**
索引：`uk_rel(customer_id, tag_id)` 唯一、`idx_tag(tag_id)`

#### 5.3.4 `t_opportunity` 商机

| 字段 | 类型 | 空 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| customer_id | VARCHAR(36) | N | | 客户 → t_customer.id |
| name | VARCHAR(128) | N | | 商机名称 |
| stage | VARCHAR(24) | N | 'INITIAL_CONTACT' | 阶段，字典 `opportunity_stage` |
| amount | DECIMAL(18,2) | N | 0 | 预计金额（原币） |
| currency_code | CHAR(3) | N | 'USD' | 币种 |
| amount_base | DECIMAL(18,2) | N | 0 | 本位币（USD）快照 |
| win_rate | INT | N | 0 | 赢率 0-100 |
| expect_close_date | DATE | Y | | 预计成交日 |
| competitor | VARCHAR(255) | Y | | 竞争对手 |
| lost_reason | VARCHAR(255) | Y | | 丢单原因（置 LOST 时必填） |
| owner_id | VARCHAR(36) | N | | 负责人 |
| remark | VARCHAR(1024) | Y | | 备注 |

索引：`idx_customer(customer_id)`、`idx_owner_stage(owner_id, stage)`、`idx_stage(stage)`、`idx_expect_close(expect_close_date)`

#### 5.3.5 `t_product` 产品

| 字段 | 类型 | 空 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| product_code | VARCHAR(64) | N | | 产品编码 |
| product_name | VARCHAR(128) | N | | 名称 |
| category | VARCHAR(64) | Y | | 分类，字典 `product_category` |
| specification | VARCHAR(255) | Y | | 规格描述 |
| unit | VARCHAR(16) | N | 'PCS' | 单位（PCS/KG/CBM/SET） |
| weight_kg | DECIMAL(12,4) | Y | | 单件重量（kg），用于运费估算 |
| volume_cbm | DECIMAL(12,6) | Y | | 单件体积（CBM），用于装柜量估算 |
| cost_price | DECIMAL(18,6) | Y | | 成本单价（**币种见 `cost_currency`**） |
| cost_currency | CHAR(3) | N | 'USD' | 成本价币种；报价时必须与报价单币种相同 |
| default_price | DECIMAL(18,6) | Y | | 默认销售价（参考） |
| default_currency | CHAR(3) | N | 'USD' | 默认币种（参考） |
| moq | INT | Y | | 起订量；报价明细数量必须 ≥ 此值 |
| hs_code | VARCHAR(32) | Y | | HS 海关编码 |
| origin | VARCHAR(64) | Y | | 产地 |
| product_status | TINYINT | N | 1 | 1 在售 0 停售 |
| remark | VARCHAR(512) | Y | | 备注 |

索引：`uk_product_code(product_code, deleted)` 唯一、`idx_category(category)`、`idx_status(product_status)`

#### 5.3.6 `t_product_price` 多币种参考价

| 字段 | 类型 | 空 | 说明 |
| --- | --- | --- | --- |
| product_id | VARCHAR(36) | N | 产品 |
| currency_code | CHAR(3) | N | 币种 |
| price | DECIMAL(18,6) | N | 参考销售单价 |
| min_quantity | INT | Y | 阶梯起点（默认 0） |
| valid_from | DATE | Y | 生效日 |
| valid_to | DATE | Y | 失效日 |

索引：`uk_product_currency(product_id, currency_code, min_quantity, deleted)` 唯一、`idx_valid(valid_to)`

> 本表是**参考价**，用于目录展示与询盘目标价参考。
> **报价的单价不取本表** —— 由成本价 + 加价率算出（§3.5.1）。

#### 5.3.7 `t_inquiry` 询盘

| 字段 | 类型 | 空 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| customer_id | VARCHAR(36) | N | | 客户 |
| contact_id | VARCHAR(36) | Y | | 联系人 |
| inquiry_no | VARCHAR(64) | N | | 询盘编号 |
| source | VARCHAR(32) | N | | 来源渠道 |
| subject | VARCHAR(255) | Y | | 主题 |
| target_price | DECIMAL(18,6) | Y | | 客户目标单价（**仅参考，不参与定价**） |
| currency_code | CHAR(3) | N | 'USD' | 币种 |
| quantity | DECIMAL(18,4) | Y | | 数量 |
| unit | VARCHAR(16) | Y | | 单位 |
| expect_delivery_date | DATE | Y | | 期望交期 |
| inquiry_status | VARCHAR(16) | N | 'NEW' | NEW/PROCESSING/QUOTED/WON/LOST |
| content | TEXT | Y | | 原始需求内容（可外文） |
| owner_id | VARCHAR(36) | N | | 负责人 |
| first_reply_at | DATETIME | Y | | 首次回复时间（**报价单创建时回填**） |

索引：`uk_inquiry_no(inquiry_no, deleted)` 唯一、`idx_customer(customer_id)`、`idx_owner_status(owner_id, inquiry_status)`、`idx_source(source)`

#### 5.3.8 `t_quotation` 报价单头

| 字段 | 类型 | 空 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| quotation_no | VARCHAR(64) | N | | 报价单号 |
| customer_id | VARCHAR(36) | N | | 客户 |
| inquiry_id | VARCHAR(36) | Y | | 来源询盘 |
| opportunity_id | VARCHAR(36) | Y | | 关联商机 |
| version_no | INT | N | 1 | 版本号（同一询盘 + 币种内递增） |
| currency_code | CHAR(3) | N | 'USD' | 币种（**一单一币种**） |
| incoterm | VARCHAR(16) | N | | 贸易术语 FOB/CIF/CFR/EXW/DDP |
| markup_rate | DECIMAL(6,2) | N | 0 | 加价率 %（单头默认，明细可覆盖），0~1000 |
| discount_rate | DECIMAL(6,2) | N | 0 | 折扣率 %（单头默认，明细可覆盖），0~100 |
| total_amount | DECIMAL(18,2) | N | 0 | 报价总额（原币，服务端算） |
| total_cost | DECIMAL(18,2) | N | 0 | 成本汇总（原币） |
| profit_amount | DECIMAL(18,2) | N | 0 | 毛利额 |
| profit_rate | DECIMAL(8,2) | N | 0 | 毛利率 % |
| total_amount_base | DECIMAL(18,2) | N | 0 | 本位币（USD）快照 |
| valid_until | DATE | Y | | 有效期至 |
| quotation_status | VARCHAR(16) | N | 'DRAFT' | DRAFT/SENT/CONFIRMED/REJECTED/EXPIRED |
| reject_reason | VARCHAR(255) | Y | | 拒绝原因（转 REJECTED 时必填） |
| owner_id | VARCHAR(36) | N | | 负责人 |
| remark | VARCHAR(512) | Y | | 备注 |

索引：`uk_quotation_no(quotation_no, deleted)` 唯一、
`uk_inquiry_version(inquiry_id, currency_code, version_no, deleted)` 唯一（含币种：允许同询盘分别报 USD 与 EUR）、
`idx_customer(customer_id)`、`idx_status(quotation_status)`、`idx_owner(owner_id)`、`idx_valid_until(valid_until)`

#### 5.3.9 `t_quotation_item` 报价明细

| 字段 | 类型 | 空 | 说明 |
| --- | --- | --- | --- |
| quotation_id | VARCHAR(36) | N | 报价单 |
| product_id | VARCHAR(36) | Y | 产品 |
| product_name | VARCHAR(128) | N | 名称快照 |
| specification | VARCHAR(255) | Y | 规格快照 |
| quantity | DECIMAL(18,4) | N | 数量（必须 ≥ 产品 MOQ） |
| unit | VARCHAR(16) | Y | 单位 |
| cost_price | DECIMAL(18,6) | N | 成本单价快照（建明细时取自产品） |
| markup_rate | DECIMAL(6,2) | Y | 行覆盖加价率 %（空则用单头） |
| discount_rate | DECIMAL(6,2) | Y | 行覆盖折扣率 %（空则用单头） |
| unit_price | DECIMAL(18,6) | N | 单价（服务端算） |
| amount | DECIMAL(18,2) | N | 行金额（服务端算） |
| manual_price | TINYINT | N | 1 = 手工改过单价，不随加价率/折扣重算 |

索引：`idx_quotation(quotation_id)`、`idx_product(product_id)`

> 明细行**不存币种** —— 取自 `t_quotation.currency_code`（一单一币种）。

#### 5.3.10 `t_order` / `t_order_item` 订单

`t_order`：

| 字段 | 类型 | 空 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| order_no | VARCHAR(64) | N | | 订单号 |
| customer_id | VARCHAR(36) | N | | 客户 |
| quotation_id | VARCHAR(36) | Y | | 来源报价单 |
| opportunity_id | VARCHAR(36) | Y | | 来源商机 |
| currency_code | CHAR(3) | N | 'USD' | 币种（**一单一币种**） |
| total_amount | DECIMAL(18,2) | N | 0 | 订单金额（原币） |
| total_amount_base | DECIMAL(18,2) | N | 0 | 本位币（USD）快照 |
| incoterm | VARCHAR(16) | Y | | 贸易术语 |
| payment_term | VARCHAR(24) | Y | | 付款方式 TT/LC/DP/DA/OA/PAYPAL |
| deposit_rate | INT | Y | | 定金比例 %（决定应收拆分） |
| order_status | VARCHAR(20) | N | 'CONFIRMED' | CONFIRMED/IN_PRODUCTION/SHIPPED/DELIVERED/COMPLETED/CANCELLED |
| ship_date | DATE | Y | | 发货日期 |
| expect_arrive_date | DATE | Y | | 预计到港日 |
| owner_id | VARCHAR(36) | N | | 负责人 |
| remark | VARCHAR(1024) | Y | | 备注 |

索引：`uk_order_no(order_no, deleted)` 唯一、`idx_customer(customer_id)`、`idx_owner_status(owner_id, order_status)`、`idx_ship_date(ship_date)`

`t_order_item`：`id`(N)、`order_id`(N)、`product_id`(Y)、`product_name`(N, 快照)、`specification`(Y, 快照)、
`quantity`(DECIMAL(18,4), N)、`unit`(Y)、`unit_price`(DECIMAL(18,6), N)、`amount`(DECIMAL(18,2), N)
索引：`idx_order(order_id)`、`idx_product(product_id)`

> 明细**不存币种**。由报价转订单时，单价金额**快照**报价行的值。

#### 5.3.11 `t_shipment` 发货单

| 字段 | 类型 | 空 | 说明 |
| --- | --- | --- | --- |
| order_id | VARCHAR(36) | N | 订单（一条订单可多次发货） |
| shipping_no | VARCHAR(64) | Y | 运单 / 提单号 |
| shipping_type | VARCHAR(16) | N | SEA/AIR/EXPRESS/TRUCK |
| departure_port | VARCHAR(64) | Y | 起运港 |
| destination_port | VARCHAR(64) | Y | 目的港 |
| ship_date | DATE | Y | 发货日期 |
| expect_arrive_date | DATE | Y | 预计到港日 |
| actual_arrive_date | DATE | Y | 实际到港日 |
| total_cbm | DECIMAL(12,3) | Y | 总体积 CBM |
| total_weight | DECIMAL(14,3) | Y | 总重量 kg |
| shipment_status | VARCHAR(16) | N | PREPARING/SHIPPED/ARRIVED |
| remark | VARCHAR(512) | Y | 备注 |

索引：`idx_order(order_id)`、`idx_shipping_no(shipping_no)`、`idx_ship_date(ship_date)`

#### 5.3.12 `t_receivable` 应收

| 字段 | 类型 | 空 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| order_id | VARCHAR(36) | N | | 订单 |
| currency_code | CHAR(3) | N | 'USD' | 币种（继承订单） |
| receivable_type | VARCHAR(16) | N | 'BALANCE' | DEPOSIT 定金 / BALANCE 尾款 / FULL 全款 |
| receivable_amount | DECIMAL(18,2) | N | 0 | 应收总额 |
| received_amount | DECIMAL(18,2) | N | 0 | 已收金额（累计值，无流水表） |
| due_date | DATE | Y | | 约定收款日 |
| receivable_status | VARCHAR(16) | N | 'UNPAID' | UNPAID/PARTIAL/PAID/OVERDUE |
| overdue_days | INT | N | 0 | 逾期天数（每日定时刷新；查询时也按 `due_date` 实时算） |
| owner_id | VARCHAR(36) | N | | 负责人 |

索引：`idx_order(order_id)`、`idx_owner_status(owner_id, receivable_status)`、`idx_due(due_date)`

> 一条订单按 `deposit_rate` 拆成**定金 + 尾款多条**，每条独立核销、独立算账龄。

#### 5.3.13 邮件三表

`t_email_template`：`name`(N)、`subject`(N)、`content`(LONGTEXT, N)、`language`(N, 'zh')（人工分类标签）、`template_type`(N)
索引：`uk_name(name, deleted)` 唯一

`t_email_campaign`：`name`(N)、`template_id`(N)、`target_type`(N, 'TAG'/'OWNER'/'CUSTOMER')、`target_value`(N)、
`campaign_status`(N, 'DRAFT')、`total_count`(N,0)、`success_count`(N,0)、`fail_count`(N,0)、`sent_at`(DATETIME)、`owner_id`(N)
索引：`idx_owner(owner_id)`、`idx_status(campaign_status)`

`t_email_log`：`campaign_id`(N)、`contact_id`(N)、`email`(N)、`send_status`(N)、`error_msg`(VARCHAR512)、`sent_at`(DATETIME)
索引：`idx_campaign(campaign_id)`、`idx_email(email)` —— **流水表，物理删除，不设 `deleted`**

#### 5.3.14 `t_dict_type` / `t_dict_item` 字典

`t_dict_type`：`type_code`(VARCHAR32, N)、`type_name`(VARCHAR64, N)、`description`(VARCHAR255, Y)
索引：`uk_type_code(type_code)` 唯一

`t_dict_item`：`type_code`(N)、`item_code`(VARCHAR32, N)、`item_name`(VARCHAR64, N)、`item_value`(VARCHAR255, Y)、`sort_no`(INT, N, 0)、`extra`(JSON, Y)（扩展属性，如阶段默认赢率）
索引：`uk_type_item(type_code, item_code)` 唯一、`idx_sort(type_code, sort_no)` —— **字典物理删除，不设 `deleted`**

#### 5.3.15 `t_message` 站内信

| 字段 | 类型 | 空 | 默认 | 说明 |
| --- | --- | --- | --- | --- |
| message_type | VARCHAR(32) | N | | 消息类型，字典 `message_type` |
| level | VARCHAR(16) | N | 'NORMAL' | NORMAL / IMPORTANT / URGENT |
| title | VARCHAR(255) | N | | 标题 |
| content | VARCHAR(2000) | Y | | 正文（渲染后的文案快照） |
| sender_type | VARCHAR(16) | N | 'SYSTEM' | SYSTEM / USER |
| sender_id | VARCHAR(36) | Y | | 系统消息为空；人工消息为发送人 |
| receiver_id | VARCHAR(36) | N | | 收件人 → t_user.id |
| biz_type | VARCHAR(32) | Y | | CUSTOMER/OPPORTUNITY/INQUIRY/QUOTATION/ORDER/RECEIVABLE/CURRENCY |
| biz_id | VARCHAR(36) | Y | | 关联业务主键（点击跳转） |
| biz_title | VARCHAR(255) | Y | | 冗余业务标题（列表免回查） |
| is_read | TINYINT | N | 0 | 0 未读 / 1 已读 |
| read_at | DATETIME | Y | | 已读时间 |

索引：`idx_receiver_read(receiver_id, is_read, create_time)`、`idx_biz(biz_type, biz_id)`、`idx_type(message_type)`

> **只建 1 张表，不建"消息-收件人"关联表** —— 群发靠 fan-out：发给 N 个用户写 N 行，
> 每行独立 `is_read` / `read_at`。当前只有 1 个用户，fan-out 行数极小；
> 真正需要"一条消息 N 收件人 + 群发记录"时再拆表。

### 5.4 逻辑外键校验清单（Service 层负责）

写入关联行前，Service 必须校验被引用的主体存在且未删除：

| 表 | 写入前必须校验 |
| --- | --- |
| `t_customer_contact` | `customer_id` 存在 |
| `t_customer_tag_rel` | `customer_id`、`tag_id` 存在 |
| `t_opportunity` | `customer_id` 存在 |
| `t_inquiry` | `customer_id` 存在；`contact_id` 存在且属于该客户 |
| `t_quotation` | `customer_id` 存在；`inquiry_id` / `opportunity_id` 存在 |
| `t_quotation_item` | `quotation_id` 存在；`product_id` 存在且在售；产品 `cost_currency` == 报价单币种；`cost_price` 非空；`quantity` ≥ `moq` |
| `t_order` | `customer_id` 存在；`quotation_id` / `opportunity_id` 存在 |
| `t_order_item` | `order_id` 存在；`product_id` 存在 |
| `t_shipment` | `order_id` 存在 |
| `t_receivable` | `order_id` 存在；币种与订单一致 |
| `t_email_campaign` | `template_id` 存在 |
| `t_email_log` | `campaign_id` 存在；`contact_id` 存在且有邮箱 |
| `t_message` | `receiver_id` / `sender_id` 存在 |

删除主体时的清理顺序（先清关联行再删主体）：

| 删除 | 先清理 |
| --- | --- |
| 客户 | `t_customer_contact`、`t_customer_tag_rel` |
| 联系人 | `t_inquiry.contact_id` 置空、`t_email_log` 级联删 |
| 产品 | `t_quotation_item.product_id` 置空 |
| 订单 | `t_order_item`、`t_shipment`、`t_receivable` |
| 询盘 | 不允许删除（已被报价引用时拒绝） |

### 5.5 金额精度与舍入规则

| 环节 | 规则 |
| --- | --- |
| 单价 | `DECIMAL(18,6)`，单价本身不做舍入（保留 6 位） |
| 数量 | `DECIMAL(18,4)` |
| 行金额 | `amount = round(unit_price × quantity, 2)` —— **在服务端统一舍入到 2 位** |
| 单头金额 | `total_amount = Σ 行金额`（已是 2 位，不再二次舍入） |
| 成本汇总 | `total_cost = Σ round(cost_price × quantity, 2)` |
| 毛利 | `profit_amount = total_amount - total_cost`；`profit_rate = round(profit_amount / total_amount × 100, 2)` |
| 汇率折算 | `*_base = round(原币金额 × 汇率, 2)` |
| 汇率取值 | 指定日期 → 取当天汇率；当天没有 → 取**该日期之前最近一条**；完全无 → 置 0 且响应标 `rateMissing=true`（**不允许把 0 当真实折算金额静默入库**） |
| 币种 | 全链路**一单一币种**，明细不存币种，不做跨币种混加 |

---

## 6. 实施顺序

每个里程碑结束都要 `./mvnw -B test` 通过 + `./mvnw -B -DskipTests package` 通过。

### 6.1 TDD 约定

CRM 模块**一律按 TDD 推进**：先写测试（红）→ 再写实现（绿）→ 再重构。

| 项 | 约定 |
| --- | --- |
| 测试位置 | `src/test/java/com/yaowenltd/projectinfomationmanage/crm/` |
| 纯逻辑测试 | JUnit 5，**不加 `@SpringBootTest`**，不连数据库；用 Mockito 打桩 Mapper |
| 上下文冒烟 | 只有 `CrmContextSmokeTests` 用 `@SpringBootTest`，显式排除 `DataSourceAutoConfiguration` / `MybatisAutoConfiguration`，全部 Mapper 用 `@MockBean` 占位 |
| 命名 | 测试类 `XxxTests`（包级私有），测试方法 `testXxx`；一条断言一个契约 |
| 业务异常 | 断言 `IllegalArgumentException` / `UnauthorizedException` / `ForbiddenException`，**不在 Service 里 try-catch 吞掉** |
| 提交红线 | `./mvnw -B test` 必须离线跑通，不连 MySQL / 不连外部接口 |

**已写好的测试基线**（实现尚未落地，当前为"红"）

| 测试类 | 用例数 | 覆盖的契约 |
| --- | --- | --- |
| `E164PhoneValidatorTests` | 22 | §8.2 E.164 格式、长度、国家码、错误文案 |
| `QuotationPricingServiceTests` | 29 | §3.5.1 加价率/折扣/手工改价/舍入/汇总/成本缺失/币种不符/MOQ/停售/服务端重算 |
| `OrderStatusServiceTests` | 25 | §3.7 订单状态机合法与非法流转、终态 |
| `OpportunityStageServiceTests` | 19 | §3.3 商机阶段机、跳级拒绝、LOST 必填原因 |
| `QuotationStatusServiceTests` | 16 | §3.5 报价单状态机、REJECTED 必填原因、过期判定 |
| `CurrencyCrossCheckServiceTests` | 15 | 汇率文档 §4 偏差算法、分级阈值、日期差跳过、熔断与恢复 |
| `CurrencyRateQueryServiceTests` | 7 | §5.5 取汇率、USD 三角折算、无汇率返回 null |
| `MessageServiceTests` | 10 | §3.11 群发 fan-out、系统消息无发送人、已读与未读数 |
| `CrmContextSmokeTests` | 1 | CRM Mapper/Service 接入后应用上下文仍可装配 |

> 后续每个里程碑按同一模式补测试：先在对应测试类里加用例（红），再实现（绿）。

### M1 · 客户建档

| 任务 | 产出 |
| --- | --- |
| Flyway `V2__init_crm_schema.sql` | 建 §5.3 的表（不含 t_currency 4 表与 t_message） |
| `V3__add_crm_permission.sql` | 插入 `crm:access` 并授予业务员角色 |
| `V4__add_crm_dict_data.sql` | 字典初始数据（来源渠道、客户等级、商机阶段、贸易术语、付款方式、产品分类） |
| `E164PhoneValidator`（common 包） | E.164 校验工具 |
| 客户 / 联系人 / 标签 CRUD + **Excel 导出** | 8 + 5 个端点 |
| 迁移 `LotteryPeriodMapper` 到 PageHelper | 统一分页写法 |

**验收**：能建客户与联系人；重复客户被唯一约束拦下；非法电话返回 400；导出 Excel 能正常打开、中文不乱码、金额列可求和。

### M2 · 商机与产品

| 任务 | 产出 |
| --- | --- |
| 产品 / 参考价 CRUD | 7 个端点 |
| 商机 CRUD + 阶段流转 + 漏斗统计 | 8 个端点 |
| 看板第一版（漏斗 + 趋势 + 客户分析） | 4 个端点 |

**验收**：商机六阶段流转合法；置 LOST 必须填原因；赢单率按当前快照统计正确。

### M3 · 询盘与报价（核心）

| 任务 | 产出 |
| --- | --- |
| 询盘 CRUD | 5 个端点 |
| 报价 CRUD + 版本 + 状态机 + 试算 + 重算 | 10 个端点 |
| **自动算价引擎** | 成本快照、加价率/折扣、MOQ 与币种校验、手工改价标记、服务端重算 |
| 报价转订单 | 快照单价金额，商机自动置 WON |

**验收**：改数量或加价率后金额自动重算；前端传的 `unitPrice` 被忽略；成本币种不符被拒；MOQ 不足被拒；利润汇总正确。

### M4 · 订单与收款

| 任务 | 产出 |
| --- | --- |
| 订单 CRUD + 状态机 | 7 个端点 |
| 发货（支持分批）+ 应收（定金/尾款拆分）+ 核销 + 账龄 | 6 个端点 |

**验收**：首次发货订单置 SHIPPED；定金尾款两条应收；核销后状态流转；订单在发货且全额收款后置 COMPLETED。

### M5 · 邮件 · 汇率 · 站内信

| 任务 | 产出 |
| --- | --- |
| 邮件模板 / 群发 / 发送记录 | 8 个端点 |
| 汇率：4 张表 + 双源同步 + 交叉比对 + 熔断 + 定时任务 | 7 个端点（见专项文档） |
| 站内信：表 + 事件触发 + 未读数 + 轮询 | 7 个端点 |
| 站内信清理任务、汇率同步任务（cron，`TZ=Asia/Shanghai`） | 定时任务 |
| 看板补齐（账龄、产品排行、顶部指标） | 2 个端点 |

**验收**：报价被拒能收到站内信；未读数正确；汇率同步后两源偏差超阈值的币种被拒绝入库；熔断币种停用。

### 上线前检查

- [ ] `./mvnw -B test` 通过
- [ ] `./mvnw -B -DskipTests package` 通过，jar 仍是 `target/ProjectInfomationManage.jar`
- [ ] Flyway 迁移在**空库**与**存量库**上都能跑通，校验和不匹配
- [ ] 容器 env 设置 `TZ=Asia/Shanghai`
- [ ] `crm:access` 权限点已授予业务员角色
- [ ] 无 secret / 真实凭据进仓库
- [ ] Swagger 注解齐全（类级 `@Tag` + 方法级 `@Operation`）
- [ ] 业务异常抛 `IllegalArgumentException` / `UnauthorizedException` / `ForbiddenException`，未在 Service 里 try-catch 吞掉

---

## 7. 非功能需求

| 维度 | 要求 |
| --- | --- |
| 性能 | 列表接口 P95 < 300ms（10 万客户量级）；看板 P95 < 1s；Excel 导出限 1 万行**流式写出**（内存恒定）；汇率折算只读本地表 |
| 分页 | PageHelper 统一处理；深分页 `page × size ≤ 10000`；排序字段走白名单 |
| 幂等 | 不做幂等键；重复提交由唯一索引 + 状态机兜底 |
| 并发 | 单号生成由应用侧加锁 + 唯一索引兜底；定时任务单实例执行（当前单副本部署） |
| 安全 | 复用现有 JWT；CRM 路径统一校验 `crm:access`；第一版**不做**敏感字段脱敏 |
| 审计 | 关键操作（客户转交、收款核销、汇率手工改数）写**结构化应用日志**（SLF4J），**不建审计表**；长期留痕用站内信 |
| 数据可信度 | 汇率入库须过**双闸门**：两源交叉比对 + 合理性校验（见专项文档 §4） |
| 外部依赖 | 汇率接口为免费第三方服务、无 SLA；读路径只读本地缓存，同步失败不影响业务可用性 |
| 语言 | **系统文案一律中文**；不做 i18n；仅客户域自由文本可含外文（§8.3） |
| 时区 | 数据库与应用统一 `Asia/Shanghai`；容器必须设 `TZ` 环境变量 |
| 可观测 | 结构化日志 + 埋点：询盘响应时长、报价转化率、逾期应收数、未读消息积压数、汇率同步成功率与拒绝数 |

---

## 8. 附录

### 8.1 枚举字典清单

| `type_code` | 枚举值 |
| --- | --- |
| `customer_source` | 网站 / 展会 / 阿里 / 邮件 / 转介绍 / 其他 |
| `customer_level` | A / B / C |
| `customer_status` | POTENTIAL / FOLLOWING / DEALED / LOST |
| `opportunity_stage` | INITIAL_CONTACT / QUALIFIED / QUOTED / NEGOTIATION / WON / LOST（`extra` 存默认赢率） |
| `incoterm` | EXW / FOB / CFR / CIF / DDP |
| `payment_term` | TT / LC / DP / DA / OA / PAYPAL |
| `product_category` | 按业务实际维护 |
| `message_type` | 见 §3.11 触发事件表 |

### 8.2 电话号码规范（E.164）

| 项 | 规则 |
| --- | --- |
| 格式 | `+` 开头，后接国家码与号码，**不含空格 / 横线 / 括号** |
| 正则 | `^\+[1-9]\d{1,14}$` |
| 长度 | 去掉 `+` 后 2~15 位数字（E.164 上限 15） |
| 国家码 | 不能以 `0` 开头（`+0` 非法） |
| 示例 | `+14155552671`（美国）、`+4915112345678`（德国）、`+8613800138000`（中国） |

落地要求：

- **入库前校验**：Service 层统一调用 `E164PhoneValidator`（放 `common` 包），非法直接抛
  `IllegalArgumentException`，文案提示期望格式（`phone must be E.164 format, e.g. +14155552671`）。
- **库中存储**：`VARCHAR(20)`。
- 后端**不做**自动猜测国家码；前端录入时按所选国家提示转换。
- 唯一性：同一客户内唯一（§5.3.2）。

### 8.3 语言策略

| 类别 | 范围 | 规则 |
| --- | --- | --- |
| 系统文案 | 站内信、字典项、看板指标名、接口 `message`/`errors`、日志、导出表头 | **一律中文**，不随任何字段变化 |
| 客户域自由文本 | 客户公司名 / 简称 / 地址 / 备注、联系人姓名 / 职位 / 备注、客户标签名、邮件主题与正文 | **原样存储、原样展示**，不翻译、不识别语种 |

**不做**：i18n 框架 / `MessageSource` / 语言包 / 多语言字典项 / 按 `language` 切换系统文案 / 自动翻译。
`language` 字段只作人工标记，不驱动任何行为。全库 `utf8mb4_unicode_ci`。

### 8.4 相关文档

| 文档 | 内容 |
| --- | --- |
| [汇率数据接入设计](./crm-currency-rate-design.md) | 汇率选型实测、双源交叉比对、熔断策略、汇率表结构与接口、配置项 |
| `AGENTS.md` / `CLAUDE.md` | 工程规范、构建命令、代码风格红线 |
| `README.md` | 部署流程与运行手册 |
