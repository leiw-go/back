# OpenAI 兼容大模型调用中转 + 用量计量

## 需求

做一个 OpenAI 兼容协议的 LLM API 网关（中转）。客户端按 OpenAI 规范发请求（`/v1/chat/completions`、`/v1/embeddings`、`/v1/models`），网关把请求转发到后端真实的大模型厂商（OpenAI / Azure OpenAI / DeepSeek / 阿里 DashScope 兼容模式 / 自建 Ollama 等），再把响应原样返回。同时具备 **token 用量计量** 能力：每次调用的 prompt / completion / total token 数都要落库，前端/调用方可查询用量统计。

## Done when

- [ ] 1. `POST /design/v1/chat/completions` 按 OpenAI Chat Completions 规范工作：接受 `model` / `messages` / `temperature` / `stream` 等字段，返回 `id` / `object` / `created` / `model` / `choices` / `usage` 字段，JSON 结构与 OpenAI 官方文档完全一致。`curl -X POST` 用 `messages: [{"role":"user","content":"hi"}]` 调通一次非流式响应。
- [ ] 2. `stream=true` 时按 SSE（`text/event-stream`）输出，每个 chunk 为 `data: {json}\n\n`，以 `data: [DONE]\n\n` 结尾，途中不丢字段、不丢顺序；客户端可用 `curl -N` 看到逐 token 输出。**实现用 Spring MVC 的 `SseEmitter`**：Controller 返回 `SseEmitter`（初值 0 表示无限超时），通过 `emitter.send(SseEventBuilder)` 转发上游 chunk；`onCompletion` / `onTimeout` / `onError` 三个回调负责清理上游订阅与写日志。Controller 方法签名需要支持 Servlet 异步（无需 `@EnableAsync`，但要避免在方法体里阻塞）。
- [ ] 3. `POST /design/v1/embeddings` 与 `GET /design/v1/models` 同样按 OpenAI 规范返回（`data: [{embedding: [...]}]` / `{data: [{id, object, owned_by, ...}]}`）。`/v1/models` 列表来自配置的 provider×model 笛卡尔积，不是固定写死。
- [ ] 4. **`/design/v1/**` 与现有 `/design/api/**` 共用同一套 JWT 鉴权**（端用户通过现有 `POST /design/api/auth/login` 拿 token，再用同一 token 调 `/design/v1/chat/completions`）。`AuthInterceptor` 的注册路径从 `/design/api/**` 扩到同时覆盖 `/design/v1/**`（改 `WebMvcConfig#addInterceptors`），不引入新的拦截器。缺失/过期/非法 token → `401`，body 沿用现有 `ResponseResult` 风格 `{"code":401,"message":"Unauthorized"}`；OpenAI SDK 把 `api_key` 字段直接填 JWT 字符串即可工作。
- [ ] 5. **用量计量**：每次调用（无论成功失败）落库一行 `t_llm_request_log`，至少含 `user_id`（JWT subject）/ `provider` / `model` / `endpoint`（`chat_completions` / `embeddings`）/ `prompt_tokens` / `completion_tokens` / `total_tokens` / `status_code` / `latency_ms` / `request_id` / `created_at`。非流式：响应解析完直接写库；**流式：在 `SseEmitter` 的 `onCompletion` 回调里写库**，触发时机是上游最后一个含 `usage` 的 chunk 到达并 `emitter.send()` 成功之后；上游未返回 usage 的情况下，`prompt_tokens` 用本地 tokenizer 估算、`completion_tokens` 记 0、`total_tokens` = 二者之和，并在日志里 `LOG.warn` 标记 `usage_missing=true`。
- [ ] 6. 上游 provider **通过 DB 接入**：新增 `t_llm_provider` 表（`name` / `base_url` / `api_key` / `enabled` / `priority` / `timeout_ms_override` / `created_at` / `updated_at`），配合管理端 API `POST/GET/PUT/DELETE /design/api/llm/providers`（走现有 JWT + ADMIN 角色）做 CRUD。运行时把 provider 列表缓存到内存（TTL 30s + 管理端写后立即 invalidate），新增/启停 provider 不需要重启。`api_key` 列不进任何日志、错误信息或响应体。
- [ ] 7. 模型路由：请求里的 `model` 形如 `"openai:gpt-4o-mini"` / `"deepseek:deepseek-chat"`，网关按 `provider:model` 拆解并路由；未配置 / 已 `enabled=false` 的 provider 直接返回 OpenAI 风格 `404 + error.type="invalid_model"`。
- [ ] 8. 提供 `GET /design/api/llm/usage` 管理端查询接口（带现有 JWT 鉴权）：支持按 `user_id` / `provider` / `model` / 时间区间聚合返回 `total_calls` / `total_prompt_tokens` / `total_completion_tokens` / `total_tokens`，分页参数沿用现有约定（具体看 `LotteryPeriodController` 的写法）。ADMIN 角色看全部；普通用户看自己（强制 `user_id = currentUser.id`，不允许 query param 覆盖）。Swagger `@Tag` + `@Operation` 齐全。
- [ ] 9. 反向路径：上游 4xx/5xx 时把上游 status + body 转成 OpenAI 风格 `error` 字段透传给客户端（不暴露我们的内部栈）；超时（默认非流式 60s / 流式 read 0；单 provider 可在 `t_llm_provider.timeout_ms_override` 覆盖 —— 见 `需要你确认`）返回 `504 + error.type="upstream_timeout"`；客户端断连（流式中途）由 `SseEmitter.onTimeout` 触发，取消上游 `RestTemplate`/`WebClient` 调用、把已收到的 chunk 数与已知 token 数写一行 `status_code=499` 的 log，不抛 500。
- [ ] 10. 新增 Flyway `V2__*.sql`：`t_llm_provider` / `t_llm_request_log` 两张表 + 必要索引（按 `user_id + created_at`、`provider + model + created_at`）。`V1__baseline_project_info_manage.sql` 不动。已执行 migration 文件不得修改。
- [ ] 11. 单元测试覆盖核心纯逻辑：token 估算器（多种 model 族、含中文/emoji）、model 字符串解析（`"openai:gpt-4o-mini"` → provider/model）、OpenAI 错误响应序列化。`./mvnw -B test` 离线通过；context 冒烟测试沿用 `ProjectInformationManageApplicationTests` 风格（新 controller / mapper 用 `@MockBean` 占位）；新增 `t_llm_request_log` 写入走 `@MockBean` Mapper，**不连真实 MySQL**。
- [ ] 12. **traceId 全链路追踪**：每个进入 `/design/**` 的 HTTP 请求生成或继承一个 32 位十六进制 `traceId`（遵循 W3C Trace Context 标准）：请求头带 `traceparent` 则沿用上游；否则新生成。**实现走 `io.opentelemetry:opentelemetry-sdk`**（手动装配，不走 `opentelemetry-spring-boot-starter` —— 见 `需要你确认`）：
    - `OpenTelemetryConfig` 装配 `OpenTelemetrySdk` + `SdkTracerProvider` + `LoggingSpanExporter`（dev 启用、生产可在 `application.yml` 关掉），暴露 `OpenTelemetry` Bean 供全应用注入。
    - 自定义 `TraceIdFilter`（`OncePerRequestFilter`，只对 `/design/api/**` 与 `/design/v1/**` 生效 —— 见 `需要你确认`）：进入时 `tracer.spanBuilder("http.request").startSpan()` → 把 `trace_id` / `span_id` 写入 SLF4J MDC → `try/finally` 在响应回到 filter 时 `span.end()` + `MDC.clear()`。
    - `SseEmitter` 异步回调用 `Span.current().makeCurrent()` 包一层，确保流式线程也能从 MDC 取到 traceId；`RestTemplate` 出站调用通过 `ClientHttpRequestInterceptor` 把当前 `Context` 注入到 `traceparent` header 传给上游。
    - logback 配置加 pattern：`%X{trace_id:-no-trace}` 与 `%X{span_id:-no-span}`（沿用 `io.opentelemetry.instrumentation:opentelemetry-logback-mdc-1.0` 把 span context 镜像到 MDC）；所有日志行自动带 traceId，无需在每个 `LOG.info` 里手动加。
    - `t_llm_request_log` 表新增 `trace_id VARCHAR(32)` 列（Flyway `V2__add_llm_relay.sql` 里加），落库时填入 `Span.current().getSpanContext().getTraceId()`。
    - 新增单元测试：(a) 模拟请求 → handler 线程 `MDC.get("trace_id")` 非空且 32 位 hex → (b) 模拟 SseEmitter `onCompletion` 回调 → 异步线程 MDC 也能取出同一个 traceId → (c) 模拟带 `traceparent` header 的请求 → 沿用上游 traceId 而非新生成。
- [ ] 13. `./mvnw -B -DskipTests package` 产出 `target/ProjectInfomationManage.jar`；新文件 license 头 `Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.` 齐全；顶层 public 类 Javadoc 含 `@since 2026-08-04`；`checkstyle_huawei.xml` 通过（若已挂入构建）。OpenTelemetry 新依赖走华为云镜像（已在 `settings.xml`）。`feishu.webhook.*` / 现有 `/api/**` 行为不变 —— 回归一下登录 + 产品列表两条现有路径，回归时确认这两条路径的日志行也带上 traceId。

## 明确不做

- **不做** 多租户/计费结算（只记录用量，不扣费、不发账单、不做余额/额度限制；那是另一个 spec）
- **不做** `function calling` / `tools` 参数透传增强（首版透传即可，不做 JSON Schema 校验）
- **不做** `vision` / `audio` 多模态内容字段（首版只接 `text`）
- **不做** `/v1/completions`（OpenAI 已废弃的 legacy 端点）
- **不做** 管理端 Web UI（仅 API；UI 在 `prompt-design-ui/` 单独做）
- **不做** 上游响应缓存（每次都真转发）
- **不做** 自动 fail-over（一个 provider 挂了不自动切另一个）

## 需要你确认

- **base URL 前缀** — OpenAI 官方 base 是 `https://api.openai.com/v1/...`。本服务 `server.servlet.context-path=/design`，所以默认会变成 `/design/v1/...`。客户端要"真正零修改兼容 OpenAI SDK"才能挂在根路径。倾向：**接受 `/design/v1/...` 并在 README 里写明** —— 改 servlet context-path 会把现有的 `/api/**` 也拖下水，影响太大。若你坚持根路径，告诉我，我们另开一个 spec 讨论（候选：Spring DispatcherServlet 转发 / 反向代理 rewrite / 第二端口）。

- **token 估算库** — Java 圈没有官方 `tiktoken` 实现，主流两个：`com.knuddels:jtokkit`（BPE，支持 cl100k_base / o200k_base，~1MB 资源）和 `com.azure:azure-openai-tiktoken`。倾向 **`jtokkit`**：纯本地、零网络、可商用、依赖轻。若你希望直接复用 Azure 官方包（更新更勤但依赖更多），告诉我。

- **超时配置** — 现有 `RestTemplate` 固定 5s，对 LLM 太短（普通聊天 10-30s、Embedding 5-15s、流式首字节 5s 后持续）。倾向：**为 LLM 路径单独建一个 `RestTemplate` Bean（`llmRestTemplate`），非流式 60s / 流式 read 0（不超时，受上游 keepalive 控制），默认值在 `application.yml` 的 `llm.upstream.timeout-ms` 字段，单 provider 可在 `t_llm_provider.timeout_ms_override` 列覆盖**。这会绕过现有 `RestTemplateConfig` 的 5s 默认值 —— 告诉我能不能动这块。

- **provider 存储位置**（因为你不让放 Nacos）。倾向：provider 元数据（`name` / `base_url` / `enabled` / `priority` / `timeout_ms_override`）走 `t_llm_provider` 表 + 管理端 API；上游 provider 的 `api_key` 走 `t_llm_provider` 同一行（**DB 落库**而非配置文件），用 AES-GCM 应用层加密（密钥来自 `application.yml` 的 `llm.encrypt.key`，32 字节 base64），启动时解密加载到内存缓存。**密钥本身不入 Nacos、不入日志、不入响应**。如果你倾向把 `api_key` 留 `application.yml` 明文（dev 调试方便）或走环境变量，告诉我。

- **是否限制 `/v1/**` 仅 ADMIN 可调** — 倾向：所有登录用户（含 USER 角色）都可以调 `/design/v1/chat/completions`，用量按 user_id 各自记账；ADMIN 可以替所有人查用量，普通用户只能查自己。如果你想把 `/v1/**` 限定 ADMIN 才能用（其他人只能查用量不能调），告诉我 —— 实现上加 `@PreAuthorize("hasRole('ADMIN')")` 一行的事。

- **provider 的 `api_key` 怎么存** — 既然 provider 数据全进 DB，`api_key` 也在 `t_llm_provider` 同一行。倾向：**AES-GCM 应用层加密**，密钥来自环境变量 `LLM_ENCRYPT_KEY`（32 字节 base64），启动时注入 `application.yml` 的 `llm.encrypt.key` 字段；密钥本身不入 git、不入日志、不入响应。如果你倾向 `api_key` 在 DB 里明文存（admin 应用层就你一家能查，偷表也无所谓），告诉我。

- **stream chunk 里的 usage** — OpenAI 默认 `stream_options.include_usage=false` 时流式响应**不带** usage 块，需要客户端显式传 `stream_options: {"include_usage": true}` 才会拿到最终 token 数。倾向：**网关默认把这个开关设为 true**，客户端不必懂这个细节，直接拿到 usage。不同意就告诉我（保留 OpenAI 默认行为）。

- **现有代码缺陷** — 顺手发现一条：`RestTemplateConfig` 的 5s 固定超时对所有出站 HTTP 都生效，做 LLM 中转必然要换 / 加 Bean（如上面"超时配置"）。本 spec 已经把这点覆盖了，无需单独开 issue。

- **traceId filter 的作用范围** — 倾向：**只对 `/design/api/**` 与 `/design/v1/**` 生效**（含现有 `/design/api/auth/login` 等所有受 JWT 保护的路径），actuator `/actuator/health`、`/actuator/refresh`、静态资源、Nacos 心跳不打 traceId。如果你希望 actuator 健康检查也带 traceId（便于把 Nacos / K8s liveness probe 日志串起来），告诉我 —— 实现上只是 filter 的 `addUrlPatterns` 多加一条。

- **span 数据要不要发到外部** — 倾向：**首版不发外部**，`SdkTracerProvider` 配 `LoggingSpanExporter`（dev stdout 打印，方便调试；生产在 `application.yml` 把 `llm.trace.exporter` 设为 `none` 关掉），内存中保留当前请求的 span context 即可。如果你要接 Jaeger / Tempo / 自建 OTel collector，告诉我 OTLP endpoint（环境变量 `OTEL_EXPORTER_OTLP_ENDPOINT` 注入）—— 实现上再加一个 `OtlpGrpcSpanExporter` Bean 就完事。

## 前提假设

- **依赖与版本**：JDK 17 + Spring Boot 3.2.4（来自 `pom.xml` 与 `AGENTS.md §1`），新增依赖走华为云镜像（`settings.xml` 已配），不引入 Spring AI / LangChain4j / `opentelemetry-spring-boot-starter` 等全家桶（依赖过重）：
    - `com.knuddels:jtokkit:0.6.1` —— BPE token 估算
    - `io.opentelemetry:opentelemetry-api:1.40.0` + `opentelemetry-sdk:1.40.0` + `opentelemetry-context:1.40.0` + `opentelemetry-exporter-logging:1.40.0` —— traceId 追踪核心（手动装配，不走 Spring Boot starter）
    - `io.opentelemetry.instrumentation:opentelemetry-logback-mdc-1.0:2.4.0-alpha` —— 把 OTel span context 镜像到 SLF4J MDC
    - 测试 scope 加 `io.opentelemetry:opentelemetry-sdk-testing:1.40.0`（用于 in-memory span 断言）
- **路径风格**：所有受 JWT 保护的路径走 `AuthInterceptor` 注册的 `/design/api/**` 与 `/design/v1/**`（在 `WebMvcConfig#addInterceptors` 里同时加两条 `addPathPatterns`）。`AuthController#login` / `#register` 仍然 `@SkipAuth` 放行。
- **配置来源**：provider 元数据 + 上游 `api_key` 走 DB（`t_llm_provider`），不写 Nacos、不进 git；本地 `application.yml` 仅放 LLM 模块的非敏感默认值（如 `llm.upstream.timeout-ms`、`llm.encrypt.key` 走环境变量注入），**不写真实 baseUrl / apiKey**。
- **包结构**：新增 `controller/openai/`（OpenAI 兼容入口）、`controller/llm/`（管理端用量查询）、`service/llm/`、`service/openai/`、`mapper/llm/`（如需新 Mapper）、`config/LlmProperties.java`（绑定 `@ConfigurationProperties("llm")`，**不加 `@RefreshScope`**——本模块不依赖 Nacos 热更新）、`config/OpenTelemetryConfig.java`（`OpenTelemetrySdk` + `SdkTracerProvider` Bean 装配，对应 `application.yml` 的 `llm.trace.*` 开关）、`config/TraceIdFilter.java`（`OncePerRequestFilter`，启动/关闭 root span、写 MDC）、`config/RestTemplateTraceInterceptor.java`（出站 HTTP 注入 `traceparent` header）。
- **现有约定**：DTO `XxxDto` / 实体 `Xxx` / Service 接口+`XxxServiceImpl` / Mapper 动词命名 / `ResponseResult<T>` 包装（管理端接口）/ OpenAI 原生 JSON（非管理端，不包 `ResponseResult`）/ Swagger `@Tag`+`@Operation` 全程在线 / Lombok `@Data` / 中文 Javadoc + `@since 2026-08-04`。
- **DB 演进**：本次新增表走 `V2__add_llm_relay.sql`；不修改 `V1__baseline_project_info_manage.sql`。