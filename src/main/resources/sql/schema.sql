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

-- 聚宽量化策略管理相关表
CREATE TABLE IF NOT EXISTS t_quant_account (
    id VARCHAR(36) PRIMARY KEY,
    account_name VARCHAR(128) NOT NULL COMMENT '账户别名',
    username VARCHAR(128) NOT NULL COMMENT '聚宽用户名',
    password VARCHAR(512) NOT NULL COMMENT '聚宽密码 (AES 加密后存储, 应用层负责加解密)',
    phone VARCHAR(20) COMMENT '绑定的手机号(用于短信验证)',
    is_active TINYINT DEFAULT 1 COMMENT '1: 启用, 0: 禁用',
    last_login_time TIMESTAMP NULL COMMENT '最近登录时间',
    last_login_status VARCHAR(512) COMMENT '最近登录状态信息',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_account_name (account_name)
);

CREATE TABLE IF NOT EXISTS t_quant_strategy (
    id VARCHAR(36) PRIMARY KEY,
    account_id VARCHAR(36) NOT NULL COMMENT '所属聚宽账户ID',
    name VARCHAR(128) NOT NULL COMMENT '策略名称',
    description VARCHAR(512) COMMENT '策略描述',
    code MEDIUMTEXT NOT NULL COMMENT '策略 Python 源码',
    parameters TEXT COMMENT 'JSON 格式的策略参数',
    strategy_type VARCHAR(16) NOT NULL DEFAULT 'BACKTEST' COMMENT 'BACKTEST/SIM/LIVE',
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/RUNNING/STOPPED/ERROR/COMPLETED',
    initial_capital DECIMAL(18, 4) DEFAULT 100000.00 COMMENT '初始资金',
    benchmark VARCHAR(32) DEFAULT '000300.XSHG' COMMENT '基准指数',
    start_date DATE COMMENT '回测/运行开始日期',
    end_date DATE COMMENT '回测结束日期',
    frequency VARCHAR(8) DEFAULT 'day' COMMENT '运行频率: day/minute/5m/15m/30m/60m',
    jq_strategy_id VARCHAR(64) COMMENT '聚宽平台返回的策略ID',
    jq_backtest_id VARCHAR(64) COMMENT '聚宽平台返回的回测ID',
    last_run_time TIMESTAMP NULL COMMENT '最近运行时间',
    last_error VARCHAR(2048) COMMENT '最近错误信息',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_account_id (account_id),
    INDEX idx_status (status),
    INDEX idx_strategy_type (strategy_type)
);

CREATE TABLE IF NOT EXISTS t_quant_strategy_log (
    id VARCHAR(36) PRIMARY KEY,
    strategy_id VARCHAR(36) NOT NULL COMMENT '策略ID',
    run_id VARCHAR(64) COMMENT '运行ID (回测ID等)',
    level VARCHAR(16) NOT NULL DEFAULT 'INFO' COMMENT 'DEBUG/INFO/WARN/ERROR',
    message TEXT COMMENT '日志内容',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_strategy_id (strategy_id),
    INDEX idx_run_id (run_id)
);

CREATE TABLE IF NOT EXISTS t_quant_strategy_metric (
    id VARCHAR(36) PRIMARY KEY,
    strategy_id VARCHAR(36) NOT NULL COMMENT '策略ID',
    run_id VARCHAR(64) NOT NULL COMMENT '运行ID',
    total_return DECIMAL(18, 6) COMMENT '总收益率',
    annual_return DECIMAL(18, 6) COMMENT '年化收益率',
    sharpe_ratio DECIMAL(18, 6) COMMENT '夏普比率',
    max_drawdown DECIMAL(18, 6) COMMENT '最大回撤',
    alpha DECIMAL(18, 6) COMMENT 'Alpha',
    beta DECIMAL(18, 6) COMMENT 'Beta',
    win_rate DECIMAL(18, 6) COMMENT '胜率',
    volatility DECIMAL(18, 6) COMMENT '波动率',
    total_trade_count INT COMMENT '总交易次数',
    raw_json MEDIUMTEXT COMMENT '原始指标 JSON',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_strategy_id (strategy_id),
    INDEX idx_run_id (run_id)
);
