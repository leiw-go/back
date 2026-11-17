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
| Provider 存储 | `t_llm_provider`（base_url / api_key AES-GCM / enabled / priority） | API key 不入日志不进响应 |
| 用量落库 | `t_llm_request_log`（prompt/completion/total token、status、latency、trace_id） | 流式在 `onCompletion` 写库 |
| 链路追踪 | `traceId` 全链路（MDC + OpenTelemetry LoggingSpanExporter） | `t_llm_request_log.trace_id` |
| 鉴权 | `/design/v1/**` 与 `/design/api/**` 共用 JWT | `AuthInterceptor` |

**已配置的 provider（2026-11-17 实测通过）**：

| 项 | 值 |
| --- | --- |
| provider 名 | `minimax-cn`（请求里的 model 写 `minimax-cn:MiniMax-M3.1-Flash`） |
| base_url | `https://api.minimax.cn`（**国内站**；国际站 `api.minimax.io` 用同一把 key 返回 401，已删除该 provider） |
| 上游模型 | `MiniMax-M3.1-Flash` |
| 密钥 | AES-GCM 加密存 `t_llm_provider.api_key_ciphertext`，由 `LLM_ENCRYPT_KEY`（dev 用默认占位密钥）解密 |

> 注意 relay 拼上游 URL 的方式是 `baseUrl + "/v1/chat/completions"`，所以 **base_url 里不能带 `/v1`**。

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
| `content` | string | | 草稿正文（≤ 8000 字）。**有内容 = 微调，无内容 = 直接生成** |
| `subject` | string | | 草稿里的原始主题（可选，作为上下文） |
| `language` | string | ✅ | 目标语言码：`en`/`de`/`es`/`fr`/`pt`/`ru`/`ja`/`ko`/`ar`/`it`/`zh` |
| `tone` | string | ✅ | `new_customer`/`existing_customer`，默认 `new_customer` |
| `scene` | string | | `inquiry_reply`/`quotation`/`order_confirm`/`shipment`/`payment`/`complaint`/`exhibition`/`followup`，默认 `inquiry_reply` |
| `customInstruction` | string | | 使用者额外提出的特殊场景与要求（优先级高于 `scene`） |
| `customerName` | string | | 客户公司名或称呼（进 prompt，决定称呼写法） |
| `senderName` | string | | 我方发件人姓名，默认 `leon` |
| `senderCompany` | string | | 我方公司名 |
| `senderTitle` | string | | 我方职位，默认 `销售业务员` |
| `model` | string | | `provider:model`，默认取 `options.defaultModel` |
| `temperature` | number | | 默认 0.3（改写要稳定，不要发散） |
| `maxTokens` | int | | 默认 4096 |

> `content` 与 `customInstruction` **至少要有一个**，否则后端直接拒绝（`请填写草稿内容，或说明特殊场景与要求`）。

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
- Controller 返回 `SseEmitter`；在 `streamExecutor` 里调 `OpenAiRelayService.chatCompletion(stream=true)`，把 relay 的 OpenAI 帧 **转译**成本协议的 `delta` 帧
- relay 是**原样转发**上游帧（实测帧序），转译要认这四种帧：

  | 上游帧 | 含义 | 转译 |
  | --- | --- | --- |
  | `choices[0].delta.reasoning_content` | M3.1 的思考过程（该模型总是先思考） | → `reasoning` 事件，**字段名用 `thinking` 而不是 `content`**（否则前端"只按 content 拼接"会把思考混进正文） |
  | `choices[0].delta.content` | 正文增量 | → `delta` 帧（字段名 `content`） |
  | `choices: []` 且带 `usage` | 收尾用量帧 | 暂存 usage |
  | `data: [DONE]` | 流结束哨兵 | → `done` 帧（带 usage / 耗时 / finishReason） |

  ⚠️ `finish_reason` 必须显式映射：Jackson 默认按驼峰匹配，`finish_reason` 映射不上会一直是 null
  （已在 `ChatCompletionChunk.Choice` 上补 `@JsonProperty("finish_reason")`）。

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

**系统提示**（模板占位：`{language}` `{tone}` `{scene}` `{sender}` `{custom}`）：

```
你是一名资深的外贸业务经理，负责写专业、简洁、干练的{targetLanguage}商务邮件。

硬性要求：
1. 输出格式固定为两段：第一行写 “Subject: <邮件主题>”（主题用{targetLanguage}，不要加任何前缀或解释）；然后空一行，接着写邮件正文（正文开头不要再重复主题）。除主题行与正文外，不输出任何解释、注释、前言、总结或 Markdown 代码块。
2. 简洁、干练、专业：一句话一个要点，删掉客套铺垫与空洞赞美，全文控制在 150 词以内。
3. {modeClause}
   - 微调：严格保留草稿中的事实性信息（型号、规格、数量、价格、金额、日期、港口、贸易术语、付款方式、联系人、公司名、附件说明），不得新增、臆测、换算或修改任何数据；草稿里的主题行也要一并优化。
   - 从零撰写：绝对不要编造任何具体的型号、价格、金额、日期与条款；需要这些信息的地方用明确占位（如 [型号]、[价格]、[交期]）或直接向客户索取。
4. 全文必须使用{targetLanguage}，包括主题、称呼、正文、结尾敬语与署名标签。
5. {toneHint}
6. 结构：主题 → 称呼（Dear {customerName}，使用该语言惯用写法）→ 正文（2~4 段，按意图顺序）→ 明确的行动请求 → 结尾敬语（该语言惯用敬语）→ 署名（我方发件人信息）。
7. 场景约定：{sceneHint}
8. {customClause}
9. 上下文里的称呼、落款如果没有给出，就用通用占位（如 Dear Sir or Madam / Best regards），不要编造具体人名。
```

**语气 → 话术映射**（`toneHint`）—— **只有两档，按客户关系分**：

| tone | 含义 | 话术要点 |
| --- | --- | --- |
| `new_customer` | 新客户语气 | 非常客气、礼貌、友好。请求对方帮忙用委婉句式：“Could you please help me ...”、“I would appreciate it if you could ...”、“Would it be possible to ...”；表达感谢与期待，不要生硬命令式的祈使句 |
| `existing_customer` | 老客户语气 | 随和、亲近、简单直接，不用过度客气。请求对方帮忙用轻松句式：“Please can you ...”、“Just let me know if ...”；可以有一句自然的过渡，但仍保持专业 |

> 两种语气**都**受第 2 条约束（简洁、干练、专业、全文 ≤ 150 词）—— 这一条写在固定骨架里，不随语气变化。


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
{ draftClause }
客户：{customerName}
我方发件人：{senderName}，{senderCompany}，{senderTitle}
```

- 有草稿（微调）：`待微调的草稿（可能包含上一轮的改进结果）：\n{content}` + （有 `subject` 时追加）`草稿里的原始主题：{subject}`
- 无草稿（生成）：`没有草稿，请直接写一封新的邮件。`
- `senderName` 缺省 `leon`，`senderTitle` 缺省 `销售业务员`，`senderCompany` 缺省用占位说明

**模型参数**：`temperature=0.3`（稳定优先）、`maxTokens=4096`、`stream=true`。
> 实测：M3.1 是推理模型，一次简单改写的 completion token 里 **思考占了 1300+**（总 1362），
> `maxTokens` 给 2048 有被截断的风险，故默认提到 4096。

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
| 路由 | `/email-engine`（`name: 'email-engine'`），**顶层菜单**，与「产品管理」同级 |
| 图标 | `robot` |
| i18n | `menu.email-engine` × 8 个 locale（zh-CN「邮件引擎」） |
| 组件目录 | `src/pages/crm/email-engine/` |

### 5.2 页面布局

```
PageContainer（标题：邮件引擎 · AI 商务邮件改写）
└ Row gutter=16
  ├ Col span=9（左）
  │  ├ Card「草稿（微调）」/「草稿（可留空）」
  │  │   右上角图标按钮：粘回草稿（把生成结果填回草稿）、清除草稿
  │  │   顶部 Alert 说明当前模式（有草稿=微调 / 无草稿=生成）
  │  │   TextArea（8 行，字数统计 812/8000）
  │  │   草稿里的原始主题 Input（可选）
  │  └ Card「改写参数」
  │      特殊场景与要求 TextArea（优先级高于业务场景）
  │      目标语言 Select（options 接口返回）
  │      语气 Segmented（新客户语气 / 老客户语气）
  │      场景 Select
  │      模型 Select（options 接口返回，默认 defaultModel）
  │      发件人：姓名（默认 leon）/ 公司 / 职位（默认 销售业务员）
  │      按钮：▶ 微调改进 / ▶ 生成邮件（主按钮，随模式切换）、■ 停止、清空
  └ Col span=15（右）
     └ Card「微调结果」/「生成结果」
         主题区（单独一块）
         正文区（等宽字体，逐字 append；生成中显示光标闪烁）
         折叠区：发送给 AI 的提示词（system / user）、AI 思考过程
         底部工具条：复制 / 下载 .txt / 保存为邮件模板
         底部信息条：模型、耗时、token 用量（prompt/completion/total）
```

交互细节：
- **占位符标红**：prompt 要求模型「不知道的地方用占位（如 [型号]、[价格]、[交期]）」，
  占位符没填就发出去是要出事的 —— 草稿与结果里 `[...]` / `{...}` 形式的占位符都标红
  （取 antd 的 `colorError` token）。
  结果区是普通 DOM，直接渲染红色 span；
  草稿区是原生 textarea（渲染不了富文本），用**镜像层**实现：
  textarea 文字设透明（只留光标），底下压一层同样字体/内边距/换行规则的 `<pre>`，
  两者的宽度（`clientWidth`，排除滚动条）与滚动位置实时同步。
- 草稿卡片右上角两个图标按钮：
  - **粘回草稿**：把本次生成结果（正文填进草稿框、主题填进「草稿里的原始主题」）写回草稿，
    于是可以改一次语气或特殊要求再跑一轮 —— 后端走的是微调模式，草稿里的事实不会被改坏。
    没有结果时按钮置灰；结果超长（> 8000 字）自动截断并警告。
  - **清除草稿**：清空草稿内容，同时清掉草稿里的原始主题。
- 草稿为空 **且** 特殊场景与要求为空时点主按钮 → 提示「请填写草稿内容，或说明特殊场景与要求」
- 生成中：参数区只读，防中途改参数导致结果与参数不一致
- 停止：AbortController 中断 fetch，同时通知后端断连（后端 relay 已按 499 落库）
- 结果区支持"复制"（navigator.clipboard，复制的是 主题 + 正文）与"下载 .txt"
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
src/pages/crm/email-engine/highlight.tsx   占位符高亮（镜像层输入框 + 只读文本）
src/pages/crm/email-engine/stream.test.ts  流式解析单测（用假 ReadableStream）
src/pages/crm/email-engine/highlight.test.tsx  占位符拆分与镜像层单测
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
| 0 | ✅ **已完成**（2026-11-17）：合并 `feature/openai-llm-relay` → `master`，Flyway `V2__add_llm_relay.sql` 改名 `V9__add_llm_relay.sql`、删除 relay 的 V1；provider 录入 `minimax-cn` 并实测 `/v1/chat/completions` 非流式/流式均通 | 分支可编译可启动，LLM 可调 |
| 1 | ✅ **已完成**：DTO / 三个枚举 / PromptBuilder / FrameTranslator / Service / Controller + 17 个单测；relay 侧加了 `SseFrameTranslator` 钩子（复用 provider 路由、密钥解密、用量计量、断连处理，只换输出协议） | `/api/crm/email-engine/**` 可用（curl 已验证 SSE） |
| 2 | ✅ **已完成**：路由 + 8 locale + 页面 + `stream.ts`（fetch + ReadableStream）+ 13 个单测 | `/crm/email-engine` 可用 |
| 3 | ✅ **已完成**：真实 provider 上跑通中/英/德三语，多语气多场景；经前端 dev 代理（浏览器同源路径）验证通过 | 验收 |
| 4 | ⬜ 部署：后端镜像重建 + 线上 provider 录入 + 前端 rsync（见前端 README「部署」） | 线上可用 |

**阶段 0 的遗留项**：线上服务器（43.143.244.13）的 `backend` 容器仍是合并前的镜像
（`leiw-go/back:crm-20261113`），要用邮件引擎必须重建后端镜像并在线上库录入 provider
（线上 `LLM_ENCRYPT_KEY` 需与录入时一致，否则密文解不开）。

---

## 8. 待确认项

1. ~~**用哪个大模型厂商**？~~ ✅ 已定：**MiniMax `MiniMax-M3.1-Flash`**（provider `minimax-cn`，base_url `https://api.minimax.cn`）。
2. ~~**provider 的 api_key 怎么给**？~~ ✅ 本地已录入（`t_llm_provider`，AES-GCM 加密）；**线上环境待录入**（后端镜像重建时一并处理）。
3. **目标语言清单**够不够？当前列了 11 种（含中/英/德/西/法/葡/俄/日/韩/阿/意）。
4. ~~**语气枚举**？~~ ✅ 已定：**只保留两档** —— `new_customer`（新客户语气，非常客气礼貌友好）/ `existing_customer`（老客户语气，随和亲近简单）。两种语气都要求简洁、干练、专业。
5. ~~**是否要 AI 顺带生成主题行（subject）**？~~ ✅ 已定：**要** —— 输出固定为 `Subject: <主题>` + 空行 + 正文，后端在 `done` 帧里把两者拆开回传（`subject` / `body`），前端分别展示。
6. **改写历史要不要落库**？一期只落模板；`t_llm_request_log` 有 token 与耗时，可满足成本审计。
7. **是否限制单次调用长度**？当前 content ≤ 8000 字、maxTokens 4096。注意 M3.1 是推理模型，思考 token 也算 completion_tokens —— 实测一次简单改写约 200 prompt / 40 completion（含 37 reasoning tokens），maxTokens 建议不低于 1024，否则正文会被思考挤掉。
8. **是否要给"邮件引擎"单独开权限**（如仅销售角色可用）？当前所有登录用户可用。
9. ~~**是否展示 AI 的思考过程**？~~ ✅ 已定：**要** —— 页面折叠区展示「发送给 AI 的提示词」（system / user 两段）与「AI 思考过程」。


---

## 9. 实测记录（2026-11-17）

### 接口

```
POST /design/api/crm/email-engine/rewrite     → text/event-stream
GET  /design/api/crm/email-engine/options    → 语言 11 / 语气 2 / 场景 8 / 默认模型 minimax-cn:MiniMax-M3.1-Flash
POST /design/api/crm/email-engine/templates   → 存为 t_email_template
```

### 一次完整调用（中文询盘 → 英文商务邮件，干练 / 询盘回复）

事件分布：`meta ×1`、`reasoning ×273`、`delta ×15`、`done ×1`；
耗时 8.4s，usage = prompt 594 / completion 768 / total 1362。

最终正文（只拼 `delta` 帧）：

```
Dear Acme Trading Co.,

Thank you for your inquiry about our LED strip lights. We have received your request for the unit price of 1,000 pcs and the delivery schedule.

Please share the LED strip specifications so that we can provide an accurate quotation and confirm the delivery date. Once we receive the details, we will reply with the unit price and lead time.

Best regards,
Wang Lei
GreatBus
Sales Manager
```

事实核对：`1000 pcs` 与「单价 / 交期」两个诉求都保留了；没有编造价格与交期（模型按"无规格不能报价"
主动转为索取规格）；称呼用了客户名；结尾敬语与署名是目标语言 + 我方信息。

---

## 10. 实测记录（2026-11-18）—— 语气两档 + 特殊场景 + 主题拆分

### 改动

- 语气只保留两档：`new_customer`（非常客气礼貌友好）/ `existing_customer`（随和亲近简单）
- 两种语气都强制简洁、干练、专业（≤ 150 词）
- 新增「特殊场景与要求」（`customInstruction`），优先级高于 `scene`
- 草稿（`content`）与特殊要求二选一即可：**有草稿 = 微调，无草稿 = 直接生成**
- 我方联系人默认 `leon`、我方职位默认 `销售业务员`
- 输出固定为 `Subject: <主题>` + 空行 + 正文；`done` 帧回传拆好的 `subject` / `body`

### 直接生成（无草稿，只给特殊要求）

请求：`customInstruction = "We met at the Canton Fair. Ask about their LED strip order and offer a sample before quoting a price."`，`tone = new_customer`。

耗时 10.2s，usage = prompt 687 / completion 772 / total 1459。

```
Subject: LED Strip Order Follow-Up and Sample Offer

Dear Sir or Madam,

Thank you for your inquiry and for taking the time to meet me at the Canton Fair. I would appreciate it if you could let me know the status of your LED strip order, including the required specification, quantity, and application.

Could you please share the product details or reference you are interested in? We would be pleased to prepare a sample for your review before providing a quotation, allowing you to confirm suitability first.

Would it be possible to send us the LED strip specifications and estimated quantity? I will then follow up regarding the sample arrangement and expected timing.

Best regards,
Leon
Sales Representative
[Company Name]
```

核对：新客户语气的三句委婉句式（I would appreciate it if you could / Could you please / Would it be possible）
全部命中；**没有编造型号与价格**，公司名用 `[Company Name]` 占位；署名取默认 `leon` + `销售业务员`。

### 微调（有草稿，老客户语气）

请求：草稿 `Dear, we want 1000 pcs LED strip 5050 RGB. Please send quotation and lead time. Thanks.`，
`tone = existing_customer`，`customerName = Acme Trading Co.`，`senderCompany = GreatBus`。

耗时 3.8s，usage = prompt 700 / completion 393 / total 1093。

```
Subject: 5050 RGB LED Strip Quotation and Lead Time

Dear Acme Trading Co.,

We are interested in 1,000 pcs of 5050 RGB LED strips.

Please send us your quotation and lead time.

Best regards,
Leon
GreatBus
Sales Executive
```

核对：`1000 pcs`、`5050 RGB` 两个事实原样保留；老客户语气直白（"Please send us your quotation and
lead time."），没有堆客套；主题行由模型优化但没改事实。

