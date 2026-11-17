# CRM 邮件引擎（AI 商务邮件改写）设计文档

> 状态：设计待评审
> 前端：`GreatBusUI`（UmiJS 4 + antd v6 + @ant-design/x）
> 后端：`GreatBus`（Spring Boot 3.2.4，context-path `/design`）

---

## 1. 目标

给外贸业务同事一个 **邮件引擎** 页面：把一段粗糙的基础邮件（中文或任意语言）交给 AI，改写成 **专业、地道、对应目标语言** 的商务邮件，**流式输出**（打字机效果），并满足"干练、礼貌"等可调语气。

同时把邮件群发链路接上：改写结果可 **一键存为邮件模板**，直接进入现有的「邮件管理 → 模板 / 群发」。

### 明确不做

- 不做通用 AI 对话（已有 `/chatbot`）
- 不做多轮会话式改写历史（一期只保留本次会话；模板是唯一持久化出口）
- 不做邮件实际发送（发送仍走现有 `POST /api/crm/email/campaigns/{id}/send`）
- 不引入新的前端依赖（不装 markdown 渲染器，输出按纯文本渲染，避免 XSS）

---

## 2. 现状盘点

### 2.1 后端已有 AI 能力（分支 `feature/openai-llm-relay`）

| 能力 | 位置 | 说明 |
| --- | --- | --- |
| OpenAI 兼容入口 | `POST /design/v1/chat/completions` | `stream=true` → SSE；`stream=false` → JSON |
| 模型列表 | `GET /design/v1/models` | 来自 `t_llm_provider` × model |
| Embedding | `POST /design/v1/embeddings` | 本期不用 |
| Provider 管理 | `POST/GET/PUT/DELETE /design/api/llm/providers` | ADMIN，写 DB 后即时失效内存缓存 |
| 用量查询 | `GET /design/api/llm/usage` | 按 user/provider/model 聚合 token |
| Provider 存储 | `t_llm_provider`（base_url / api_key AES-GCM / enabled / priority） | API key 不入日志不��响应 |
| 用量落库 | `t_llm_request_log`（prompt/completion/total token、status、latency、trace_id） | 流式在 `onCompletion` 写库 |
| 链路追踪 | `traceId` 全链路（MDC + OpenTelemetry LoggingSpanExporter） | `t_llm_request_log.trace_id` |
| 鉴权 | `/design/v1/**` 与 `/design/api/**` 共用 JWT | `AuthInterceptor` |

**流式帧格式**（中转已把上游 OpenAI 帧原样转发）：

```
data: {"id":"...","object":"chat.completion.chunk","choices":[{"delta":{"content":"Dear"}}],...}

data: [DONE]
```

无 `event:` 名字 → 默认事件名 `message`。

### 2.2 前端已有流式实现

`src/pages/chatbot/` 用 `@ant-design/x-sdk` 的 `OpenAIChatProvider` + `useXChat`（`XRequest` 支持 `headers`，可带 `Authorization`）。**可作为流式客户端的现成参考**，但它绑定的是 OpenAI 帧格式且面向多轮对话，不适合"表单 → 一次性改写"的场景。

### 2.3 缺口

1. 没有任何"把业务参数（语言/语气/场景）拼成 prompt"的业务层 —— 现有中转是通用协议
2. 前端没有一次性改写的页面与流式客户端
3. **分支未合并**：`feature/openai-llm-relay` 尚未合进 `feature/crm-backend`

---

## 3. 总体架构

```
┌──────────── GreatBusUI ────────────┐        ┌──────────── GreatBus (Spring Boot) ────────────┐
│ 页面 /crm/email-engine              │        │  POST /api/crm/email-engine/rewrite  (SSE)      │
│  ├ 左：原始邮件 + 改写参数          │  POST  │        │                                     │
│  ├ 右：流式结果 + 复制/存模板       │ ─────► │  CrmEmailEngineController                   │
│  └ stream.ts: fetch + ReadableStream│  SSE   │        ▼                                     │
└───────────────┬────────────────────┘  ◄─── │  CrmEmailEngineService                      │
                │  Authorization: Bearer JWT   │        │ 1. 组装 prompt（系统提示 + 业务上下文） │
                │                              │        ▼                                        │
                │                              │  OpenAiRelayService.chatCompletion(stream=true)│
                │                              │        │                                        │
                │                              │        ▼                                        │
                │                              │  上游 provider（DashScope / DeepSeek / ...）  │
                │                              │        │                                        │
                │                              │        ▼                                        │
                │                              │  t_llm_request_log（token 用量 + traceId）    │
                │                              └─────────────────────────────────────────────────┘
```

**为什么不直接让前端打 `/v1/chat/completions`**：prompt 需要业务语义（语言/语气/场景/事实不得编造），放后端才能统一治理、复用、审计；而且 `/v1/**` 不在 nginx 现有的 `/api/` 映射里，生产还要额外加 location。

---

## 4. 后端设计

### 4.0 前置：合并 `feature/openai-llm-relay`

这是**硬前提**。两分支的 Flyway 版本号冲突，必须先处理：

| 文件 | relay 分支 | CRM 分支 | 处理 |
| --- | --- | --- | --- |
| V1 | `V1__baseline_project_info_manage.sql` | `V1__init_project_info_manage.sql` | 内容等价（同 6 张表 + 初始数据）。**保留 CRM 分支的 V1**（线上库已应用，改名会导致 Flyway 校验和不一致） |
| V2 | `V2__add_llm_relay.sql` | `V2__init_crm_customer.sql` | **relay 的 V2 改名为 `V9__add_llm_relay.sql`**（CRM 已用到 V8） |

合并后再加邮件引擎的 migration（见 §4.5）。

### 4.1 接口一：AI 改写（流式）

```
POST /design/api/crm/email-engine/rewrite
Content-Type: application/json
Accept: text/event-stream
```

**请求体**（`CrmEmailEngineRewriteRequest`）：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `content` | string | ✅ | 原始邮件正文（≤ 8000 字） |
| `subject` | string | | 原始主题（可选，作为上下文） |
| `language` | string | ✅ | 目标语言码：`en`/`de`/`es`/`fr`/`pt`/`ru`/`ja`/`ko`/`ar`/`it`/`zh` |
| `tone` | string | ✅ | `concise`/`polite`/`formal`/`friendly`/`assertive`，默认 `polite` |
| `scene` | string | | `inquiry_reply`/`quotation`/`order_confirm`/`shipment`/`payment`/`complaint`/`exhibition`/`followup`，默认 `inquiry_reply` |
| `customerName` | string | | 客户公司名或称呼（进 prompt，决定称呼写法） |
| `senderName` | string | | 我方发件人姓名 |
| `senderCompany` | string | | 我方公司名 |
| `senderTitle` | string | | 我方职位 |
| `model` | string | | `provider:model`，默认取 `options.defaultModel` |
| `temperature` | number | | 默认 0.3（改写要稳定，不要发散） |
| `maxTokens` | int | | 默认 2048 |

**响应**：`text/event-stream;charset=UTF-8`，事件协议（自定义，比 OpenAI 帧更贴业务）：

```
event: meta
data: {"requestId":"3f2a...","model":"dashscope:qwen-max","provider":"dashscope","language":"en","tone":"concise","scene":"quotation","elapsedToFirstTokenMs":420}

event: delta
data: {"content":"Dear Mr. Smith,\n\n"}

event: delta
data: {"content":"Thank you for your inquiry."}

event: done
data: {"requestId":"3f2a...","finishReason":"stop","elapsedMs":2140,"usage":{"promptTokens":812,"completionTokens":156,"totalTokens":968}}

event: error
data: {"requestId":"3f2a...","code":"upstream_error","message":"上游返回 429，请稍后重试"}
```

- `meta`：仅第一帧，携带本次调用的模型与语言，供前端展示
- `delta`：增量文本，可直接 append（前端打字机）
- `done`：末帧，含 `usage`（从 relay 的 `t_llm_request_log` 同一份数据取，前端展示 token 消耗）
- `error`：出错时唯一一帧；前端停止 loading 并展示原因

**实现要点**：
- Controller 返回 `SseEmitter`；在 `streamExecutor` 里调 `OpenAiRelayService.chatCompletion(stream=true)`，把 relay 的 OpenAI 帧 **转译**成本协议的 `delta` 帧（解析 `choices[0].delta.content`）
- relay 的 `usage` 在 `[DONE]` 前最后一个 chunk 里，转译时把 `usage` 暂存，`done` 帧里带出去
- 客户端断连：`emitter.onCompletion` / `onTimeout` / `onError` 里置 `disconnected` 标志，停止读取上游（relay 侧已处理断连，这里只做透传）
- 超时：非流式 60s / 流式 read 0（沿用 `llm.upstream.timeout-ms` 与 provider 级 `timeout_ms_override`）
- 首次 token 超过 20s 还没来 → 发 `error` 帧（`code=first_token_timeout`）并结束，避免前端无限 loading

### 4.2 接口二：可选项（前端初始化用）

```
GET /design/api/crm/email-engine/options
→ ResponseResult<{
     languages: [{code, label}],
     tones:     [{code, label, hint}],
     scenes:    [{code, label, hint}],
     models:    [{id, label}],     // id = "provider:model"
     defaultModel: "dashscope:qwen-max"
   }>
```

`models` 由 relay 的 `listModels()` 提供（来自启用的 provider）。

### 4.3 接口三：保存为邮件模板

```
POST /design/api/crm/email-engine/templates
→ ResponseResult<CrmEmailTemplateDto>
```

请求体复用 `CrmEmailTemplateRequest`：

| 字段 | 说明 |
| --- | --- |
| `id` | 传入则更新，不传则新建 |
| `name` | 模板名（前端默认 `AI改写-{场景}-{日期}`） |
| `subject` | 主题（前端可手填，或用原文主题） |
| `content` | 改写结果全文 |
| `language` | 目标语言码（写入 `t_email_template.language`） |
| `templateType` | 场景码（写入 `t_email_template.template_type`） |

落库表 `t_email_template`（已有，字段见 `V7__init_crm_message_email_currency.sql`）：`name` / `subject` / `content` / `language` / `template_type`。保存后模板立刻出现在现有「邮件管理 → 模板」列表里，可直接群发。

### 4.4 接口四：改写记录（**一期不做**）

暂不新增 `t_email_ai_rewrite` 表。理由：token 用量已有 `t_llm_request_log` 可查；业务侧真正需要的是"改写成什么"，那由模板承担。等有"AI 改写台账"需求再补表（届时增量 migration）。

### 4.5 Prompt 设计

`CrmEmailEnginePromptBuilder` 负责拼装，规则与话术全部集中在后端（改话术不用发版前端）。

**系统提示**（模板占位：`{language}` `{tone}` `{scene}` `{sender}`）：

```
你是一名资深的外贸业务经理，负责把客户发来的基础邮件改写成专业、地道的{targetLanguage}商务邮件。

硬性要求：
1. 只输出改写后的邮件正文，不要输出任何解释、注释、前言或 Markdown 代码块。
2. 严格保留原文中的事实性信息：型号、规格、数量、价格、金额、日期、港口、贸易术语、付款方式、联系人、公司名、附件说明。不得新增、臆测、换算或修改任何数据；原文没有的信息一律不要写。
3. 全文必须使用{targetLanguage}，包括称呼、正文、结尾敬语与署名标签。
4. 语气要求：{toneHint}
5. 结构：称呼（Dear {customerName}，使用该语言惯用写法）→ 正文（2~4 段，按原文意图顺序）→ 明确的行动请求（如原文有）→ 结尾敬语（使用该语言惯用敬语）→ 署名（{sender}）。
6. 长度控制在原文的 80%~120%，删掉空话与重复表达，保留全部实质信息。
7. 场景约定：{sceneHint}
```

**语气 → 话术映射**（`toneHint`）：

| tone | 含义 | 话术要点 |
| --- | --- | --- |
| `concise` | 干练 | 直接给结论，删掉铺垫与客套，一句一个要点，全文 ≤ 150 词 |
| `polite` | 礼貌 | 保留问候与致谢，多用 please / would be grateful / kindly，不强求 |
| `formal` | 正式 | 完整敬语与敬辞，适合合同、索赔、催款等严肃场景 |
| `friendly` | 友好 | 亲切自然，适合长期客户维护，可有一句轻松的过渡 |
| `assertive` | 强势 | 明确立场与期限（催款/索赔），措辞坚定但不生硬 |

**场景 → 结构约定**（`sceneHint`）：

| scene | 含义 | 结构约定 |
| --- | --- | --- |
| `inquiry_reply` | 询盘回复 | 先确认收到并感谢，再回答问题，最后给下一步 |
| `quotation` | 报价/议价 | 先给结论（价格/交期），再说明依据与有效期，最后邀确认 |
| `order_confirm` | 订单确认 | 复述订单要素（型号/数量/金额/交期/条款）并请对方确认 |
| `shipment` | 交货通知 | 给出单号/船期/目的港/预计到港，附单证清单 |
| `payment` | 催款 | 明确金额/到期日/付款方式，给出银行信息占位，语气坚定 |
| `complaint` | 客诉处理 | 先共情致歉，再给已采取的措施与时间点，最后给补偿/后续 |
| `exhibition` | 展会邀约 | 说明亮点与展位信息，邀约具体时间，给联系方式 |
| `followup` | 跟进问候 | 简要回顾上次沟通，给出新的价值点，试探下一步 |

**用户提示**：

```
原始邮件主题：{subject}
原始邮件内容：
{content}

客户：{customerName}
我方发件人：{senderName}，{senderCompany}，{senderTitle}
```

**模型参数**：`temperature=0.3`（稳定优先）、`maxTokens=2048`、`stream=true`。

### 4.6 权限与安全

- 走现有 JWT（`AuthInterceptor` 覆盖 `/design/api/**`）→ 所有登录用户可用
- 上游 `api_key` 只存在 `t_llm_provider`（AES-GCM），本模块不接触明文
- `content` 长度上限 8000 字，防止超长输入打爆上游
- 每次调用自动落 `t_llm_request_log`（token / 耗时 / traceId），异常调用可追溯
- 不做限流（依赖 `t_llm_request_log` 的用量统计观察成本，一期不加新机制）

### 4.7 新增后端文件（预估）

```
crm/controller/CrmEmailEngineController.java
crm/service/CrmEmailEngineService.java  (+ impl)
crm/model/dto/email/CrmEmailEngineRewriteRequest.java
crm/model/dto/email/CrmEmailEngineOptionsDto.java
crm/service/email/CrmEmailEnginePromptBuilder.java
crm/service/email/CrmEmailEngineTone.java / Scene.java / Language.java   （枚举 + 话术常量）
```

复用：`OpenAiRelayService`、`CrmEmailService.saveTemplate`、`CurrentUserResolver`、`ResponseResult`。

### 4.8 测试

- 单元测试（不连真实 MySQL / 不连上游）：prompt 拼装结果快照（语言/语气/场景占位都被替换、事实保留约束存在）、SSE 帧转译（OpenAI chunk → delta 帧）、options 组装
- 冒烟测试：`CrmEmailEngineControllerTests` 用 `@MockBean` 占位 relay，验证请求体校验（content 为空 → 400）与 SSE 首帧
- Flyway：本次不新增表（模板复用现表），无新 migration

---

## 5. 前端设计

### 5.1 路由与菜单

| 项 | 值 |
| --- | --- |
| 路由 | `/crm/email-engine`（`name: 'email-engine'`），位于 CRM 分组内、与「邮件管理」相邻 |
| 图标 | `mail`（与邮件管理同族）或 `robot` |
| i18n | `menu.crm.email-engine` × 8 个 locale（zh-CN「邮件引擎」） |
| 组件目录 | `src/pages/crm/email-engine/` |

### 5.2 页面布局

```
PageContainer（标题：邮件引擎 · AI 商务邮件改写）
└ Row gutter=16
  ├ Col span=9（左）
  │  ├ Card「原始邮件」
  │  │   TextArea（8 行，字数统计 812/8000，粘贴按钮）
  │  │   原文主题 Input（可选）
  │  └ Card「改写参数」
  │      目标语言 Select（options 接口返回）
  │      语气 Segmented/Radio（concise/polite/formal/friendly/assertive）
  │      场景 Select
  │      模型 Select（options 接口返回，默认 defaultModel）
  │      发件人：姓名 / 公司 / 职位（可折叠）
  │      按钮：▶ 开始改写（主按钮）、■ 停止、清空
  └ Col span=15（右）
     └ Card「改写结果」
         流式文本区（等宽字体，逐字 append；生成中显示光标闪烁）
         底部工具条：复制 / 下载 .txt / 保存为邮件模板 / 重新改写
         底部信息条：模型、耗时、token 用量（prompt/completion/total）
```

交互细节：
- 原始内容为空时「开始改写」禁用
- 生成中：参数区只读，防中途改参数导致结果与参数不一致
- 停止：AbortController 中断 fetch，同时通知后端断连（后端 relay 已按 499 落库）
- 结果区支持"复制"（navigator.clipboard）与"下载 .txt"
- 「保存为邮件模板」→ 弹出小表单填模板名 → 调 §4.3 接口 → 成功提示并给出「前往邮件管理」链接
- 错误：卡片内红色 Alert 展示后端 `error` 帧的 message，保留原文方便重试

### 5.3 流式客户端

新增 `src/pages/crm/email-engine/stream.ts`：

```ts
export interface EmailEngineStreamHandlers {
  onMeta?: (meta: EngineMeta) => void;
  onDelta?: (text: string) => void;
  onDone?: (done: EngineDone) => void;
  onError?: (err: { code: string; message: string }) => void;
}

export function streamEmailRewrite(
  body: Crm.CrmEmailEngineRewriteRequest,
  handlers: EmailEngineStreamHandlers,
  signal?: AbortSignal,
): Promise<void>
```

实现要点：
- `fetch('/api/crm/email-engine/rewrite', { method:'POST', headers:{'Content-Type':'application/json', Authorization:`Bearer ${token}`}, body: JSON.stringify(body), signal })`
- `res.body.getReader()` 逐块解码（`TextDecoder` 带 `stream:true`），按 `\n\n` 切帧，解析 `event:` / `data:` 行
- `data` 是 JSON；`[DONE]` 之类的哨兵不出现（本协议没有）
- 结束条件：收到 `done` 帧，或流自然结束
- 401 → 跳登录页（复用现有 `requestErrorConfig` 的处理思路；fetch 不走 umi request，需要自己判断 `res.status`）

### 5.4 前端文件清单

```
src/pages/crm/email-engine/index.tsx      页面
src/pages/crm/email-engine/stream.ts       SSE 客户端
src/pages/crm/email-engine/types.ts        请求/响应类型（对应后端 DTO）
src/pages/crm/email-engine/stream.test.ts  流式解析单测（用假 ReadableStream）
```

后端接口就绪后，用 `npm run openapi:crm` 生成 `src/services/crm/crmEmailEngine.ts`（注意：SSE 接口不能走 umi 的 `request`，生成后仅用于取 options / 保存模板两个普通接口，流式接口手写 fetch）。

### 5.5 部署影响

- 邮件引擎接口在 `/api/crm/**` 下 → **现有 nginx `/api/` → `/design/api/` 映射已经覆盖，无需改 nginx** ✓
- 若将来要直连 `/v1/chat/completions`，需要在 `design.yaowen.store.conf` 与 `services.conf` 各加一条 `location /v1/ { proxy_pass http://.../design/v1/; }`（本期不做）
- 部署流程见前端仓库 `README.md` 的「部署」章节

---

## 6. 前后端契约速查

| 场景 | 方法 | 路径 | 类型 |
| --- | --- | --- | --- |
| 改写（流式） | POST | `/api/crm/email-engine/rewrite` | SSE |
| 可选项 | GET | `/api/crm/email-engine/options` | JSON（`ResponseResult`） |
| 存为模板 | POST | `/api/crm/email-engine/templates` | JSON（`ResponseResult`） |

---

## 7. 实施计划

| 阶段 | 内容 | 产出 |
| --- | --- | --- |
| 0 | 合并 `feature/openai-llm-relay` → `feature/crm-backend`，Flyway V2→V9 改名，确认 provider 可用 | 分支可编译可启动 |
| 1 | 后端：DTO / PromptBuilder / Service / Controller / 单测 | `/api/crm/email-engine/**` 可用（curl 验证 SSE） |
| 2 | 前端：路由 + 8 locale + 页面 + stream.ts + 单测 | `/crm/email-engine` 可用 |
| 3 | 联调：真实 provider 上跑通多语言、多语气、多场景 | 验收 |
| 4 | 部署：本地 build → rsync → 线上验证（见 README） | 线上可用 |

**阶段 0 的硬前提**：provider 表 `t_llm_provider` 目前是空的，必须先录入一条启用的 provider（name / base_url / api_key / priority），否则 `/v1/models` 为空、调用直接 404 `invalid_model`。

---

## 8. 待确认项

1. **用哪个大模型厂商**？DashScope（qwen-max / qwen-plus）、DeepSeek、还是别的？决定了 provider 配置与默认 model。
2. **provider 的 api_key 怎么给**？需要在管理端 `POST /design/api/llm/providers` 录入（本地或线上环境各一份）。
3. **目标语言清单**够不够？当前列了 11 种（含中/英/德/西/法/葡/俄/日/韩/阿/意）。
4. **语气枚举**是否要加"强势/谈判"这类（当前 `assertive` 覆盖催款场景）？
5. **是否要 AI 顺带生成主题行（subject）**？当前只改写正文，主题仍手工填。
6. **改写历史要不要落库**？一期只落模板；`t_llm_request_log` 有 token 与耗时，可满足成本审计。
7. **是否限制单次调用长度**？当前 content ≤ 8000 字、maxTokens 2048。
8. **是否要给"邮件引擎"单独开权限**（如仅销售角色可用）？当前所有登录用户可用。
