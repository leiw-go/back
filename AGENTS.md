# AGENTS.md

> 给在本项目里工作的所有 AI 编码代理（Claude Code / Cursor / Junie / 其它 IDE agent）的一份统一指南。
> 只要你是 AI，且正在这个仓库里改代码，就请先读这份文档。

---

## 1. 项目速览

- **仓库根名**：`prompt-design-api`（Maven artifactId 是 `ProjectInfomationManage`，Spring 应用名也是 `ProjectInfomationManage`）
- **定位**：Spring Boot 后端 + Nacos 配置中心 + Jenkins 容器化部署
- **包名根**：`com.yaowenltd.projectinfomationmanage`
- **监听端口**：`8888`
- **默认 profile**：`dev`（也可由 `SPRING_PROFILES_ACTIVE` 覆盖）
- **Java**：17（构建、运行时统一）
- **构建工具**：Maven（仓库已自带 `mvnw` / `mvnw.cmd` / `settings.xml`，国内走华为云镜像）

### 技术栈快照

| 领域 | 选型 |
| --- | --- |
| Web | Spring Boot 3.2.4 + Spring MVC |
| 微服务 | Spring Cloud Alibaba 2023.0.1.0（已自带 BOM） |
| 配置/注册中心 | Nacos（config + discovery） |
| ORM | MyBatis 3.0.3（XML 映射在 `src/main/resources/mapper/`） |
| 数据库 | dev: H2（嵌入式）  / prod: MySQL Connector/J |
| 鉴权 | JWT (`io.jsonwebtoken:jjwt 0.12.5`) + Spring Security Crypto BCrypt |
| 文档 | springdoc-openapi-starter-webmvc-ui 2.6.0（Swagger UI 在 `/swagger-ui.html`） |
| 运维 | spring-boot-starter-actuator（暴露 `refresh / health / info`） |
| 外部 HTTP | `RestTemplate`（固定默认超时 5s） |
| 对象映射 | Lombok（仅编译期，必须装 Lombok 插件） |
| 终端/Java 版本 | Eclipse Temurin 17（JDK 17 / JRE 17-jre-alpine） |

---

## 2. 目录结构（只标重要的）

```
.
├── pom.xml                              # Maven 主 POM
├── settings.xml                         # Maven 镜像（华为云，重要！）
├── Dockerfile                           # 后端多阶段镜像
├── docker-compose.nacos.yml             # 本机 dev 用 Nacos
├── Jenkinsfile                          # Jenkins 流水线入口
├── checkstyle_huawei.xml                # 华为代码规约（当前未在构建阶段强制）
├── scripts/
│   ├── check-nacos.sh                   # 部署前探活 Nacos
│   ├── publish-config.sh                # 把本地 YAML 推送到 Nacos
│   └── deploy-backend.sh                # 启/重启后端容器
├── src/main/java/com/yaowenltd/projectinfomationmanage/
│   ├── ProjectInformationManageApplication.java   # 主类（@EnableDiscoveryClient + @EnableScheduling）
│   ├── common/      # ResponseResult / GlobalExceptionHandler / JwtUtil / 异常
│   ├── config/      # AuthInterceptor / WebMvcConfig / CorsConfig / SwaggerConfig / *Properties
│   ├── controller/  # Auth / User / Role / Product / LotteryPeriod
│   ├── mapper/      # MyBatis Mapper 接口
│   ├── model/dto/   # 请求 / 响应 DTO
│   ├── model/entity # 数据库实体
│   ├── service/     # 接口
│   ├── service/impl # Spring 实现
│   └── task/        # @Scheduled 任务（飞书机器人）
├── src/main/resources/
│   ├── application.yml          # 公共 Nacos 配置 + 默认值
│   ├── application-dev.yml      # dev 覆盖（目前刻意留空）
│   ├── application-prod.yml     # prod 覆盖（host.docker.internal + nacos/nacos）
│   ├── mapper/*.xml             # MyBatis 映射
│   ├── META-INF/                # （空，可放 spring.factories 等）
│   └── sql/schema.sql + data.sql + data-user.sql
└── src/test/...                 # JUnit 5 + Spring Boot Test
```

---

## 3. 构建 / 运行 / 测试

> 国内构建请用 `mvnw`（自带 `settings.xml` 会切到华为云镜像），跑测试时尤其要确保网络可达。

```bash
# 编译（首次会拉依赖，慢）
./mvnw -B -DskipTests package

# 跑测试（依赖 H2 内存库，无需 Nacos；详见 application-test.yml）
./mvnw -B test

# 本机直接 Run（dev profile）
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

# 本机起 Nacos（一次性）
docker compose -f docker-compose.nacos.yml up -d

# 直接打 Docker 镜像
docker build -f Dockerfile -t leiw-go/back:dev .
```

`./mvnw test` 与 `mvn test` 等价；但**不要**忽略 `settings.xml`——它把仓库切到华为云镜像。

---

## 4. 配置 / Profile 约定

- `application.yml` 是公共底座，提供 Nacos 连接、actuator 暴露、feishu 集成所需的字段。
- `application-dev.yml` **几乎是空文件**（故意留空），用 `docker-compose.nacos.yml` 提供本地 Nacos。
- `application-prod.yml` 仅覆盖 Nacos 的 `server-addr` / `username` / `password`，靠 `host.docker.internal:8848`。
- 敏感字段（生产 `datasource` / `jwt` / sql.init）放在 Nacos 的 `ProjectInfomationManage-prod.yaml`，**不要**塞进仓库。
- dataId 命名约定：`ProjectInfomationManage-${spring.profiles.active}.yaml`。
- Spring Cloud Alibaba 2023.0.1.0 在 `spring.config.import` 中必须**显式**写出后缀，所以 `application.yml` 里那里要写完整 dataId。
- 切换远程 Nacos：`export NACOS_SERVER_ADDR=... NACOS_USERNAME=... NACOS_PASSWORD=...` 即可。

### 几个不易察觉的配置陷阱

- `spring.cloud.nacos.config.fail-fast: false` + `register-enabled: false`：本机没 Nacos 时也能起服务，只是配置走默认。
- `management.endpoints.web.exposure.include: refresh,health,info`：`refresh` 暴露出来是为了给运维在不发重启的前提下强制刷新一次 Nacos 配置。
- `feishu.webhook.secret` 留空表示未加签；如果生产开了加签却留空会得到 `190002 params error`。

---

## 5. 鉴权 / 接口约定

- 拦截器：`AuthInterceptor` 注册在 `/api/**`，所有 `/api/...` 默认需要 JWT。
- 例外：在 controller **方法或类**上标 `@SkipAuth` 即可（不是 `@RequestMapping` 级）。当前放行的有：
  - `AuthController#register`
  - `AuthController#login`
- JWT 头部：`Authorization: Bearer <token>`，没有 / 过期 / 非法 → `401 UnauthorizedException` → 经 `GlobalExceptionHandler` 返回 `code=401`。
- 默认无权限注解 `@PreAuthorize` 之类；如果未来加 RBAC，请只变更 `AuthInterceptor` + `RoleController` 这一层，**不要**在每个 controller 上重复判断。
- 默认账号在 `src/main/resources/sql/data.sql` 与 `data-user.sql`：
  - `admin / admin123`（ADMIN）
  - `testuser / test123456`（USER）

---

## 6. 数据库 / Mapper

- 表结构与演示数据：`src/main/resources/sql/schema.sql` / `data.sql` / `data-user.sql`。
- 主键策略：`UUID`（`String` 类型字段，与 MySQL `VARCHAR(36)` 配对）。
- 字段命名：`snake_case`；MyBatis 开启 `map-underscore-to-camel-case`，**不要再写 `resultMap`**。
- 软删除约定：表中用 `status TINYINT`（1 启用 / 0 停用），不要直接 DELETE。
- 新增表 → 同步改 `schema.sql`；新增种子数据 → 同步改 `data.sql` 且使用 `INSERT IGNORE` / `INSERT IGNORE ... SELECT`。
- `t_lottery_period` 字段重复（`front_1` … `front_5` / `back_1` / `back_2`），是历史遗留，**新增分析逻辑请走 Service 而不是再加列**。

---

## 7. 错误处理 / 日志

- 统一异常：抛 `IllegalArgumentException`（→ 400）/ `UnauthorizedException`（→ 401）/ `ForbiddenException`（→ 403）。
- 非受检 / 系统异常 → `GlobalExceptionHandler` 兜底 → 500 + 中文文案。
- 校验失败 → 400，`errors` 字段会带每个字段的 message。
- 日志风格：SLF4J；服务/仓库层 `LOG.warn/error`，定时任务 `LOG.info`；**不要**用 `System.out.println`。
- 出站 HTTP 失败：捕获 `RestClientException` 后统一返回 `ResponseResult(code, "上游错误: ...", null)`，便于调用方区分业务异常与传输异常。

---

## 8. 飞书机器人 / 定时任务

- `FeishuBotScheduledTask#pushHelloWorld` 当前 `@Scheduled` 注释掉了——**改 cron 时记得把注释打开**。
- 关闭方式：把 `feishu.webhook.enabled` 改 `false`（Nacos 改完推一次，等几秒生效，无需重启）。
- 加签：开启加签时 `feishu.webhook.secret` 必须配上 `sec-...` 串；当前实现**尚未**实现加签算法（只有 secret 字段占位）。

---

## 9. 改动指南（每类任务的红线）

### 9.1 新增 HTTP 接口

1. `model/dto/XxxDto.java` + `model/entity/Xxx.java`（如有新表）。
2. `mapper/XxxMapper.java` + `src/main/resources/mapper/XxxMapper.xml`。
3. `service/XxxService.java` + `service/impl/XxxServiceImpl.java`。
4. `controller/XxxController.java`，在类上 `RequestMapping("/api/xxx")`；**默认要 JWT**，测试需要时再 `@SkipAuth`。
5. 在 Swagger 上加 `@Tag` / `@Operation`，不要复制粘贴无注解的接口。
6. 写 `schema.sql` + `data.sql` 做迁移。

### 9.2 改 Nacos 配置

- 改 `application.yml` / `*-dev.yml` / `*-prod.yml` = 改默认值。
- 改 Nacos 线上配置 = 改 `scripts/publish-config.sh` 引用的那个外部 YAML 仓库（**不在本仓库**）。
- 涉及 `feishu.*` / `biz.*` 的字段必须走 `@RefreshScope`，否则改完不生效。

### 9.3 改部署

- 改 `Dockerfile`：注意 `target/ProjectInfomationManage.jar` 是 fat-jar 的预期文件名，**不要**改 finalName。
- 改 `Jenkinsfile` / `scripts/*.sh`：保持 `set -eu` + 显式 `trap rm -f`，别让临时文件留在 Jenkins agent。
- 改 `docker-compose.nacos.yml`：`MODE: standalone` + 免 MySQL 是 dev 约定；生产请用 Nacos 官方 MySQL 模式。

---

## 10. 代码风格（必须遵守）

- 许可证头（每个 `.java` / 不少 `.sql` 第一行注释）：
  ```
  /*
   * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
   */
  ```
- 顶层 public 类的 Javadoc 必须包含 `@since YYYY-MM-DD`（格式严格，参考 `checkstyle_huawei.xml`）。
- 4 空格缩进，**禁止 Tab**。
- 单文件 ≤ 2000 行（保留 `checkstyle_huawei.xml` 的 `FileLength` 上限）。
- 注释 / Javadoc 默认中文；类、public 方法必须写 Javadoc，**字段级可省略**。
- 不要在 Service 里直接拼 SQL，全部走 Mapper。
- 命名：DB 字段 `snake_case`；Java 字段 `camelCase`；接口 `-able` / `Service`；实现 `XxxServiceImpl`。
- 工具类构造私有；枚举优先于魔法数字；常量写类顶部并 `static final`。
- 加 Lombok：实体、DTO 上用 `@Data` / `@Builder` 等即可，**不要**自己写 getter/setter。

---

## 11. 测试

- 测试 profile：`src/test/resources/application-test.yml` 会关 Nacos、用 H2 内存库，保证 `./mvnw test` 能离线跑。
- 新增测试类放 `src/test/java/.../<被测类名>Tests.java`。
- 上下文冒烟测试沿用 `ProjectInformationManageApplicationTests#contextLoads` 风格。
- 不要把外部依赖（真实 Nacos / MySQL）耦合进单元测试；要写就用 Testcontainers。

---

## 12. 提交 / 协作

- 分支：默认 `master`；功能分支按 `feature/xxx` 或 `fix/xxx` 走。
- 提交信息走约定式（看 `git log` 历史：`add ...` / `fix(scope): ...`）。
- 一次提交只做一件事，至少包含：源码 + 相关测试 + 相关 `schema.sql`/`data.sql`。
- 改公共 Bean / 拦截器 / 拦截路径：`/api/**` 时一定更新 `WebMvcConfig`。
- 改 `pom.xml`：升级 Spring Boot / Spring Cloud Alibaba 时**必须**同时核对 `bootstrap.yml` / `application.yml` 的配置项迁移。

---

## 13. AI 代理动手前的硬性 checklist

> 写代码前先对一遍：

- [ ] 跑过 `./mvnw -B -DskipTests package` 至少一次，确认基线干净。
- [ ] 跑过 `./mvnw -B test` 至少一次。
- [ ] 涉及 Nacos 配置：先看 `application.yml` 默认值，再决定走 Nacos 还是本地。
- [ ] 涉及鉴权：默认 `/api/**` 需要 JWT，需要时显式 `@SkipAuth`。
- [ ] 涉及 DB：先确认表字段是否已存在，**不要**想当然加列。
- [ ] 涉及飞书：先确认 `feishu.webhook.enabled` 默认值，**不要**误触发真实推送。
- [ ] 涉及 Dockerfile：跨阶段 COPY 路径不要硬编码绝对路径。
- [ ] 涉及 `pom.xml` / 版本：国内构建，依赖默认 `*` 镜像走 `repo.huaweicloud.com`。
- [ ] 不要提交：`.env` / `target/` / `.idea/` / `*.log` / 任何含真实凭据的 YAML。

---

## 14. 关于本仓库的内置代理知识

- `.claude/settings.local.json` 已存在：尊重其本地权限设置，**不要**主动覆盖。
- `.junie/` 目录存在：JetBrains Junie 的状态文件，**不要**写业务代码到这里。
- `.traces/` 不进 git，但 agent 别主动创建。
- `ProjectInfomationManage.iml` 是 IntelliJ 模块文件，**不要**人手改。

---

## 15. 后续值得做但本仓库还没做的（仅备忘，不要求一次都做）

- `pom.xml` 配 checkstyle / spotless 插件，把 `checkstyle_huawei.xml` 真用起来。
- `feishu.webhook.secret` 真正实现 HMAC-SHA256 加签。
- `Bcrypt` 编码器提到独立 `@Bean` 而不是每个 Service `new`。
- 用 Redis 做 token 黑名单 / 强制下线。
- 把 `RestTemplate` 换成 `WebClient`（响应式 + 不阻塞 Netty event loop）。
