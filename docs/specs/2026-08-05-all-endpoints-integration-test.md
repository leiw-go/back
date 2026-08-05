# 给所有接口编写集成测试并全部通过

## 需求

把当前 Spring Boot 后端的 34 个 HTTP 接口（10 个 Controller）逐一补上集成测试，跑 `./mvnw -B test` 全部绿，且不破坏现有"离线 / 不连 MySQL 不连 Nacos"的约定。沿用现有 `AuthControllerIntegrationTests` 风格（H2 + 独立 profile + `@MockBean AuthInterceptor` 放行），不引入 Testcontainers / Failsafe / RestAssured / WireMock 这类新基建。

## Done when

- [ ] 1. **测试基建就绪**。新增 `src/test/resources/application-all-endpoints-it.yml`（profile `all-endpoints-it`）—— H2 datasource（沿用 `auth-it` 那条的 URL / `MODE=MySQL` / `DB_CLOSE_DELAY=-1` / `DATABASE_TO_LOWER=TRUE`）+ `spring.sql.init.mode: always` + Nacos 全关。**`jwt.secret` / `jwt.expiration` 不在 yml 里写，由所有 IT 类共用一个 `@DynamicPropertySource` 静态方法（沿用 `AuthControllerIntegrationTests` 模式）以 `SecureRandom` 64 字节生成，杜绝真实密钥进仓库（CLAUDE.md §10 红线）**。新增 `src/test/resources/db/h2-schema-all.sql` + `h2-data-all.sql`，覆盖 **`V1__baseline_project_info_manage.sql` 的所有 7 张表**（`t_user` / `t_role` / `t_user_role` / `t_permission` / `t_role_permission` / `t_product` / `t_lottery_period`）**加 `V2__add_llm_relay.sql` 的 2 张表**（`t_llm_provider` / `t_llm_request_log`），并把 `V1` 种子里的 `admin / admin123` 与 `testuser / test123456`（BCrypt 哈希沿用 `BCryptPasswordEncoderTests` 已验证过的那两组）搬过来；删掉旧的 `application-auth-it.yml` + `db/h2-schema.sql` + `db/h2-data.sql`，统一用 `all-endpoints-it` profile。schema / seed 必须 H2 兼容（去 `ENGINE=` / `CHARSET=` / `DEFAULT (UUID())`，UUID 用 `'550e8400-e29b-41d4-a716-446655440001'` 字面量）。新增 `src/test/java/.../testsupport/JwtTestTokenFactory.java`（test scope utility）—— 接收 `username` / `roleCode`，**调用真实 `JwtUtil.generateToken(...)` 签发 token**（从 Spring 上下文注入 `JwtUtil` bean，复用同一份 `@DynamicPropertySource` 注入的 secret），不要自己手动拼 JWT 字符串。新增 `src/test/java/.../testsupport/ExpiredJwtTestTokenFactory.java`（或同一文件里提供 `expiredToken(...)` 方法）—— 签一个 `expiration = now - 1s` 的 token 用于「过期 token → 401」断言。**所有 IT 类不 `@MockBean AuthInterceptor`**，让 `AuthInterceptor` 真实加载；token 由 `JwtTestTokenFactory` 注入 Authorization 头。**没改 `pom.xml` 新增任何依赖** —— H2 / spring-boot-starter-test / Mockito 已全在。
- [ ] 2. **`AuthControllerIntegrationTests`**（已有 1 个）扩展覆盖 3 个端点 + `@SkipAuth` 短路验证 + currentUser 三态：
    - `POST /api/auth/register` —— happy（**无 token**） + 同 username 重复注册返 `code != 200`（**无 token**）；两条都不带 token，**验证 `@SkipAuth` 真短路**。
    - `POST /api/auth/login` —— happy（**无 token**） + 密码错返 `code != 200`（**无 token**）；额外一条：带过期 token 调 login 仍 200（验证 `@SkipAuth` 不解析 Authorization header）。
    - `GET /api/auth/currentUser` 三态：合法 admin token → 200 + `$.data.username=admin` + `$.data.roleCode=ADMIN`；过期 token → 401 + `$.code=401` + `$.message` 含 "no permission"；**无 token → 401**。
    断言统一走 `jsonPath("$.code")` / `$.data.*` / `$.message`。
- [ ] 3. **`UserControllerIntegrationTests`** 覆盖 5 个端点，**每个端点三态**：admin token happy → 200、USER token → 200（**当前实现没 RBAC，记录到测试 javadoc**）、不带 token → 401。`POST /api/users` happy 断言 `$.data.username` 与入参一致；`PUT /api/users/{id}` happy 断言 `$.data.username` 已更新；`DELETE /api/users/{id}` happy 断言二次 GET 返 `code != 200`；`GET /api/users/{id}` happy 用 seed 中 admin UUID、negative case 用随机 UUID 断言 `code != 200`；`GET /api/users` happy 断言列表长度 ≥ seed 用户数。
- [ ] 4. **`RoleControllerIntegrationTests`** 覆盖 6 个端点。每个端点三态：admin token → 200、USER token → 200、不带 token → 401。`GET /api/roles/permissions` happy 断言返回 `List<Permission>` 实体的 `id` / `permissionCode` / `permissionName` 字段都在（**当前返回裸 entity 是已知 API 形态，测试 javadoc 里注一句"建议后续改为 DTO"**）；CRUD 4 个端点 + `GET /api/roles` 走 happy 字段断言 + 不存在 id 返 `code != 200`。
- [ ] 5. **`ProductControllerIntegrationTests`** 覆盖 5 个端点。每个端点三态：admin token → 200、USER token → 200、不带 token → 401。CRUD happy 断言 `$.data.id` 回写非空 + `$.data.productName` 与入参一致；`GET /api/products/{id}` negative case 用随机 UUID 断言 `code != 200`。
- [ ] 6. **`LotteryPeriodControllerIntegrationTests`** 覆盖 7 个端点。每个端点三态：admin token → 200、USER token → 200、不带 token → 401。CRUD happy 断言 `$.data.id` 回写 + 字段对得上；`GET /api/lottery/periods?page=1&size=10` 断言 `$.data.total` / `$.data.list.length` 字段名与 `PageResponse<T>` 一致；`GET /api/lottery/statistics/single?startDate=...&endDate=...` + `POST /api/lottery/statistics/multiple`（body 走 `MultiplePeriodStatisticsRequest`）输入日期用 `h2-data-all.sql` 里 seed 的 period 行覆盖区间，断言 `$.data` 字段名与 `SinglePeriodStatisticsResponse` / `MultiplePeriodStatisticsResponse` 对得上。
- [ ] 7. **`UsageControllerIntegrationTests`** 覆盖 1 个端点 `GET /api/llm/usage` 的三条分支 + 权限矩阵：
    - admin token + 无 query `userId` → 200 + `$.data` 是聚合列表（**current hardcoded 行为走 admin 分支看全部**，测试 javadoc 注一句"hardcoded `username == "admin"`，建议接入 RoleController"）；
    - USER token + 无 query `userId` → 200 + `$.data` 也是聚合列表（**current hardcoded 行为走 user 分支只看自己**，同上注）；
    - admin token + `userId=testuser-uuid` + `groupBy=model` → 200 + `$.data` 按 model 分组；
    - 不带 token → 401。
    三条 happy 都断言 `$.data` 是非空 `List<UsageAggregateDto>`；groupBy 那条额外断言 `$.data[0]` 含 `model` 字段。
- [ ] 8. **`ProviderControllerIntegrationTests`** 覆盖 4 个端点 CRUD。每个端点三态：admin token → 200、USER token → 200、不带 token → 401。CRUD happy 断言 `$.data` 字段与 `LlmProviderDto` 对得上；额外断言 `$.data` **不包含 `apiKey` / `api_key` 字段**（用 `jsonPath("$.data[*].apiKey").doesNotExist()` + `$.data[*].api_key` 双断言；当前若泄露则测试 fail 并标红）。`enabled=false` 的 provider 不出现在 `GET /api/llm/providers` 结果里（happy + 改一条 `enabled=0` 后重新 list 断言列表长度减 1）。
- [ ] 9. **`ModelsControllerIntegrationTests`** 覆盖 `GET /v1/models`：admin token → 200 + 响应是裸 JSON（**不是** `ResponseResult` 包装）+ `$.data[*].id` 形如 `"openai:gpt-4o-mini"` 这种 `provider:model` 串 + `$.object == "list"`；USER token → 200（**当前实现没 RBAC**）；不带 token → 401。`h2-data-all.sql` 里至少 seed 2 个 enabled provider × 1 个 model，保证列表 ≥ 2 条。
- [ ] 10. **`ChatCompletionsControllerIntegrationTests`** 覆盖 `POST /v1/chat/completions` 三态：
    - `stream=false` + admin token：`@MockBean OpenAiRelayService` stub `relay(...)` 返回固定 `ChatCompletionResponse`，断言响应 JSON 含 `id` / `object=chat.completion` / `choices[0].message.role=assistant` / `usage.total_tokens > 0`；
    - `stream=true` + admin token：同上但 mock 返 SSE chunks 流（`data: {...}\n\n` + `data: [DONE]\n\n`），断言响应 `Content-Type: text/event-stream` 且 body 末尾是 `[DONE]`；
    - `stream=false` + USER token → 200（**当前实现没 RBAC，OpenAI 路径允许 USER 角色调用**）；
    - `stream=false` + 不带 token → 401；
    两条 stream case 都要断言 `request_log_mapper`（`@MockBean`）收到一次调用，参数含 `user_id` / `provider` / `model` / `status_code=200`。
- [ ] 11. **`EmbeddingsControllerIntegrationTests`** 覆盖 `POST /v1/embeddings` 三态：admin token → 200 + mock 上游（`@MockBean OpenAiRelayService` 或更下层）返固定 OpenAI 风格 `{object:"list", data:[{embedding:[0.1,...], index:0}], model, usage}`，断言响应透传（`$.object == "list"` / `$.data[0].embedding` 是数组）；USER token → 200；不带 token → 401。
- [ ] 12. **`./mvnw -B test` 全绿**。包含所有新加的 `XxxControllerIntegrationTests` + 现有 12 个测试类（`BCryptPasswordEncoderTests` / `NacosConnectionStatusLoggerTests` / `WebMvcConfigInterceptorPathTests` / `TraceIdFilterTests` / `SseLineParserTests` / `ModelRouterTests` / `TokenEstimatorTests` / `OpenAiErrorTests` / `AesGcmEncryptorTests` / `GlobalExceptionHandlerContentNegotiationTests` / `ChatCompletionsControllerContentNegotiationTests` / `AuthControllerUnitTests`）。**完全离线**，不连 MySQL、不连 Nacos、不连任何真实 LLM provider。
- [ ] 13. **`./mvnw -B -DskipTests package` 通过**，产出仍是 `target/ProjectInfomationManage.jar`。新增 `.java` / `.sql` / `.yml` 顶部 license 头齐全；顶层 public 测试类 Javadoc 含 `@since 2026-08-05`。
- [ ] 14. **`AuthPermissionMatrixIntegrationTests`**（新增，专门做权限矩阵全量覆盖）—— 真加载 `AuthInterceptor`，**不** `@MockBean`，用 `JwtTestTokenFactory` + `ExpiredJwtTestTokenFactory` 覆盖以下矩阵。矩阵每条断言都用 `jsonPath("$.code")`：
    - **A. 匿名（无 token）** × 全部 31 个需 token 端点（4 个 `/v1/**` + 27 个 `/api/**` 中除 `@SkipAuth` 外）→ 全部 401 + `$.code=401` + `$.message` 含 "no permission"。参数化用 `@ParameterizedTest` + `@MethodSource` 列端点清单。
    - **B. 错 token**（签名错，secret 不匹配） × 同 31 个端点 → 全部 401。
    - **C. 过期 token** × 同 31 个端点 → 全部 401。
    - **D. 合法 USER token** × 同 31 个端点 → 全部 200（happy case 一致 + `$.code=200`）；其中 `GET /api/llm/usage` 不带 `userId` 时断言 `$.data` 列表里**不包含其他用户的 row**（验证 hardcoded user 分支确实过滤了 user_id）；`POST /api/users` 等管理类端点也返 200（**当前实现没 RBAC，测试 javadoc 明文记录："截至 2026-08-05，整个 `/api/**` 没有 role-based 鉴权，只有 `UsageController` 的 hardcoded admin 判断"**）。
    - **E. 合法 ADMIN token** × 同 31 个端点 → 全部 200（happy case 一致）。
    - **F. `@SkipAuth` 端点特殊验证**：`POST /api/auth/login` 不带 token → 200；带过期 token → 200；带错签名 token → 200（验证 `@SkipAuth` 真短路 + 不解析 Authorization header）。
    - **G. 端点清单维护**：31 个端点的清单写到一个 `EndpointCatalog` 静态方法里（路径 + HTTP 方法 + 是否 `@SkipAuth`），所有 controller IT 类与权限矩阵共用一份，**保证覆盖与代码同步** —— 新增端点忘了加测试时这条会自然 drift，参数化测试一次性捕获。
    - **断言**：每条参数化 case 拿一个三元组 `{HttpMethod, path, role-or-anonymous}`，跑完断言 `status` + `$.code` + `$.message`（401 case） / `$.data.*`（200 case）。端点分类注解在测试源码里写明，迁移到 `@Tag` 或文档都行。
    测试总计 **31 × 4（角色/状态）+ 2（@SkipAuth 短路）≈ 130+ 断言**，参数化跑一遍几秒就完。

## 明确不做

- **不做** 真实 LLM provider 出站调用（OpenAI / DeepSeek / Ollama），所有上游一律 `@MockBean`，理由：CI 跑测试时不应该消耗 token、也不依赖外网。**真实联调请用 `SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run` 手测。**
- **不做** Testcontainers / Failsafe 插件改造 —— 沿用 H2 + Surefire + `*Tests` 命名，与现有 `AuthControllerIntegrationTests` 一致；这跟 `AGENTS.md §11` 的"理想路径"有偏差，但符合"现有项目惯例 + 离线"约束。
- **不做** RestAssured / WireMock —— 用 MockMvc 就够。
- **不做** 性能 / 压力测试（JMH / Gatling）。
- **不做** 覆盖率工具（jacoco / OpenTelemetry coverage）的接入与指标看板。
- **不做** `RoleController.getAllPermissions` 返回裸 `Permission` 实体的"该不该换 DTO"重构 —— 只测现状。
- **不做** `UsageController` 的 hardcoded `username == "admin"` 角色判断重构 —— 只测现状 + 在「需要你确认」里 flag。
- **不做** 整个项目加 RBAC（`@PreAuthorize` / `RoleController` 真实角色查询 / 角色矩阵决策表）—— 矩阵测试只固化"截至 2026-08-05 没 RBAC"这个现状，详见「需要你确认」里的 flag。
- **不做** `feishu.*` / Nacos / Flyway 的集成验证（沿用项目一贯约定，单元测试层只覆盖核心纯逻辑；FEISHU `pushHelloWorld` 当前 `@Scheduled` 注释着，外部副作用测试不在本轮范围）。
- **不做** `pom.xml` 的依赖变更（包括新加 `testcontainers` / `rest-assured` / `wiremock` / `awaitility`）。
- **不做** CI（Jenkinsfile）改造 —— 本轮只让本地 `./mvnw -B test` 与 `package` 跑通。

## 需要你确认

- **`UsageController` 硬编码 admin 判断要顺手修吗** —— `src/main/java/.../controller/llm/UsageController.java` 大约 83-85 行有 `if (username.equals("admin"))` 配 `TODO: 接入 RoleController 后替换为真实角色判断`。**倾向：只测试现状 + 在测试里加注释明确"这是 hardcoded 行为"，不修实现** —— 改实现涉及到鉴权层加 `@PreAuthorize` / `RoleController` 真实角色查询，是另一条独立的横切改造，本轮只补测试、不动业务代码。若你想顺手修，告诉我，可以拆为独立 Done when 条目。

- **整个项目没 RBAC，要顺手加吗** —— 写权限矩阵时才发现：`AGENTS.md §5` 明确写了"默认无 `@PreAuthorize` 之类" + "未来加 RBAC 只变更 `AuthInterceptor` + `RoleController`"，但 `Explore` 已确认：**目前除 `@SkipAuth` 外整个 `/api/**` 没有任何角色判断**，USER 角色能调 `POST /api/users` / `DELETE /api/users/{id}` / 改 product / 改 lottery 等所有管理类操作。**矩阵测试 D / E 行就是在固化"现状就是没 RBAC"，并 javadoc 里明文标注"截至 2026-08-05 没 RBAC"。** 倾向：本轮**只把现状测下来、不补 RBAC 实现** —— RBAC 是横切改造，应该走独立 spec（加 `@PreAuthorize` + `RoleController` 真角色查询 + 角色矩阵决策表），不是 IT 任务。若你想顺手加，告诉我拆出独立 Done when；不加也明确写进 `明确不做`。

- **OpenAI 上游 mock 粒度** —— `ChatCompletionsController` / `EmbeddingsController` 真正调上游的是 `OpenAiRelayService`（按 `2026-08-04-openai-llm-relay.md` 已存在）。**倾向：`@MockBean OpenAiRelayService` 直接 stub `relay(...)` 返回值**，不引入 WireMock —— 因为 controller 的契约是"调 relay、转发响应"，mock 到 relay 这一层就够测 controller 自身的请求解析 / 响应包装 / SSE 拼装 / 用量落库回调了。若你希望 mock 到更下层（如 `RestTemplate` 出站 HTTP），告诉我，会多写一层 WireMock 配置。

- **`RoleController.getAllPermissions` 返回裸 entity 要不要在测试里吐槽一下** —— **倾向：在测试里加注释明确"当前返回的是 entity 而非 DTO，是已知的 API 形态，建议后续重构"**，不动实现。

- **`ProviderController` 的 `api_key` 字段** —— 我要先确认 `LlmProviderDto` 是不是已经把 `api_key` 标了 `@JsonIgnore`。**倾向：测试里断言"列表响应里不含 `api_key` 字段"**，若当前会泄露则 fail 测试并把这条标红作为本轮的 blocker 之一。

- **测试粒度 —— 每端点 2-3 个 case 还是只 happy path** —— 34 个端点 × 3 case ≈ 100 条测试方法，写起来量大、维护也重。**倾向：每个端点 happy path 必须 + 1 个反向 case（如 id 不存在 / 缺字段 / 无权限）必须；正向路径里"成功路径 + 字段完整性"二合一**。若有端点业务特别复杂（如 `/v1/chat/completions` 双路径）单独多写。

- **`ApplicationTests` 系列要不要一并补上** —— `AGENTS.md §11` 提到 `ProjectInformationManageApplicationTests` / `ProdProfileWithoutNacosTests` 是 smoke-context 模板，但**这两个文件当前并不在仓库里**（agent 已确认）。**倾向：本轮补 `ProjectInformationManageApplicationTests` 一个，沿用既有惯例（`@SpringBootTest` + `properties=` 排除 `DataSourceAutoConfiguration` / `MybatisAutoConfiguration` + Nacos 全关 + 7 个 Mapper `@MockBean`），断言 `contextLoads()` 不抛**；`ProdProfileWithoutNacosTests` 文档与现实有偏差，**不动它**（修了会和文档不一致，不修又好像在"装看不见"——这件事我想听你定调）。

- **Flyway 在 IT 阶段要不要开** —— 现有 `auth-it` profile 把 `spring.flyway.enabled: false`，用 `spring.sql.init` 手喂 H2 schema。**倾向：沿用 `spring.sql.init`，不开 Flyway** —— Flyway 在 H2 上跑 V1 / V2 migration 需要把 V 文件里的 MySQL 方言翻译一遍，引入额外 H2 兼容工作量（CLAUDE.md §4 已写"无需评估 H2 兼容"，但 Flyway 跑 H2 还是要解决的）。若你坚持 Flyway 真跑一遍迁移，告诉我，可以加 `V*__*.sql` 的 H2 兼容补丁。

## 前提假设

- **测试约定**：沿用 `src/test/java/com/yaowenltd/projectinfomationmanage/AuthControllerIntegrationTests` 的样式（确认于 `src/test/java/com/yaowenltd/projectinfomationmanage/controller/AuthControllerIntegrationTests.java`）—— `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("auth-it")` + H2 datasource（`jdbc:h2:mem:auth_it;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1`）+ `@MockBean AuthInterceptor` + `lenient().when(preHandle).thenReturn(true)`。
- **JWT 处理**：mock 掉 `AuthInterceptor` 之后，所有需要 token 的端点都不再校验 token；想真测"不带 token 返 401"必须保留 `AuthInterceptor` 真实加载 + 注入一个测试用 `jwt.secret`，本次只在 `AuthControllerIntegrationTests` 里留一个反向 case 验证（Done when #2）。
- **现有测试基线**：当前 `./mvnw -B test` 包含 12 个文件全绿（已确认；本次只新增 `XxxControllerIntegrationTests` 9 个 + 1 个 smoke-context 测试），不动现有的 mock 模式 / `@ExtendWith(MockitoExtension.class)` 写法。
- **MockMvc 自动剥 `server.servlet.context-path=/design`**：测试代码里写 `/api/...` 不写 `/design/api/...`（已有 `WebMvcConfigInterceptorPathTests` 文档化这个陷阱）。
- **错误响应统一形态**：所有包 `ResponseResult<T>` 的端点，错误码在 `$.code` 而非 HTTP status；只有 OpenAI 兼容端点（`/v1/**`）的 4xx / 5xx 才直接走 HTTP status。
- **`pom.xml` 不动**：H2 / spring-boot-starter-test / mockito-core / assertj-core / mockmvc / opentelemetry-sdk-testing 已在 `scope=test`，不需要新加任何依赖。
- **Controller 清单与路径**：来自 `Explore` 子代理盘点的 34 个端点（见 `AuthController.java:32` 等类声明位置 + `@RequestMapping` 路径），其中 31 个包 `ResponseResult<T>`、3 个（`/v1/models`、`/v1/chat/completions`、`/v1/embeddings`）裸返回。
- **数据库表清单**：来自 `src/main/resources/db/migration/V1__baseline_project_info_manage.sql`（7 张）+ `V2__add_llm_relay.sql`（2 张），共 9 张表。
- **`UsageController` admin 分支**：来自 `UsageController.java:83-85` 的 `username.equals("admin")` 硬编码。
- **种子用户**：`admin / admin123`（`ADMIN` 角色）与 `testuser / test123456`（`USER` 角色），见 `V1__baseline_project_info_manage.sql`。