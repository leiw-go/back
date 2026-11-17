-- H2 友好的 baseline schema。仅包含 AuthController 链路 (register / login / currentUser) 触达的三张表：
--   t_user / t_role / t_user_role。
-- 与生产 V1__baseline_project_info_manage.sql 字段对齐，但去掉 MySQL 专属语法：
--   - 去掉 ENGINE=InnoDB / DEFAULT CHARSET=utf8mb4 / COLLATE=utf8mb4_0900_ai_ci
--   - 去掉 COMMENT '...' 子句
--   - 去掉 DEFAULT (UUID()) 表达式（H2/MySQL mode 不支持函数默认值）
-- 由 spring.sql.init 在 AuthControllerIntegrationTests 启动时执行。
-- 不允许在此文件中添加生产代码会触达的字段或新表。

CREATE TABLE t_user (
    id            VARCHAR(36)   NOT NULL,
    username      VARCHAR(64)   NOT NULL,
    password      VARCHAR(256)  NOT NULL,
    real_name     VARCHAR(64),
    email         VARCHAR(128),
    phone         VARCHAR(20),
    status        TINYINT       DEFAULT 1,
    create_time   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (username)
);

CREATE TABLE t_role (
    id            VARCHAR(36)   NOT NULL,
    role_name     VARCHAR(64)   NOT NULL,
    role_code     VARCHAR(64)   NOT NULL,
    description   VARCHAR(256),
    status        TINYINT       DEFAULT 1,
    create_time   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    update_time   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (role_name),
    UNIQUE (role_code)
);

CREATE TABLE t_user_role (
    id            VARCHAR(36)   NOT NULL,
    user_id       VARCHAR(36)   NOT NULL,
    role_id       VARCHAR(36)   NOT NULL,
    create_time   TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES t_user(id),
    FOREIGN KEY (role_id) REFERENCES t_role(id)
);
