-- V7: CRM 站内信 + 邮件 + 汇率域
-- 约定（见 docs/crm-backend-requirements.md §5.1）：
--   * MySQL 8 / utf8mb4_unicode_ci
--   * 不建物理外键，关联关系由 Service 层维护（逻辑 mapping）
--   * 主键 VARCHAR(36)，应用层生成 UUID
--   * 逻辑删除 deleted BIGINT（0 = 未删除，删除时写入毫秒时间戳）
--   * 唯一例外：t_email_log 是流水表，物理删除，不设 deleted

-- ============================================================
-- 站内信（只 1 张表，不建"消息-收件人"关联表：群发靠 fan-out）
-- ============================================================
CREATE TABLE `t_message` (
    `id` VARCHAR(36) NOT NULL,
    `message_type` VARCHAR(32) NOT NULL COMMENT '消息类型，字典 message_type',
    `level` VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL / IMPORTANT / URGENT',
    `title` VARCHAR(255) NOT NULL COMMENT '标题',
    `content` VARCHAR(2000) DEFAULT NULL COMMENT '正文（渲染后的文案快照）',
    `sender_type` VARCHAR(16) NOT NULL DEFAULT 'SYSTEM' COMMENT 'SYSTEM / USER',
    `sender_id` VARCHAR(36) DEFAULT NULL COMMENT '系统消息为空；人工消息为发送人',
    `receiver_id` VARCHAR(36) NOT NULL COMMENT '收件人 → t_user.id',
    `biz_type` VARCHAR(32) DEFAULT NULL COMMENT 'CUSTOMER/OPPORTUNITY/INQUIRY/QUOTATION/ORDER/RECEIVABLE/CURRENCY',
    `biz_id` VARCHAR(36) DEFAULT NULL COMMENT '关联业务主键（点击跳转）',
    `biz_title` VARCHAR(255) DEFAULT NULL COMMENT '冗余业务标题（列表免回查）',
    `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '0 未读 / 1 已读',
    `read_at` DATETIME DEFAULT NULL COMMENT '已读时间',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_receiver_read` (`receiver_id`, `is_read`, `create_time`),
    KEY `idx_biz` (`biz_type`, `biz_id`),
    KEY `idx_type` (`message_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 站内信';

-- ============================================================
-- 邮件三表
-- ============================================================
CREATE TABLE `t_email_template` (
    `id` VARCHAR(36) NOT NULL,
    `name` VARCHAR(128) NOT NULL COMMENT '模板名',
    `subject` VARCHAR(255) NOT NULL COMMENT '主题',
    `content` LONGTEXT NOT NULL COMMENT '正文，变量占位 {{customer.company_name}}',
    `language` VARCHAR(16) NOT NULL DEFAULT 'zh' COMMENT '语言分类（人工标签）',
    `template_type` VARCHAR(32) NOT NULL COMMENT '类型：开发信 / 报价跟进 / 催款 等',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`, `deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 邮件模板';

CREATE TABLE `t_email_campaign` (
    `id` VARCHAR(36) NOT NULL,
    `name` VARCHAR(128) NOT NULL COMMENT '群发任务名',
    `template_id` VARCHAR(36) NOT NULL COMMENT '模板 → t_email_template.id',
    `target_type` VARCHAR(16) NOT NULL COMMENT '范围 TAG / OWNER / CUSTOMER',
    `target_value` VARCHAR(128) NOT NULL COMMENT '范围值：标签 ID / 负责人 ID / 客户 ID',
    `campaign_status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / SENT',
    `total_count` INT NOT NULL DEFAULT 0 COMMENT '目标收件人数',
    `success_count` INT NOT NULL DEFAULT 0 COMMENT '发送成功数',
    `fail_count` INT NOT NULL DEFAULT 0 COMMENT '发送失败数',
    `sent_at` DATETIME DEFAULT NULL COMMENT '发送时间',
    `owner_id` VARCHAR(36) NOT NULL COMMENT '创建人',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_owner` (`owner_id`),
    KEY `idx_status` (`campaign_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 邮件群发任务';

-- 流水表：物理删除，不设 deleted
CREATE TABLE `t_email_log` (
    `id` VARCHAR(36) NOT NULL,
    `campaign_id` VARCHAR(36) NOT NULL COMMENT '群发任务 → t_email_campaign.id',
    `contact_id` VARCHAR(36) DEFAULT NULL COMMENT '联系人 → t_customer_contact.id',
    `email` VARCHAR(128) NOT NULL COMMENT '收件邮箱',
    `send_status` VARCHAR(16) NOT NULL COMMENT 'SUCCESS / FAILED',
    `error_msg` VARCHAR(512) DEFAULT NULL COMMENT '失败原因',
    `sent_at` DATETIME DEFAULT NULL COMMENT '发送时间',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_campaign` (`campaign_id`),
    KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 邮件发送记录（流水表）';

-- ============================================================
-- 汇率四表（详见 docs/crm-currency-rate-design.md §7）
-- 迁移只建空表，不灌初始汇率数据
-- ============================================================
CREATE TABLE `t_currency` (
    `id` VARCHAR(36) NOT NULL,
    `currency_code` CHAR(3) NOT NULL COMMENT 'ISO 4217 三字母码',
    `currency_name` VARCHAR(64) NOT NULL COMMENT '币种名称',
    `symbol` VARCHAR(16) DEFAULT NULL COMMENT '符号',
    `numeric_code` CHAR(3) DEFAULT NULL COMMENT 'ISO 4217 数字码',
    `is_common` TINYINT NOT NULL DEFAULT 0 COMMENT '常用币标记（前端下拉排序）',
    `currency_level` VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT 'CORE / NORMAL（分级阈值）',
    `status` VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL / SUSPECT（熔断中）/ DISABLED（停用）',
    `suspect_days` INT NOT NULL DEFAULT 0 COMMENT '连续超标天数（熔断判定用）',
    `recover_days` INT NOT NULL DEFAULT 0 COMMENT '连续达标天数（恢复判定用）',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_currency_code` (`currency_code`),
    KEY `idx_common` (`is_common`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 币种字典';

CREATE TABLE `t_currency_rate` (
    `id` VARCHAR(36) NOT NULL,
    `base_currency` CHAR(3) NOT NULL COMMENT '基准币（统一 USD）',
    `quote_currency` CHAR(3) NOT NULL COMMENT '目标币',
    `rate` DECIMAL(18,8) NOT NULL COMMENT '汇率',
    `rate_date` DATE NOT NULL COMMENT '汇率日期（按该币种自己的源日期）',
    `source` VARCHAR(16) NOT NULL COMMENT 'FRANKFURTER / OPEN_ER_API / MANUAL',
    `cross_source` VARCHAR(16) DEFAULT NULL COMMENT '比对源标识',
    `cross_status` VARCHAR(16) NOT NULL COMMENT 'MATCH / WARN / SINGLE / REJECT / MANUAL',
    `deviation_rate` DECIMAL(12,6) DEFAULT NULL COMMENT '与比对源的偏差（%）',
    `date_gap_days` INT NOT NULL DEFAULT 0 COMMENT '两源数据日期差（天）',
    `checked_at` DATETIME DEFAULT NULL COMMENT '交叉比对时间',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_pair_date` (`base_currency`, `quote_currency`, `rate_date`),
    KEY `idx_quote_date` (`quote_currency`, `rate_date`),
    KEY `idx_cross_status` (`cross_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 汇率';

CREATE TABLE `t_currency_sync_log` (
    `id` VARCHAR(36) NOT NULL COMMENT '主键 = syncId',
    `trigger_type` VARCHAR(16) NOT NULL COMMENT 'SCHEDULE / MANUAL',
    `primary_status` VARCHAR(16) NOT NULL COMMENT 'OK / FAILED / RATE_LIMITED / SKIPPED',
    `primary_rows` INT NOT NULL DEFAULT 0 COMMENT '主源拉到的条数',
    `primary_date` DATE DEFAULT NULL COMMENT '主源数据日期',
    `secondary_status` VARCHAR(16) NOT NULL COMMENT 'OK / FAILED / RATE_LIMITED / REUSED / SKIPPED',
    `secondary_rows` INT NOT NULL DEFAULT 0 COMMENT '比对源条数',
    `secondary_date` DATE DEFAULT NULL COMMENT '比对源快照日期',
    `compared_count` INT NOT NULL DEFAULT 0 COMMENT '实际参与比对的币种数',
    `matched_count` INT NOT NULL DEFAULT 0 COMMENT '一致数',
    `warned_count` INT NOT NULL DEFAULT 0 COMMENT '警告数',
    `rejected_count` INT NOT NULL DEFAULT 0 COMMENT '拒绝数',
    `single_count` INT NOT NULL DEFAULT 0 COMMENT '单源数',
    `saved_count` INT NOT NULL DEFAULT 0 COMMENT '实际入库数',
    `skipped_count` INT NOT NULL DEFAULT 0 COMMENT '跳过数（熔断币种 / 手工维护行）',
    `max_deviation` DECIMAL(12,6) DEFAULT NULL COMMENT '本次最大偏差（%）',
    `max_deviation_currency` CHAR(3) DEFAULT NULL COMMENT '最大偏差对应币种',
    `sync_status` VARCHAR(16) NOT NULL COMMENT 'SUCCESS / PARTIAL / FAILED',
    `error_msg` VARCHAR(1024) DEFAULT NULL COMMENT '失败原因',
    `duration_ms` INT DEFAULT NULL COMMENT '耗时',
    `started_at` DATETIME NOT NULL COMMENT '开始时间',
    `finished_at` DATETIME DEFAULT NULL COMMENT '结束时间',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_started` (`started_at`),
    KEY `idx_status` (`sync_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 汇率同步批次日志';

CREATE TABLE `t_currency_rate_check` (
    `id` VARCHAR(36) NOT NULL,
    `sync_id` VARCHAR(36) NOT NULL COMMENT '关联 t_currency_sync_log.id',
    `base_currency` CHAR(3) NOT NULL COMMENT '基准币',
    `quote_currency` CHAR(3) NOT NULL COMMENT '目标币',
    `primary_rate` DECIMAL(18,8) DEFAULT NULL COMMENT '主源值',
    `primary_date` DATE DEFAULT NULL COMMENT '主源该币种的日期',
    `secondary_rate` DECIMAL(18,8) DEFAULT NULL COMMENT '比对源值',
    `secondary_date` DATE DEFAULT NULL COMMENT '比对源快照日期',
    `deviation_rate` DECIMAL(12,6) DEFAULT NULL COMMENT '偏差（%）',
    `date_gap_days` INT NOT NULL DEFAULT 0 COMMENT '日期差',
    `threshold` DECIMAL(12,6) DEFAULT NULL COMMENT '本次适用的阈值',
    `check_result` VARCHAR(16) NOT NULL COMMENT 'MATCH / WARN / REJECT / SKIP / SINGLE',
    `reject_reason` VARCHAR(64) DEFAULT NULL COMMENT '拒绝原因码',
    `checked_at` DATETIME NOT NULL COMMENT '比对时间',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sync_currency` (`sync_id`, `quote_currency`),
    KEY `idx_quote` (`quote_currency`),
    KEY `idx_result` (`check_result`),
    KEY `idx_checked` (`checked_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 汇率交叉比对明细';

-- ============================================================
-- 字典：站内信消息类型
-- ============================================================
INSERT INTO `t_dict_type` (`id`, `type_code`, `type_name`, `description`) VALUES
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f8001', 'message_type', '站内信消息类型', '业务事件触发的站内信类型');

INSERT INTO `t_dict_item` (`id`, `type_code`, `item_code`, `item_name`, `sort_no`) VALUES
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f9001', 'message_type', 'QUOTATION_REJECTED', '报价被拒绝', 1),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f9002', 'message_type', 'QUOTATION_CONFIRMED', '报价被确认', 2),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f9003', 'message_type', 'QUOTATION_EXPIRING', '报价即将过期', 3),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f9004', 'message_type', 'ORDER_CONFIRMED', '订单已确认', 4),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f9005', 'message_type', 'ORDER_SHIPPED', '订单已发货', 5),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f9006', 'message_type', 'RECEIVABLE_OVERDUE', '应收逾期', 6),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f9007', 'message_type', 'INQUIRY_TIMEOUT', '询盘超时未报价', 7),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f9008', 'message_type', 'CUSTOMER_ASSIGNED', '客户转交', 8),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f9009', 'message_type', 'OPPORTUNITY_STAGE', '商机阶段变更', 9),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f900a', 'message_type', 'RATE_SYNC_FAILED', '汇率同步失败', 10),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f900b', 'message_type', 'RATE_CURRENCY_SUSPENDED', '汇率币种熔断', 11),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f900c', 'message_type', 'MANUAL', '人工消息', 12);
