# AGENTS.md

> 给在本项目里工作的所有 AI 编码代理（Claude Code / Cursor / Junie / 其它 IDE agent）的一份统一指南。
> 只要你是 AI，且正在这个仓库里改代码，就请先读这份文档。

---

## 1. 项目速览

- **仓库根名**：`prompt-design-api`（Maven artifactId 是 `ProjectInfomationManage`，Spring 应用名也是 `ProjectInfomationManage`）
- **定位**：Spring Boot 后端 + Jenkins 容器化部署
- **包名根**：`com.yaowenltd.projectinfomationmanage`
- **监听端口**：`8888`
- **默认 profile**：`dev`（也可由 `SPRING_PROFILES_ACTIVE` 覆盖）
- **Java**：17（构建、运行时统一）
- **构建工具**：Maven（仓库已自带 `mvnw` / `mvnw.cmd` / `settings.xml`，国内走华为云镜像）

### 技术栈快照

| 领域 | 选型 |
| --- | --- |
| Web | Spring Boot 3.2.4 + Spring MVC |
| 配置来源 | 仓库内 `application-{profile}.yml` + 环境变量注入（**无外部配置中心**） |
| ORM | MyBatis 3.0.3（XML 映射在 `src/main/resources/mapper/`） |
| 数据库 | dev / test / prod: MySQL Connector/J（test 不连 MySQL，依赖 JUnit 5 + MockBean 上下文冒烟） |
| 鉴权 | JWT (`io.jsonwebtoken:jjwt 0.12.5`) + Spring Security Crypto BCrypt |
| 文档 | springdoc-openapi-starter-webmvc-ui 2.6.0（Swagger UI 在 `/swagger-ui.html`） |
| 运维 | spring-boot-starter-actuator（暴露 `health / info`） |
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
├── docker-compose.mysql.yml             # 本机 dev 用 MySQL（表结构由应用启动时自动初始化）
├── Jenkinsfile                          # Jenkins 流水线入口
├── checkstyle_huawei.xml                # 华为代码规约（当前未在构建阶段强制）
├── scripts/
│   ├── deploy-backend.sh                # 启/重启后端容器
│   └── pick-docker-mirror.sh            # 探测可用 docker 镜像源
├── src/main/java/com/yaowenltd/projectinfomationmanage/
│   ├── ProjectInformationManageApplication.java   # 主类（@EnableScheduling）
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
│   ├── application.yml          # 公共默认值（server / mybatis / springdoc / actuator / jwt 占位）
│   ├── application-dev.yml      # dev 覆盖（datasource / Flyway baseline / feishu / jwt）
│   ├── application-prod.yml     # prod 覆盖（datasource / Flyway 安全开关 / jwt / feishu，均走 ${ENV:default}）
│   ├── mapper/*.xml             # MyBatis 映射
│   ├── META-INF/                # （空，可放 spring.factories 等）
│   └── db/migration/V*__*.sql   # Flyway 版本化迁移
└── src/test/...                 # JUnit 5 + Spring Boot Test
```

---

## 3. 构建 / 运行 / 测试

> 国内构建请用 `mvnw`（自带 `settings.xml` 会切到华为云镜像），跑测试时尤其要确保网络可达。

```bash
# 编译（首次会拉依赖，慢）
./mvnw -B -DskipTests package

# 跑测试（不连 MySQL，纯 JUnit 5 单元测试 + MockBean 上下文冒烟；详见 application-test.yml）
./mvnw -B test

# 本机起 MySQL（一次性；dev profile 必需）
docker compose -f docker-compose.mysql.yml up -d
#   → 127.0.0.1:13306，库 project_info_manage，root / 123456
#   → 表结构与种子数据由应用启动时的 Flyway 迁移自动执行（classpath:db/migration/）

# 本机直接 Run（dev profile）
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run

# 直接打 Docker 镜像
docker build -f Dockerfile -t leiw-go/back:dev .
```

`./mvnw test` 与 `mvn test` 等价；但**不要**忽略 `settings.xml`——它把仓库切到华为云镜像。

---

## 4. 配置 / Profile 约定

- **没有配置中心**。所有配置都在仓库内的 `application.yml` / `application-{profile}.yml` 里。
- `application.yml` 是公共底座：server / spring 应用名 / profile / mybatis / springdoc / actuator 暴露面 / jwt 占位。
- `application-dev.yml` 是 dev 的**唯一**来源：本地 MySQL（`127.0.0.1:13306`，库 `project_info_manage`，`root/123456`，由 `docker-compose.mysql.yml` 提供）、Flyway baseline、feishu、jwt。
- `application-prod.yml` 是 prod 的**唯一**来源：datasource / Flyway 安全开关 / jwt / feishu。
- 敏感值一律用 `${ENV:default}` 占位，由 Jenkins 参数或 Docker 环境变量注入；**不要**把真实凭据写进仓库。
  - `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` / `JWT_SECRET` / `JWT_EXPIRATION`
  - `FEISHU_WEBHOOK_URL` / `FEISHU_WEBHOOK_ENABLED` / `FEISHU_WEBHOOK_TIMEOUT_MS` / `FEISHU_WEBHOOK_SECRET`
  - `FLYWAY_ENABLED` / `FLYWAY_BASELINE_ON_MIGRATE`
- **改配置需要重启应用**（没有 `/actuator/refresh` 热刷新端点）。

### 数据库迁移（Flyway）

- 迁移文件在 `src/main/resources/db/migration/`，命名 `V<版本号>__<描述>.sql`。
- 当前只有一个迁移：

  | 版本 | 文件 | 作用 |
  | --- | --- | --- |
  | V1 | `V1__init_project_info_manage.sql` | 建 7 张表（**无物理外键**、utf8mb4_unicode_ci、`update_time` 自动更新、索引齐全）+ 种子数据（12 权限 / 2 角色 / admin / testuser） |

- 后续改表结构一律**新增** `V2__xxx.sql`、`V3__xxx.sql`...，**不要回头改 V1**（已执行过的迁移校验和不匹配会导致启动失败）。
- `spring.flyway.baseline-version: "1"`：**已有非空库**首次接入时登记为"已到 V1"，跳过 V1；**空库**正常执行 V1。
- `spring.flyway.baseline-on-migrate`：dev 默认 `true`；**prod 默认 `false`** —— 首次接管存量生产库必须显式传 `FLYWAY_BASELINE_ON_MIGRATE=true`（Jenkins 参数或容器 env），否则会因"非空 schema 且未 baseline"直接启动失败。这是刻意的保护，避免连错库时静默 baseline。
- `spring.flyway.clean-disabled: true` —— 禁止 `flyway clean`。
- 迁移里不要用 `INSERT IGNORE` 掩盖错误；迁移只执行一次，出错应直接失败。
- ⚠️ 删改 `src/main/resources/db/` 下的文件后要 **`./mvnw clean package`** —— 旧的 `.sql` 会残留在 `target/classes` 里被打进 jar，导致 Flyway 报 "Found more than one migration with version N"。

### 几个不易察觉的配置陷阱

- `jwt.secret` 在 base `application.yml` 里默认是空串。生产必须注入 `JWT_SECRET`，否则登录签发的 token 无法被后续请求校验。
- `feishu.webhook.secret` 留空表示未加签；如果生产开了加签却留空会得到 `190002 params error`。
- profile 文件里的同名键会覆盖 base `application.yml` 的值 —— base 里的 `jwt` 只是兜底默认值。
- **迁移失败后 Flyway 会拒绝再次启动**。MySQL 的 DDL 不在事务里，失败的迁移会留下 `success=0` 的记录；修好 SQL 后需要清掉那条失败记录（`DELETE FROM flyway_schema_history WHERE version='<N>' AND success=0`）或执行 `flyway repair`，才能重跑。

---

## 5. 鉴权 / 接口约定

- 拦截器：`AuthInterceptor` 注册在 `/api/**`，所有 `/api/...` 默认需要 JWT。
- ⚠️ **拦截路径不写 context-path**：`server.servlet.context-path` 是 `/design`，该前缀在进入 DispatcherServlet **之前**就被剥离，所以拦截器匹配的是 `/api/**` 而不是 `/design/api/**`。写成后者会**静默失效**（拦截器一次都不触发，所有 `/api/**` 端点变成免鉴权）—— 2026-11-06 修过一次，勿改回。
- 例外：在 controller **方法或类**上标 `@SkipAuth` 即可（不是 `@RequestMapping` 级）。当前放行的有：
  - `AuthController#register`
  - `AuthController#login`
- JWT 头部：`Authorization: Bearer <token>`，没有 / 过期 / 非法 → `401 UnauthorizedException` → 经 `GlobalExceptionHandler` 返回 `code=401`。
- 默认无权限注解 `@PreAuthorize` 之类；如果未来加 RBAC，请只变更 `AuthInterceptor` + `RoleController` 这一层，**不要**在每个 controller 上重复判断。
- 默认账号在 `src/main/resources/db/migration/V1__init_project_info_manage.sql`：
  - `admin / admin123`（ADMIN）
  - `testuser / test123456`（USER）

---

## 6. 数据库 / Mapper

- 表结构与演示数据由 `src/main/resources/db/migration/V*__*.sql` 管理；**已执行的 migration 禁止修改**（Flyway 校验和不匹配会导致启动失败）。
- 新增表 / 字段 / 索引 / 种子数据 → **新增递增版本的迁移**（例如 `V2__add_xxx.sql`），不要改旧版本，也不要回头改 `V1`。
- 迁移命名约定：`V<版本号>__<下划线小写描述>.sql`，版本号连续递增，不复用已删除的版本号。
- 主键策略：`UUID`（`String` 类型字段，与 MySQL `VARCHAR(36)` 配对）。
- 字段命名：`snake_case`；MyBatis 开启 `map-underscore-to-camel-case`，**不要再写 `resultMap`**。
- 软删除约定：表中用 `status TINYINT`（1 启用 / 0 停用），不要直接 DELETE。
- 迁移里**不要**用 `INSERT IGNORE` 掩盖错误 —— 迁移只执行一次，出错应该直接失败。
- `t_lottery_period` 字段重复（`front_1` … `front_5` / `back_1` / `back_2`），是历史遗留，**新增分析逻辑请走 Service 而不是再加列**。

### 外键约定：不使用物理外键，统一逻辑 mapping

**表结构中不允许出现 `FOREIGN KEY` 约束。** 关联关系统一用「逻辑外键 / 逻辑 mapping」维护：

| 层面 | 职责 |
| --- | --- |
| 关联查询 | 应用层用 MyBatis `JOIN` 完成（见 `PermissionMapper#findPermissionsByRoleId` 等） |
| 引用完整性 | Service 层保证 —— 写操作时先校验引用是否存在 |
| 级联清理 | Service 层显式删除关联行，不依赖数据库的 `ON DELETE CASCADE` |
| 性能 | 关联列**必须**显式建索引（`KEY user_id` / `role_id` / `permission_id`）—— 没有外键就不会再由 InnoDB 自动建 |

具体要求：

- 新增表 / 改表结构时**不要**加 `FOREIGN KEY`；`V1__init_project_info_manage.sql` 里从一开始就没建外键。
- **删除顺序由 Service 负责**，参考现有实现：
  - `UserServiceImpl#deleteUser` → 先 `userRoleMapper.deleteUserRoleByUserId(id)`，再 `userMapper.deleteUserById(id)`。
  - `RoleServiceImpl#deleteRole` → 先 `userRoleMapper.deleteUserRoleByRoleId(id)` + `rolePermissionMapper.deleteRolePermissionByRoleId(id)`，再 `roleMapper.deleteRoleById(id)`。
  - 新增任何删除路径时**必须**照此先清关联表，否则会留下孤儿行。
- 关联写入时同样要在 Service 层校验被引用记录存在（例如 `RoleServiceImpl#updateRole` 写 `t_role_permission` 前先确认权限 id 有效）。
- 之所以不用物理外键：它只带来 RESTRICT 语义（本该成功的删除被挡住）与级联误删风险，收益（引用完整性）用应用层实现成本更低且更可控。

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
- 关闭方式：把 `feishu.webhook.enabled` 改 `false`（即环境变量 `FEISHU_WEBHOOK_ENABLED=false`）后重启应用生效。
- 加签：开启加签时 `feishu.webhook.secret` 必须配上 `sec-...` 串；当前实现**尚未**实现加签算法（只有 secret 字段占位）。

---

## 9. 改动指南（每类任务的红线）

### 9.1 新增 HTTP 接口

1. `model/dto/XxxDto.java` + `model/entity/Xxx.java`（如有新表）。
2. `mapper/XxxMapper.java` + `src/main/resources/mapper/XxxMapper.xml`。
3. `service/XxxService.java` + `service/impl/XxxServiceImpl.java`。
4. `controller/XxxController.java`，在类上 `RequestMapping("/api/xxx")`；**默认要 JWT**，测试需要时再 `@SkipAuth`。
5. 在 Swagger 上加 `@Tag` / `@Operation`，不要复制粘贴无注解的接口。
6. 写 `src/main/resources/db/migration/V<N>__*.sql` 做迁移；已执行版本禁止修改。

### 9.2 改配置

- 改 `application.yml` / `*-dev.yml` / `*-prod.yml` = 改默认值（结构在仓库里，值用 `${ENV:default}` 占位）。
- 生产实际生效的值来自 Jenkins 参数 / Docker 环境变量，改完需要**重启容器**才生效。
- **不要**新增 `@RefreshScope` —— 项目的 `spring-cloud-context` 依赖已随 Nacos 一并移除，该注解不存在。

### 9.3 改部署

- 改 `Dockerfile`：注意 `target/ProjectInfomationManage.jar` 是 fat-jar 的预期文件名，**不要**改 finalName。
- 改 `Jenkinsfile` / `scripts/*.sh`：保持 `set -eu` + 显式 `trap rm -f`，别让临时文件留在 Jenkins agent。
- `scripts/deploy-backend.sh` 的位置参数顺序：`<name> <port> <profile> <network> <image> [db_url] [db_user] [db_pass] [jwt_secret] [jwt_expiration]` —— 改签名时要同步改 `Jenkinsfile` 里的调用。
- 飞书 webhook 的生产配置（原先在 Nacos）现在通过宿主机环境变量 `FEISHU_WEBHOOK_URL` / `FEISHU_WEBHOOK_ENABLED` / `FEISHU_WEBHOOK_TIMEOUT_MS` / `FEISHU_WEBHOOK_SECRET` 透传进容器；未设置则回落到 `application-{profile}.yml` 的默认值。

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

- 当前 `src/test` 下只有 `BCryptPasswordEncoderTests`（纯单元测试）。
- **纯单元测试**只使用 JUnit 5 (`org.junit.jupiter.api.Test`)，不依赖 Spring 上下文、不连接 MySQL/任何外部基础设施，`./mvnw test` 离线即可跑通。
- **上下文冒烟测试**（如需新增）使用 `@SpringBootTest`，但要在测试类自身通过 `spring.autoconfigure.exclude` 排除 `DataSourceAutoConfiguration` / `MybatisAutoConfiguration`，并对全部 Mapper（`UserMapper` / `RoleMapper` / `UserRoleMapper` / `PermissionMapper` / `RolePermissionMapper` / `ProductMapper` / `LotteryPeriodMapper`）使用 `@MockBean` 占位；不需要外部 MySQL 即可完成 context 加载冒烟。
- 测试 profile：`src/test/resources/application-test.yml` 关闭 SQL 初始化（`spring.sql.init.mode: never`）与 Flyway（`spring.flyway.enabled: false` —— 只要有 DataSource bean 它就会自动装配并尝试迁移），仅保留 `jwt.secret` / `jwt.expiration` 占位（`JwtUtil` `@Value` 必填）。项目已无 Nacos，无需再关 config / discovery。
- 新增测试类放 `src/test/java/.../<被测类名>Tests.java`；纯逻辑测试默认按 JUnit 5 写，不引入 `@SpringBootTest`。
- **不要**把外部依赖（真实 MySQL）耦合进单元测试；要写集成测试请用 Testcontainers + Failsafe 插件（`*IT.java`），与单元测试目录/阶段分离。

---

## 12. 提交 / 协作

- 分支：默认 `master`；功能分支按 `feature/xxx` 或 `fix/xxx` 走。
- 提交信息走约定式（看 `git log` 历史：`add ...` / `fix(scope): ...`）。
- 一次提交只做一件事，至少包含：源码 + 相关测试 + 对应的 Flyway migration（如涉及数据库变更）。
- 改公共 Bean / 拦截器 / 拦截路径：`/api/**` 时一定更新 `WebMvcConfig`。
- 改 `pom.xml`：升级 Spring Boot 时**必须**同时核对 `application.yml` 的配置项迁移。

---

## 13. AI 代理动手前的硬性 checklist

> 写代码前先对一遍：

- [ ] 跑过 `./mvnw -B -DskipTests package` 至少一次，确认基线干净。
- [ ] 跑过 `./mvnw -B test` 至少一次。
- [ ] 涉及配置：先看 `application.yml` 默认值与 `application-{profile}.yml` 的占位符，再决定改仓库还是改环境变量。
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
