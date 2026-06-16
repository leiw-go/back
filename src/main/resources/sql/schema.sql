-- Schema initialization script for ProjectInfomationManage
-- Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.

CREATE TABLE IF NOT EXISTS t_user (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(256) NOT NULL,
    real_name VARCHAR(64),
    email VARCHAR(128),
    phone VARCHAR(20),
    status TINYINT DEFAULT 1 COMMENT '1: active, 0: inactive',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_role (
    id VARCHAR(36) PRIMARY KEY,
    role_name VARCHAR(64) NOT NULL UNIQUE,
    role_code VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(256),
    status TINYINT DEFAULT 1 COMMENT '1: active, 0: inactive',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_permission (
    id VARCHAR(36) PRIMARY KEY,
    permission_name VARCHAR(64) NOT NULL,
    permission_code VARCHAR(64) NOT NULL UNIQUE,
    description VARCHAR(256),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS t_user_role (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(36) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES t_user(id),
    FOREIGN KEY (role_id) REFERENCES t_role(id)
);

CREATE TABLE IF NOT EXISTS t_role_permission (
    id VARCHAR(36) PRIMARY KEY,
    role_id VARCHAR(36) NOT NULL,
    permission_id VARCHAR(36) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (role_id) REFERENCES t_role(id),
    FOREIGN KEY (permission_id) REFERENCES t_permission(id)
);

CREATE TABLE IF NOT EXISTS t_product (
    id VARCHAR(36) PRIMARY KEY,
    product_name VARCHAR(128) NOT NULL,
    product_code VARCHAR(64) NOT NULL UNIQUE,
    category VARCHAR(64),
    description VARCHAR(512),
    price DECIMAL(10, 2),
    status TINYINT DEFAULT 1 COMMENT '1: active, 0: inactive',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
 CREATE TABLE IF NOT EXISTS t_lottery_period (
     id VARCHAR(36) PRIMARY KEY,
     `period` VARCHAR(20) NOT NULL UNIQUE COMMENT '期数',
     draw_date DATE NOT NULL COMMENT '开奖日期',
     front_1 INT NOT NULL COMMENT '前区一号',
     front_2 INT NOT NULL COMMENT '前区二号',
     front_3 INT NOT NULL COMMENT '前区三号',
     front_4 INT NOT NULL COMMENT '前区四号',
     front_5 INT NOT NULL COMMENT '前区五号',
     back_1 INT NOT NULL COMMENT '后区一号',
     back_2 INT NOT NULL COMMENT '后区二号',
     create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
     INDEX idx_draw_date (draw_date),
     INDEX idx_period (`period`)
 );
