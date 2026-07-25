# CLAUDE.md

> 给 Claude Code / Claude Agent SDK 看的"项目专属手册"。
> 通用版在 `AGENTS.md`；这份只讲 Claude 系代理在这个仓库里需要多注意的点。

---

## 1. 启动前必读

1. 先读 `AGENTS.md` —— 项目结构、构建、鉴权、Profile 约定都在那里。
2. 续接之前的对话：用 `SendMessage` 给同名 teammate 续上下文；不要靠"我记得上次"硬猜。
3. 跨过 5 分钟缓存窗口前决定：是否需要 `ScheduleWakeup` 续 `loop`，详情见 `AGENTS.md` §1。

---

## 2. 工具与命令

### 2.1 选用工具的优先级

| 任务 | 优先用 | 退路 |
| --- | --- | --- |
| 读文件 | `Read` | `Bash` + `cat` |
| 搜代码 | `Grep` / `Glob` | `Bash` + `grep` / `find` |
| 改文件 | `Edit` / `Write` | `Bash` + `sed` |
| 跑测试 | `Bash` + `./mvnw test` | 直连 IDE |
| 跑应用 | `Bash` + `spring-boot:run`（仅验证用） | `docker compose up` |
| 跨大量文件 grep | `Agent` / `general-purpose` 子代理 | 自己 `Grep` |
| 写代码前查全局上下文 | `Explore` 子代理 | 自己读 |
| 复杂实现规划 | `EnterPlanMode` | 边写边想 |

### 2.2 常用命令（可直接复制）

```bash
# 编译 + 跑测试（不依赖 MySQL/Nacos，纯 JUnit 5 单元测试 + MockBean 上下文冒烟）
./mvnw -B -DskipTests package
./mvnw -B test

# 启动本地 Nacos（dev profile 用）
docker compose -f docker-compose.nacos.yml up -d

# 本机直接 Run（dev 默认配置）
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

# 跑特定测试类
./mvnw -B test -Dtest=BCryptPasswordEncoderTests
./mvnw -B test -Dtest=ProjectInformationManageApplicationTests
./mvnw -B test -Dtest=ProdProfileWithoutNacosTests

# 远程 Nacos 健康检查 + 配置发布
bash scripts/check-nacos.sh <host> <port> [user] [pass]
bash scripts/publish-config.sh <host> <port> <user> <pass> <profile> <config_repo_dir>
```

### 2.3 模型选择提示

- 改 schema / 改鉴权 / 改 Nacos 配置 → 默认主会话（Opus 都行），不要下放到 Haiku 子代理。
- 大规模 grep / 文档类问题 → `Explore`/`general-purpose` 子代理，**不要**全塞主上下文。
- 写代码遇到选择题（参数命名 / 路径 / bean 命名）→ `AskUserQuestion` 一次问清，**不要**边写边改。

---

## 3. 国内网络 / 代理

- Maven 镜像：`settings.xml` 已切到 `https://repo.huaweicloud.com/repository/maven/`，**不要**手动换源。
- 拉 Docker 镜像：`Dockerfile` / `docker-compose.nacos.yml` 已用 `docker.m.daocloud.io` mirror，不要改回 `docker.io`。
- 任何 `git clone/pull/push` / `gh` / `curl` 到 GitHub / Google 等已知被墙域：在动手前**主动**调用 `proxy-helper` 技能（SSH 走 GitHub 通常不需代理，优先用 SSH URL）。

---

## 4. 改 Spring 代码的"高压线"（agent 容易踩）

> 这些改完会引发一次完整重启，而且排错成本高 —— 下笔前先想清楚。

- **Nacos dataId**：Spring Cloud Alibaba 2023.0.1.0 不会自动拼 `file-extension`，`application.yml` 里的 `spring.config.import` 已经写完整了 dataId，**不要**简化。
- **数据库 schema**：dev / test / prod 均用 MySQL；新加字段务必在 `schema.sql` 同步；`schema.sql` 已统一为 MySQL 方言（反引号 / `ON UPDATE CURRENT_TIMESTAMP` 等），无需再评估 H2 兼容。
- **单元测试**：纯逻辑测试用 JUnit 5 写，不要加 `@SpringBootTest`；上下文冒烟测试在测试类自身 `properties` 中显式排除 `DataSourceAutoConfiguration` / `MybatisAutoConfiguration`、禁用 Nacos config / discovery / service-registry，并对 Mapper 用 `@MockBean` 占位；`./mvnw test` 必须能离线跑通，不连 MySQL/Nacos。
- **禁用 Nacos 的开关**：测试用 `spring.cloud.nacos.config.enabled: false`（已在 `application-test.yml` 写好），改测试时不要动 `application.yml` 的兜底 `import: optional:...`。
- **MyBatis XML 改 namespace**：Mapper 接口和 XML 的 `namespace` 必须一致；`map-underscore-to-camel-case` 已开，**不要**手写 `resultMap`。
- **JWT secret**：默认 `application.yml` 没有 `jwt.secret`（交给 Nacos）；测试有占位。**不要**在仓库里写真实 secret。
- **拦截器新增路径**：默认 `/api/**`；新增 `management/**` 之类时**显式**修改 `WebMvcConfig`，别只放行单点。
- **`@RefreshScope`**：动 `feishu.*` / `biz.*` 的字段绑定类必须保持 `@RefreshScope`，否则改了 Nacos 也不生效。
- **类头部 license**：复制 `Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.` — 改类同时改 license。
- **`@since` 标签**：顶层 public 类 Javadoc 必须有 `@since YYYY-MM-DD`。`checkstyle_huawei.xml` 已在规则里。

---

## 5. 调试 / 验证流程

- 接口验证（dev profile）：
  - Swagger UI: <http://localhost:8888/swagger-ui.html>
  - 登录：`POST /api/auth/login` body `{"username":"admin","password":"admin123"}`
  - 鉴权：所有 `/api/**` 除 `@SkipAuth` 外需 `Authorization: Bearer <token>`。
  - 健康：`curl http://localhost:8888/actuator/health`
  - 强制刷新：`curl -X POST http://localhost:8888/actuator/refresh`
- 飞书 webhook 出问题时：先看 `FeishuBotServiceImpl` 的日志（推送的 JSON、飞书返回码），加签开启时 `secret` 是否配置。

---

## 6. 写法上的偏好（按本仓库惯例）

- DTO 名字：`XxxDto` / `XxxRequest` / `XxxResponse`，字段 `camelCase`。
- Controller：**类级别 `@Tag` + 方法级别 `@Operation`**（OpenAPI 注解），不要漏。
- Service：`@Service` + 接口 + `XxxServiceImpl`；事务用 `@Transactional`。
- Mapper：方法名用动词（`findUserById` / `insertUser` / `deleteUserById`）。
- 异常：业务抛 `IllegalArgumentException` / `UnauthorizedException` / `ForbiddenException`，**不要**在 service 里 `try-catch` 后吃掉。
- 返回值：Controller 全部用 `ResponseResult<T>` 包装，**不要**直接 `return user`。
- Lombok：能 `@Data` 就 `@Data`；**不要**自己写 getter/setter。
- 日志：构造期记 `LOG = LoggerFactory.getLogger(...)`；**不要** `System.out.println`。
- ASCII / 中文：Chinese Javadoc 与默认 commit 文案都用中文，技术名词保持英文。

---

## 7. 写代码前的"必读一遍"

1. 这个改动会不会动 `/api/**` 鉴权？ → 同步改 `WebMvcConfig`。
2. 这个改动会不会改 Nacos 配置？ → 改 `application.yml` 默认值，**且**告诉用户去 Nacos 同步改 prod。
3. 这个改动会不会改 DB schema？ → 同步改 `schema.sql`，并考虑 `data.sql` 需要回填。
4. 这个改动会不会重启才能生效？ → 能不能用 `@RefreshScope` 规避？
5. 这个改动会不会触发飞书真实推送？ → 默认 `feishu.webhook.enabled=true` 是有意为之，调试时主动 `false`。
6. 这个改动会不会改 docker 网络？ → 同步改 `docker-compose.nacos.yml` / `Jenkinsfile` / `scripts/`。

---

## 8. 完成的定义 (DoD)

> 一个改动在"完工"之前必须满足：

- [ ] `./mvnw -B test` 通过（至少相关测试类绿）。
- [ ] `./mvnw -B -DskipTests package` 通过，jar 仍是 `target/ProjectInfomationManage.jar`。
- [ ] 改的接口加了 Swagger 注解；改的 Service 抛业务异常而非 `RuntimeException`。
- [ ] 改的 `.java` / `.sql` 顶部 license 头正确。
- [ ] 改的代码没有新增 `System.out.println` / `TODO` / `FIXME`（除非你能给出原因）。
- [ ] 改的鉴权路径要么保留默认 JWT，要么显式 `@SkipAuth` 并写明动机。
- [ ] 没有把 secret / 真实 Nacos 凭据写进仓库。
- [ ] 改完告知用户在 Nacos / Jenkinsfile 哪些下游需要同步。

---

## 9. 失败模式（已知 & 应对）

| 现象 | 原因 | 应对 |
| --- | --- | --- |
| `mvn test` 卡住加载 context | Nacos 启用着 | 确认 `application-test.yml` 里 `nacos.config.enabled: false` |
| `401` 但 token 看似正确 | 过期（默认 24h） | 调 `/api/auth/login` 重新拿 |
| 改了 Nacos 不生效 | `RefreshScope` 没装配 / diff 为空 | 确认 `DynamicRefreshFallback` 在场；`POST /actuator/refresh` 手动触发 |
| Docker 镜像拉不下来 | 国内网 | 看 `docker-compose.nacos.yml` 是否用了 daocloud mirror |
| `190002 params error` (飞书) | 加签开启但 secret 未配 | 配 `feishu.webhook.secret`，或关闭加签 |
| 启动 `ClassNotFoundException: javax.crypto...` | 误用 JDK 8 编译 | 强制 `mvnw -B -Dmaven.compiler.source=17 ...` |

---

## 10. 与 agent 系统约定

- **不要**在 `AGENTS.md` / `CLAUDE.md` / `README.md` 里写真实 token / 凭据 / 手机号 / 邮箱。
- **不要**直接 `git add` / `git commit` / `git push` —— 让用户决定。
- **不要**改 `.claude/settings.local.json`（这是用户本机权限设置）。
- **不要**破坏 `mvnw`（已修改为 `0x` 状态可执行）；`git diff mvnw` 看起来异常是预期的。
- **不要**在 `target/` 下写代码（构建产物，`dockerignore` 已经忽略）。
- **不要**在 `pom.xml` 里加 CI 才需要的镜像源；用 `settings.xml`。
- **不要**绕过 `AuthInterceptor` 实现"管理员免登"——走 `RoleController` 配权限。

---

## 11. 已知差异 / 临时状态（写代码时要留意）

- `mvnw` 文件显示已修改（`M mvnw`），但当前是用的 `mvnw` 直接调用，不需要 commit 改它。
- `prompt-design-ui/` 内是前端代码，**不要**在 Java 主仓库里改它 —— 那是另一个子项目。
- `FeishuBotScheduledTask#pushHelloWorld` 的 `@Scheduled` 注释掉了。如果用户要求"把它打开"，**先确认** `feishu.webhook.enabled` 与 `feishu.webhook.url` 是不是测试值。
- `RestTemplateConfig` 现在用固定 5s 超时，不再依赖 Nacos；若后续要切回 `@RefreshScope` 请改用 `feishu.webhook.timeout-ms` 这类仍然存在的配置字段。
- 默认 `feishu.webhook.url` 写了 `a13b74c9-...`，看起来像是真实 webhook —— **不要**在调试时往真实群发消息；先在 `application-dev.yml` 覆盖成 `https://open.feishu.cn/.../hook/test` 或关闭 `enabled`。
