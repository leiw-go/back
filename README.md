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
  - [部署形态](#部署形态) · [前置条件](#部署前置条件) · [首次部署](#首次部署完整流程)
  - [日常发布](#日常发布) · [回滚](#回滚) · [数据库迁移](#数据库迁移) · [数据库连接](#数据库连接)
  - [故障排查](#故障排查) · [本地手动构建镜像](#本地手动构建镜像) · [Jenkins（同机部署）](#jenkins同机部署)
- [生产环境现状](#生产环境现状4314314424313)
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

### 部署形态

**Docker 镜像 + docker compose**，编排只含后端服务；MySQL 是远程机上的独立实例，通过 `DB_URL` 连接。

```
deploy/
├── docker-compose.prod.yml    # 生产编排（只编排后端）
├── backend.env.example        # 环境变量模板（含凭据占位符）
└── backend.env                # 实际配置（gitignore，勿提交）
```

一次发布的数据流：

```
本地                          远程机 43.143.244.13
────                          ─────────────────────────
docker build ──┐
               ├─ docker save|gzip ─scp─→ docker load
docker compose.prod.yml ───────→ /home/ubuntu/jenkins-deploy/backend/
backend.env ──────────────────→ 同上（权限 600）
                                    ↓
                              docker compose up -d
                                    ↓
                        backend 容器（deploy-net, 8888:8888）
                                    ↓
                            nginx → 127.0.0.1:8888
```

### 部署前置条件

| 条件 | 说明 |
| --- | --- |
| 本机 | Docker 可用；ssh 公钥已加入远程机 `~/.ssh/authorized_keys` |
| 远程机 | 已装 Docker + Compose；已存在外部网络 `deploy-net`（MySQL 在上面） |
| 远程机 | `8888` 端口空闲（nginx upstream 写死了 `127.0.0.1:8888`） |
| 架构 | 目标架构与 `DEPLOY_PLATFORM` 一致（默认 `linux/amd64`） |

### 首次部署（完整流程）

#### 1. 本地准备

```bash
# 确认本机能连上远程机
ssh -i ~/.ssh/id_ed25519 ubuntu@43.143.244.13 'docker version --format "{{.Server.Version}}"'
```

#### 2. 远程机准备

**a) 建部署目录**

```bash
ssh -i ~/.ssh/id_ed25519 ubuntu@43.143.244.13 \
  'mkdir -p ~/jenkins-deploy/backend'
```

**b) 确认外部网络存在**（后端要用容器名 `mysql` 解析 MySQL，必须在同一网络）

```bash
ssh -i ~/.ssh/id_ed25519 ubuntu@43.143.244.13 'docker network inspect deploy-net'
# 网络不存在时：docker network create deploy-net
```

**c) 生成 `backend.env`（在远程机上做，凭据不落到开发机）**

```bash
ssh -i ~/.ssh/id_ed25519 ubuntu@43.143.244.13 'bash -s' <<'EOF'
set -eu
cd ~/jenkins-deploy
set -a; . ./.env; set +a          # MYSQL_ROOT_PASSWORD / MYSQL_DATABASE 在这里
JWT_SECRET=$(openssl rand -base64 32)
umask 077
cat > backend/backend.env <<ENVEOF
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:mysql://mysql:3306/project_info_manage?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf-8
DB_USERNAME=root
DB_PASSWORD=${MYSQL_ROOT_PASSWORD}
JWT_SECRET=${JWT_SECRET}
JWT_EXPIRATION=86400000
JAVA_TOOL_OPTIONS=-Xmx320m -Xms128m -XX:MaxMetaspaceSize=128m
FEISHU_WEBHOOK_URL=
FEISHU_WEBHOOK_ENABLED=false
FEISHU_WEBHOOK_TIMEOUT_MS=3000
FEISHU_WEBHOOK_SECRET=
FLYWAY_BASELINE_ON_MIGRATE=false
ENVEOF
chmod 600 backend/backend.env
EOF
```

> `JWT_SECRET` 必须是合法 Base64 且解码后 ≥ 32 字节（`JwtUtil` 直接 `Base64.decode` 后交给
> `Keys.hmacShaKeyFor`）。`openssl rand -base64 32` 正好满足。
> 填了 `CHANGE_ME` 会让容器启动即崩（`Illegal base64 character 5f`）。

**d) 准备数据库**

全新库：什么都不用做，`V1` 会自动建表 + 写种子数据。

接管存量库（已有表但没有 Flyway 历史）：见下面的「数据库迁移」。

#### 3. 发布

```bash
DEPLOY_HOST=ubuntu@43.143.244.13 \
DEPLOY_DIR=/home/ubuntu/jenkins-deploy/backend \
DEPLOY_SSH_KEY=~/.ssh/id_ed25519 \
BACKEND_NETWORK=deploy-net \
./scripts/deploy-prod.sh
```

脚本六步：

1. **本地构建** —— `docker build --platform $DEPLOY_PLATFORM`（自动探测可用镜像源）
2. **导出上传** —— `docker save | gzip` → `scp` → 远端 `docker load`
3. **校验架构** —— 远端 `docker image inspect` 的平台必须与目标一致
4. **上传编排文件** —— `docker-compose.prod.yml`；`backend.env` 默认也上传（`SKIP_ENV=1` 则保留远端现有）
5. **启动** —— 校验外部网络存在 → 删掉同名旧容器 → `docker compose up -d`
6. **健康检查** —— 轮询 `http://localhost:8888/design/actuator/health`，通过后打印访问地址

脚本可用环境变量：

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DEPLOY_HOST` | **必填** | ssh 目标 `user@host` |
| `DEPLOY_DIR` | `/opt/greatbus` | 远端部署目录（生产用 `~/jenkins-deploy/backend`） |
| `DEPLOY_PORT` | `22` | ssh 端口 |
| `DEPLOY_SSH_KEY` | — | ssh 私钥路径 |
| `BACKEND_IMAGE` | `leiw-go/back` | 镜像名 |
| `BACKEND_PORT` | `8888` | 容器对外端口 |
| `BACKEND_NETWORK` | `deploy-net` | 后端与 MySQL 共用的外部网络 |
| `BACKEND_CONTAINER_NAME` | `backend` | 容器名 |
| `DEPLOY_PLATFORM` | `linux/amd64` | 目标架构，见下方说明 |
| `SKIP_BUILD` | `0` | `1` = 跳过本地构建，复用已有镜像 |
| `SKIP_ENV` | `0` | `1` = 不上传 env，保留远端现有配置 |

> ⚠️ **镜像架构必须和远程机一致**。脚本默认按 `linux/amd64` 构建（远程机一般是 x86_64）。
> 如果你在 Apple Silicon Mac 上开发、远程机是 x86_64，**不要改这个默认值** ——
> 默认值就是对的。若远程机是 arm64，把 `DEPLOY_PLATFORM` 改成 `linux/arm64`。
>
> 原因：基础镜像 `eclipse-temurin:17-jre-alpine` 在国内镜像源上**只有 amd64 版本**，
> 在 arm64 机器上直接构建会报 `no match for platform in manifest`。
> 脚本会在本地构建后、远端加载后各校验一次架构，不一致直接终止并给出提示。

> ⚠️ **Bash 变量名不能跟中文字符相邻**。`echo "...$VAR）..."` 会把 `）` 算进变量名，
> 报 `unbound variable`。脚本里凡是用到都写成 `${VAR}`。

#### 4. 验证

```bash
# 容器状态（应为 healthy）
ssh -i ~/.ssh/id_ed25519 ubuntu@43.143.244.13 \
  'docker ps --format "{{.Names}}\t{{.Status}}\t{{.Ports}}" | grep backend'

# 健康检查
ssh -i ~/.ssh/id_ed25519 ubuntu@43.143.244.13 \
  'curl -s http://localhost:8888/design/actuator/health'      # {"status":"UP"}

# 登录并取数据
TOKEN=$(curl -s -X POST http://localhost:8888/design/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8888/design/api/users

# Flyway 是否正常执行
docker exec mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e \
  "SELECT version, description, success FROM project_info_manage.flyway_schema_history;"
```

### 日常发布

改完代码后重复「发布 + 验证」两步即可：

```bash
# 本地先自检
./mvnw -B clean test && ./mvnw -B -DskipTests package

# 发布
DEPLOY_HOST=ubuntu@43.143.244.13 \
DEPLOY_DIR=/home/ubuntu/jenkins-deploy/backend \
DEPLOY_SSH_KEY=~/.ssh/id_ed25519 \
BACKEND_NETWORK=deploy-net \
./scripts/deploy-prod.sh
```

### 回滚

发布前先给当前镜像打一个 tag，回滚时把 compose 指回旧 tag 即可：

```bash
# 发布前
ssh -i ~/.ssh/id_ed25519 ubuntu@43.143.244.13 \
  'docker tag leiw-go/back:latest leiw-go/back:rollback-$(date +%Y%m%d%H%M)'

# 回滚（远端执行）
cd ~/jenkins-deploy/backend
BACKEND_IMAGE=leiw-go/back BACKEND_TAG=rollback-202611072359 docker compose up -d
```

> ⚠️ 回滚只回滚**代码**，不回滚**数据库**。Flyway 迁移一旦执行就不会自动撤销，
> schema 变更需要另写一个反向迁移。

### 数据库迁移

#### Flyway 行为

`spring.flyway.baseline-version: "1"` 决定存量库怎么接入：

| 库状态 | 行为 |
| --- | --- |
| 空库 | 正常执行 `V1`（建表 + 种子数据） |
| 已有非空库 | baseline 登记为 V1 → **跳过 V1**，库保持原样 |

`FLYWAY_BASELINE_ON_MIGRATE` 控制"已有非空库"时是否自动 baseline：

- dev：`true`（本地老库能自动接管）
- prod：`false` —— 防止连错库时静默 baseline 一个无关的库。
  **首次接管存量生产库时必须显式传 `FLYWAY_BASELINE_ON_MIGRATE=true`**，否则启动会因
  "非空 schema 且未 baseline"直接失败。

改表结构一律**新增** `V2__xxx.sql`、`V3__xxx.sql`...，**不要回头改 V1**
（已执行过的迁移校验和不匹配会导致启动失败）。

#### 接管存量库：备份 + 重建

生产库若已有表结构但没有 Flyway 历史，MySQL **不支持 `RENAME DATABASE`**，
要靠跨库移动表：

```bash
# 远程机上执行（示例）
BACKUP_DB="project_info_manage_bak_$(date +%Y%m%d_%H%M%S)"
docker exec mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" \
  -e "CREATE DATABASE ${BACKUP_DB} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 触发器不能跨 schema 移动 —— 先删（mysqldump 里已有备份）
docker exec mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" \
  -e "DROP TRIGGER IF EXISTS project_info_manage.trg_t_lottery_period_uuid;"

# 逐表移动（MySQL 不支持 RENAME TABLE a.* TO b.*）
docker exec mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "
RENAME TABLE project_info_manage.t_user TO ${BACKUP_DB}.t_user,
             project_info_manage.t_role TO ${BACKUP_DB}.t_role,
             project_info_manage.t_permission TO ${BACKUP_DB}.t_permission,
             project_info_manage.t_user_role TO ${BACKUP_DB}.t_user_role,
             project_info_manage.t_role_permission TO ${BACKUP_DB}.t_role_permission,
             project_info_manage.t_product TO ${BACKUP_DB}.t_product,
             project_info_manage.t_lottery_period TO ${BACKUP_DB}.t_lottery_period;"

# 重建干净的库
docker exec mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" \
  -e "CREATE DATABASE project_info_manage CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

别忘了再存一份 dump 双保险：

```bash
docker exec mysql mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" \
  --single-transaction --routines --triggers project_info_manage \
  > ~/jenkins-deploy/backups/project_info_manage_$(date +%Y%m%d_%H%M%S).sql
```

### 数据库连接

MySQL 是远程机上的独立实例，**不由本项目编排**。两种写法：

```bash
# 同机（后端与 MySQL 在同一个 docker 网络）—— 用容器名解析
DB_URL=jdbc:mysql://mysql:3306/project_info_manage?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf-8

# 异机 —— 写 MySQL 的内网 IP / 域名
DB_URL=jdbc:mysql://10.0.0.5:3306/project_info_manage?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf-8
```

compose 里已配 `host.docker.internal:host-gateway`，所以同机也能用
`jdbc:mysql://host.docker.internal:3306/...` 这种写法（走宿主机映射端口）。

### 故障排查

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 容器起不来，日志 `Illegal base64 character 5f` | `JWT_SECRET` 不是合法 Base64 | 检查 `backend.env`，用 `openssl rand -base64 32` 重新生成 |
| 启动报 `Found more than one migration with version N` | 删改过 `db/migration/` 下的文件，但 `target/classes` 里还有旧文件被打进 jar | `./mvnw clean package` |
| 启动报 `no match for platform in manifest` | 在 arm64 机器上构建，但基础镜像只有 amd64 | 加 `--platform linux/amd64`（脚本已按 `DEPLOY_PLATFORM` 处理） |
| 启动报 `non-empty schema without history` | 存量库首次接入，没开 baseline | 首次部署设 `FLYWAY_BASELINE_ON_MIGRATE=true`，之后改回 `false` |
| 容器反复重启 | 8888 端口被占 / 数据库连不上 | `ss -lntp | grep 8888`；查 `docker logs backend` |
| `docker compose up` 报容器名冲突 | 有旧的 `docker run` 起的同名容器 | 脚本会自动 `docker rm -f`；手动部署时注意 |
| 公网访问失败但直连 8888 正常 | nginx / 证书问题 | 见下方「生产环境现状」里的证书说明 |
| 远程机目录写不进去 | `/opt` 等目录 root 拥有 | 换个可写目录（`~/xxx`），或给远程机配免密 sudo（脚本会自动用） |

### 本地手动构建镜像

```bash
docker build -f Dockerfile -t leiw-go/back:dev .
```

### Jenkins（同机部署）

`Jenkinsfile` 走两个 stage：构建镜像 → 部署容器。**注意它部署到的是 Jenkins agent 自己那台机器**，
不是远程机 —— 远程发布请用上面的 `deploy-prod.sh`。参数：

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

同机部署脚本也可以单独跑（参数顺序固定）：

```bash
bash scripts/deploy-backend.sh <name> <port> <profile> <network> <image> \
  [db_url] [db_user] [db_pass] [jwt_secret] [jwt_expiration]
```

---

## 生产环境现状（43.143.244.13）

| 项 | 值 |
| --- | --- |
| 主机 | `ubuntu@43.143.244.13`（Ubuntu 24.04 / x86_64 / Docker 29.5.3，1.9G 内存） |
| 部署目录 | `/home/ubuntu/jenkins-deploy/backend/`（compose + `backend.env`，权限 600） |
| 容器 | `backend`，`deploy-net` 网络，宿主 `8888:8888`，`restart: unless-stopped` |
| MySQL | 同机 `deploy-net` 上的 `mysql` 容器 → `DB_URL` 用 `mysql:3306` 解析 |
| 公网入口 | `https://design.yaowen.store/design/...`（nginx → `127.0.0.1:8888`） |

后端必须占用宿主 **8888** 端口 —— nginx 的 upstream 写的是 `127.0.0.1:8888`，
且应用的 `context-path` 是 `/design`，所以对外路径是 `/design/api/...` 而**不是** `/api/...`。

> 机器上 `services.conf` 里的 `location /api/` 指向的是**另一个 Go 服务**，不是本项目。

> ⚠️ **`backend.env` 里有真实凭据，绝不能提交**（已在 `.gitignore`）。
> 脚本会拒绝上传仍含 `CHANGE_ME` 占位符的 env —— 那会让应用启动即崩。

> ✅ **证书已更换**（2026-11-08）：`design.yaowen.store` 与 `yaowen.store` 的证书原为
> `notAfter=Oct 19 / Oct 6 2026`（已过期），已换成新签发的 `notAfter=Feb 5 2027`，
> 完整证书链校验通过（`Verify return code: 0`）。旧证书备份在
> `/home/ubuntu/yaowen.store_nginx/backup_20261108_012606/`。
>
> 换证步骤：上传 zip → 解压覆盖 `_bundle.crt` / `.key` → `nginx -t` → `nginx -s reload`。
> 私钥权限必须是 600。

> ⚠️ 远程机上还留着旧部署方式：`.shared/Jenkinsfile`（用 `docker run` + Nacos 环境变量）和
> `docker-compose.yml`（含 jenkins + mysql）。**不要**用它们部署后端 —— 会与
> `deploy/docker-compose.prod.yml` 抢容器名和 8888 端口。

### 数据库迁移记录

2026-11-07 首次接管生产库时，旧库（`spring.sql.init.mode: always` 建的，带 4 个外键）已改名备份：

| 备份 | 内容 |
| --- | --- |
| 库 `project_info_manage_bak_20261107_235151` | 7 张表 + 触发器 `trg_t_lottery_period_uuid`，含 2890 行历史开奖数据 |
| 文件 `/home/ubuntu/jenkins-deploy/backups/project_info_manage_20261107_234841.sql` | 上述库的 mysqldump |

新库 `project_info_manage` 由 `V1__init_project_info_manage.sql` 从零建成（Flyway 真实执行，非 baseline）。

---

## 项目结构

```
.
├── pom.xml                              # Maven 主 POM
├── settings.xml                         # Maven 镜像（华为云）
├── Dockerfile                           # 后端多阶段镜像
├── docker-compose.mysql.yml             # 本机 dev 用 MySQL
├── deploy/
│   ├── docker-compose.prod.yml          # 生产编排（只编排后端）
│   ├── backend.env.example              # 生产环境变量模板
│   └── backend.env                      # 实际配置（gitignore）
├── Jenkinsfile                          # Jenkins 流水线
├── checkstyle_huawei.xml                # 华为代码规约
├── scripts/
│   ├── deploy-prod.sh                   # 一键发布到远程机（build→传镜像→compose up→健康检查）
│   ├── deploy-backend.sh                # 启/重启后端容器（同机）
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
