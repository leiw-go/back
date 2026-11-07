# ProjectInfomationManage

> Spring Boot 后端服务 —— 用户 / 角色 / 权限 / 商品 / 彩票期数管理，带 JWT 鉴权与飞书机器人推送。
>
> 端口 **8888**，全局 context-path **`/design`**。

---

## 目录

- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [配置说明](#配置说明)
- [API 一览](#api-一览)
- [鉴权](#鉴权)
- [构建与测试](#构建与测试)
- [部署](#部署)
- [项目结构](#项目结构)
- [数据模型](#数据模型)
- [开发约定](#开发约定)

---

## 技术栈

| 领域 | 选型 |
| --- | --- |
| Web | Spring Boot 3.2.4 + Spring MVC |
| 配置来源 | 仓库内 `application-{profile}.yml` + 环境变量注入（**无外部配置中心**） |
| ORM | MyBatis 3.0.3（XML 映射在 `src/main/resources/mapper/`） |
| 数据库 | MySQL 8（Connector/J） |
| 鉴权 | JWT (`io.jsonwebtoken:jjwt 0.12.5`) + Spring Security Crypto BCrypt |
| 文档 | springdoc-openapi-starter-webmvc-ui 2.6.0 |
| 运维 | spring-boot-starter-actuator（暴露 `health` / `info`） |
| 外部 HTTP | `RestTemplate`（固定 5s 超时） |
| 构建 | Maven（仓库自带 `mvnw`，国内走华为云镜像） |
| Java | 17 |

> 项目已移除 Nacos / Spring Cloud Alibaba：无配置中心、无服务注册、无 `@RefreshScope` 热刷新。
> **改配置需要重启应用或容器。**

---

## 快速开始

### 1. 准备 MySQL

dev profile 用宿主机 `127.0.0.1:13306`，库 `project_info_manage`，账号 `root/123456`：

```bash
docker compose -f docker-compose.mysql.yml up -d
```

表结构与种子数据**不需要手动导入** —— 应用启动时会自动执行 Flyway 迁移
（`src/main/resources/db/migration/` 下的 `V*__*.sql`）：

- **空库**：正常执行 `V1`（建表 + 种子数据）
- **已有库**：baseline 登记为 V1，跳过 V1

停止：`docker compose -f docker-compose.mysql.yml down`

### 2. 启动应用

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

启动完成后：

| 地址 | 说明 |
| --- | --- |
| <http://localhost:8888/design/swagger-ui.html> | Swagger UI（接口文档 / 在线调试） |
| <http://localhost:8888/design/api-docs> | OpenAPI JSON |
| <http://localhost:8888/design/actuator/health> | 健康检查 |

健康检查：

```bash
curl -s http://localhost:8888/design/actuator/health
# {"status":"UP"}
```

### 3. 登录拿 token

```bash
curl -s -X POST http://localhost:8888/design/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}'
```

拿返回里的 `data.token`，后续请求带上：

```bash
curl -s -H "Authorization: Bearer <token>" http://localhost:8888/design/api/users
```

---

## 配置说明

配置**全部来自仓库内的 YAML**，敏感值用 `${ENV:default}` 占位，由 Jenkins 参数或 Docker 环境变量注入。

| 文件 | 作用 |
| --- | --- |
| `src/main/resources/application.yml` | 公共底座：server / 应用名 / profile / mybatis / springdoc / actuator 暴露面 / jwt 默认值 |
| `src/main/resources/application-dev.yml` | dev 覆盖：本地 MySQL、Flyway baseline、feishu、jwt |
| `src/main/resources/application-prod.yml` | prod 覆盖：datasource、Flyway 安全开关、jwt、feishu（均走 `${ENV:default}`） |
| `src/test/resources/application-test.yml` | 测试 profile：关闭 SQL 初始化与 Flyway，仅保留 jwt 占位 |

### 可注入的环境变量

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` | 激活的 profile |
| `DB_URL` | `jdbc:mysql://mysql:3306/project_info_manage?...` | 生产 JDBC URL |
| `DB_USERNAME` / `DB_PASSWORD` | `root` / `Root@123456` | 数据库凭据 |
| `JWT_SECRET` | 空 | **生产必填**，Base64 密钥 |
| `JWT_EXPIRATION` | `86400000` | token 有效期（毫秒，默认 24h） |
| `FEISHU_WEBHOOK_URL` | 空 | 飞书机器人 webhook 地址 |
| `FEISHU_WEBHOOK_ENABLED` | `false`（prod） | 是否启用飞书推送 |
| `FEISHU_WEBHOOK_TIMEOUT_MS` | `3000` | webhook 超时 |
| `FEISHU_WEBHOOK_SECRET` | 空 | 飞书加签密钥（`sec-...`） |
| `FLYWAY_ENABLED` | `true` | 是否启用 Flyway 迁移（一般不要关） |
| `FLYWAY_BASELINE_ON_MIGRATE` | dev `true` / prod `false` | prod 首次接管存量库时需显式置 `true` |

> 生产环境的 `JWT_SECRET` 默认是空串，**必须**由部署平台注入，否则签发的 token 无法通过校验。

---

## API 一览

所有路径以 `/design` 开头。控制器分 5 个：

| 前缀 | 控制器 | 说明 |
| --- | --- | --- |
| `/api/auth` | `AuthController` | 注册 / 登录 / 当前用户 |
| `/api/users` | `UserController` | 用户 CRUD |
| `/api/roles` | `RoleController` | 角色 CRUD + 角色权限 |
| `/api/products` | `ProductController` | 商品 CRUD |
| `/api/lottery` | `LotteryPeriodController` | 彩票期数 CRUD + 号码统计 |

<details>
<summary>展开完整接口列表</summary>

### `/api/auth`

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | 免鉴权 | 注册 |
| POST | `/api/auth/login` | 免鉴权 | 登录，返回 JWT |
| GET | `/api/auth/currentUser` | 需鉴权 | 当前登录用户 + 角色编码 |

### `/api/users`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/users` | 新增用户 |
| PUT | `/api/users/{id}` | 更新用户 |
| DELETE | `/api/users/{id}` | 删除用户（物理删除） |
| GET | `/api/users/{id}` | 用户详情 |
| GET | `/api/users` | 用户列表 |

### `/api/roles`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/roles` | 新增角色 |
| PUT | `/api/roles/{id}` | 更新角色 |
| DELETE | `/api/roles/{id}` | 删除角色 |
| GET | `/api/roles/{id}` | 角色详情 |
| GET | `/api/roles` | 角色列表 |
| GET | `/api/roles/permissions` | 全部权限列表 |

### `/api/products`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/products` | 新增商品 |
| PUT | `/api/products/{id}` | 更新商品 |
| DELETE | `/api/products/{id}` | 删除商品 |
| GET | `/api/products/{id}` | 商品详情 |
| GET | `/api/products` | 商品列表 |

### `/api/lottery`

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/lottery/periods` | 新增开奖记录 |
| PUT | `/api/lottery/periods/{id}` | 更新开奖记录 |
| DELETE | `/api/lottery/periods/{id}` | 删除开奖记录 |
| GET | `/api/lottery/periods/{id}` | 开奖记录详情 |
| GET | `/api/lottery/periods?page=1&size=10` | 分页查询（按开奖日期倒序） |
| GET | `/api/lottery/statistics/single?startDate=yyyy-MM-dd&endDate=yyyy-MM-dd` | 单时间段号码冷热统计（起止日期必填） |
| POST | `/api/lottery/statistics/multiple` | 多时间段号码统计（见下方 body，区间不可重叠） |

</details>

`/api/lottery/statistics/multiple` 请求体示例：

```json
{
  "ranges": [
    { "label": "2025上半年", "startDate": "2025-01-01", "endDate": "2025-06-30" },
    { "label": "2025下半年", "startDate": "2025-07-01", "endDate": "2025-12-31" }
  ]
}
```

### 统一响应体

所有 Controller 返回 `ResponseResult<T>`：

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "errors": null
}
```

`code` 才是业务状态码（HTTP 状态码恒为 200）：`200` 成功 / `400` 参数错误 / `401` 未认证 / `403` 无权限 / `500` 未预期异常。

---

## 鉴权

- 拦截器 `AuthInterceptor` 注册在 **`/api/**`**（注意：**不要**把 context-path `/design` 写进去 —— 它在进入 DispatcherServlet 前会被剥离，写成 `/design/api/**` 会让拦截器静默失效）。
- 免鉴权接口：在 controller **方法或类**上标 `@SkipAuth`（当前是 `AuthController#register` 与 `#login`）。
- 请求头：`Authorization: Bearer <token>`；缺失 / 过期 / 非法 → `code=401`。
- 默认种子账号（由 `db/migration/V1__init_project_info_manage.sql` 写入）：

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `admin123` | ADMIN |
| `testuser` | `test123456` | USER |

---

## 构建与测试

```bash
# 编译打包 → target/ProjectInfomationManage.jar
./mvnw -B -DskipTests package

# 跑测试（纯 JUnit 5，不连 MySQL，可离线跑通）
./mvnw -B test

# 跑单个测试类
./mvnw -B test -Dtest=BCryptPasswordEncoderTests
```

---

## 部署

### Docker 镜像

```bash
docker build -f Dockerfile -t leiw-go/back:dev .
```

### Jenkins

`Jenkinsfile` 走两个 stage：构建镜像 → 部署容器。参数：

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `BACKEND_TAG` | `latest` | 镜像 tag |
| `PROFILE` | `prod` | Spring profile |
| `BACKEND_PORT` | `8888` | 宿主机端口 |
| `BACKEND_NAME` | `backend` | 容器名 |
| `NETWORK_NAME` | `deploy-net` | docker 网络（不存在会自动创建） |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | 见上表 | 数据库连接 |
| `JWT_SECRET` / `JWT_EXPIRATION` | 见上表 | JWT 配置 |
| `FLYWAY_BASELINE_ON_MIGRATE` | `false` | Flyway 首次接管存量库时开启 baseline（prod 存量库首次上线需置 `true`） |

部署脚本也可以单独跑（参数顺序固定）：

```bash
bash scripts/deploy-backend.sh <name> <port> <profile> <network> <image> \
  [db_url] [db_user] [db_pass] [jwt_secret] [jwt_expiration]
```

脚本会把 DB / JWT 以及宿主机上的 `FEISHU_WEBHOOK_*` 透传进容器环境变量；未设置的变量不写入，回落到 profile 自身的默认值。

---

## 项目结构

```
.
├── pom.xml                              # Maven 主 POM
├── settings.xml                         # Maven 镜像（华为云）
├── Dockerfile                           # 后端多阶段镜像
├── docker-compose.mysql.yml             # 本机 dev 用 MySQL
├── Jenkinsfile                          # Jenkins 流水线
├── checkstyle_huawei.xml                # 华为代码规约
├── scripts/
│   ├── deploy-backend.sh                # 启/重启后端容器
│   └── pick-docker-mirror.sh            # 探测可用 docker 镜像源
├── src/main/java/com/yaowenltd/projectinfomationmanage/
│   ├── ProjectInformationManageApplication.java
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
│   ├── application.yml / -dev.yml / -prod.yml
│   ├── mapper/*.xml             # MyBatis 映射
│   └── db/migration/V*__*.sql   # Flyway 版本化迁移
└── src/test/...                 # JUnit 5 + Spring Boot Test
```

---

## 数据模型

| 表 | 说明 |
| --- | --- |
| `t_user` | 用户 |
| `t_role` | 角色 |
| `t_permission` | 权限 |
| `t_user_role` | 用户-角色关联 |
| `t_role_permission` | 角色-权限关联 |
| `t_product` | 商品 |
| `t_lottery_period` | 彩票期数（`front_1`…`front_5` / `back_1` / `back_2`） |

约定：

- 主键 `VARCHAR(36)`，由应用生成 UUID。
- 字段 `snake_case`；MyBatis 开了 `map-underscore-to-camel-case`。
- `t_user` / `t_role` / `t_product` 上有 `status` 字段（`1` 启用 / `0` 停用）。**当前实现里的 DELETE 是物理删除**（`DELETE FROM ... WHERE id = ?`），与 `AGENTS.md` 里"优先软删除"的约定不一致 —— 新增/改造删除逻辑时请按软删除来做。
- **不使用物理外键**：表结构里没有 `FOREIGN KEY` 约束，关联关系统一用「逻辑外键」维护 —— 查询走 MyBatis `JOIN`，引用完整性和级联清理由 Service 层负责（删除用户/角色时先清关联表）。关联列上仍显式建了索引供 JOIN 使用。
- **改表结构走 Flyway**：新增 `src/main/resources/db/migration/V<N>__<描述>.sql`，版本号连续递增。已执行过的迁移文件**不可修改**（校验和不匹配会导致启动失败），要改就新增一个版本。
- 迁移里不要用 `INSERT IGNORE` —— 迁移只执行一次，出错应该直接失败。

### 迁移与 baseline

当前只有一个迁移 `V1__init_project_info_manage.sql`：建 7 张表 + 种子数据，
表结构**从一开始就没有物理外键**，字符集统一 utf8mb4_unicode_ci，`update_time` 由 MySQL 自动维护。

`spring.flyway.baseline-version: "1"` 决定了存量库怎么接入：

| 库状态 | 行为 |
| --- | --- |
| 空库 | 正常执行 V1 |
| 已有非空库 | baseline 登记为 V1 → 跳过 V1（视为"结构已经是 V1 的样子"） |

后续改表结构一律新增 `V2__xxx.sql`，**不要回头改 V1**（已执行过的迁移校验和不匹配会导致启动失败）。

prod 的 `baseline-on-migrate` 默认为 `false`（避免连错库时静默 baseline），**首次接管存量生产库需显式传 `FLYWAY_BASELINE_ON_MIGRATE=true`**，否则启动会因"非空 schema 且未 baseline"直接失败。

> 删改 `db/migration/` 下的文件后记得 `./mvnw clean package` —— 旧的 `.sql` 会残留在 `target/classes` 被打进 jar，Flyway 会报 "Found more than one migration with version N"。

---

## 开发约定

更详细的规范见 [`AGENTS.md`](./AGENTS.md)（通用）和 [`CLAUDE.md`](./CLAUDE.md)（Claude Code 专用），要点：

- 类头 license：`Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.`
- 顶层 public 类的 Javadoc 必须带 `@since YYYY-MM-DD`。
- 业务异常抛 `IllegalArgumentException` / `UnauthorizedException` / `ForbiddenException`，不要在 service 里 try-catch 吞掉。
- Controller 统一返回 `ResponseResult<T>`，类上加 `@Tag`、方法上加 `@Operation`。
- 日志用 SLF4J，不要 `System.out.println`。
- 注释与 commit 文案用中文，技术名词保留英文。
- **不要**在仓库里写真实凭据（数据库口令、JWT 密钥、飞书 webhook）。
