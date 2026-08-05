/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

-- 覆盖 V1 baseline + V2 llm_relay 的全部 9 张表（H2 兼容版）。
--   - 去掉了 MySQL 专属的 ENGINE / CHARSET / COLLATE / DEFAULT (UUID()) / COMMENT
--   - UUID 主键全部改用字面量（外键测试可控）
--   - TIMESTAMP DEFAULT CURRENT_TIMESTAMP 在 H2 上 OK，ON UPDATE CURRENT_TIMESTAMP 也支持
--   - VARBINARY(512) / DECIMAL(10,2) / TINYINT H2 都支持
--   - 注意：AuthServiceImpl 默认 USER 角色 ID 必须是
--         '550e8400-e29b-41d4-a716-446655440102'（DEFAULT_USER_ROLE_ID 常量）

-- 让 schema 幂等 —— 同 JVM 内多个 IT 类共享 H2 in-memory DB（DB_CLOSE_DELAY=-1），
-- 先 DROP ALL OBJECTS 再 CREATE，避免后续测试类跑 schema 时表已存在而报 "Table already exists"。
DROP ALL OBJECTS;

CREATE TABLE t_user (
    id VARCHAR(36) NOT NULL,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(256) NOT NULL,
    real_name VARCHAR(64),
    email VARCHAR(128),
    phone VARCHAR(20),
    status TINYINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (username)
);

CREATE TABLE t_role (
    id VARCHAR(36) NOT NULL,
    role_name VARCHAR(64) NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    description VARCHAR(256),
    status TINYINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (role_name),
    UNIQUE (role_code)
);

CREATE TABLE t_permission (
    id VARCHAR(36) NOT NULL,
    permission_name VARCHAR(64) NOT NULL,
    permission_code VARCHAR(64) NOT NULL,
    description VARCHAR(256),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (permission_code)
);

CREATE TABLE t_product (
    id VARCHAR(36) NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    product_code VARCHAR(64) NOT NULL,
    category VARCHAR(64),
    description VARCHAR(512),
    price DECIMAL(10,2),
    status TINYINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (product_code)
);

CREATE TABLE t_lottery_period (
    id VARCHAR(36) NOT NULL,
    period VARCHAR(20) NOT NULL,
    draw_date DATE NOT NULL,
    front_1 INT NOT NULL,
    front_2 INT NOT NULL,
    front_3 INT NOT NULL,
    front_4 INT NOT NULL,
    front_5 INT NOT NULL,
    back_1 INT NOT NULL,
    back_2 INT NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (period)
);

CREATE TABLE t_user_role (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(36) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (user_id) REFERENCES t_user(id),
    FOREIGN KEY (role_id) REFERENCES t_role(id)
);

CREATE TABLE t_role_permission (
    id VARCHAR(36) NOT NULL,
    role_id VARCHAR(36) NOT NULL,
    permission_id VARCHAR(36) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    FOREIGN KEY (role_id) REFERENCES t_role(id),
    FOREIGN KEY (permission_id) REFERENCES t_permission(id)
);

CREATE TABLE t_llm_provider (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(64) NOT NULL,
    base_url VARCHAR(512) NOT NULL,
    api_key_ciphertext VARBINARY(512) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    priority INT NOT NULL DEFAULT 100,
    timeout_ms_override INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (name)
);

CREATE TABLE t_llm_request_log (
    id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    model VARCHAR(128) NOT NULL,
    endpoint VARCHAR(32) NOT NULL,
    prompt_tokens INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    total_tokens INT NOT NULL DEFAULT 0,
    status_code INT NOT NULL,
    latency_ms INT NOT NULL DEFAULT 0,
    request_id VARCHAR(64),
    trace_id VARCHAR(32),
    error_message VARCHAR(512),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);