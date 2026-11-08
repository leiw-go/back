-- V4: CRM 商机域（销售漏斗）
-- 约定（见 docs/crm-backend-requirements.md §5.1）：
--   * MySQL 8 / utf8mb4_unicode_ci
--   * 不建物理外键，customer_id 由 Service 层校验存在
--   * 主键 VARCHAR(36)，应用层生成 UUID
--   * 逻辑删除 deleted BIGINT（0 = 未删除，删除时写入毫秒时间戳）
--   * 金额：amount 原币，amount_base 本位币（USD）快照

CREATE TABLE `t_opportunity` (
    `id` VARCHAR(36) NOT NULL,
    `customer_id` VARCHAR(36) NOT NULL COMMENT '所属客户 → t_customer.id',
    `name` VARCHAR(128) NOT NULL COMMENT '商机名称',
    `stage` VARCHAR(24) NOT NULL DEFAULT 'INITIAL_CONTACT' COMMENT '阶段，字典 opportunity_stage',
    `amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '预计金额（原币）',
    `currency_code` CHAR(3) NOT NULL DEFAULT 'USD' COMMENT '原币币种',
    `amount_base` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '本位币（USD）快照',
    `win_rate` INT NOT NULL DEFAULT 0 COMMENT '赢率 0-100',
    `expect_close_date` DATE DEFAULT NULL COMMENT '预计成交日',
    `competitor` VARCHAR(255) DEFAULT NULL COMMENT '竞争对手',
    `lost_reason` VARCHAR(255) DEFAULT NULL COMMENT '丢单原因（置 LOST 时必填）',
    `owner_id` VARCHAR(36) NOT NULL COMMENT '负责人 → t_user.id',
    `remark` VARCHAR(1024) DEFAULT NULL,
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` BIGINT NOT NULL DEFAULT 0 COMMENT '0 = 未删除；删除时写入毫秒时间戳',
    PRIMARY KEY (`id`),
    KEY `idx_customer` (`customer_id`),
    KEY `idx_owner_stage` (`owner_id`, `stage`),
    KEY `idx_stage` (`stage`),
    KEY `idx_expect_close` (`expect_close_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 商机';

-- 字典：商机阶段（extra.defaultWinRate 与代码里的 OpportunityStage 枚举保持一致）
INSERT INTO `t_dict_type` (`id`, `type_code`, `type_name`, `description`) VALUES
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f4001', 'opportunity_stage', '商机阶段', '销售漏斗阶段');

INSERT INTO `t_dict_item` (`id`, `type_code`, `item_code`, `item_name`, `sort_no`, `extra`) VALUES
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f5001', 'opportunity_stage', 'INITIAL_CONTACT', '初步接触', 1,
     '{"defaultWinRate":10}'),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f5002', 'opportunity_stage', 'QUALIFIED', '需求确认', 2,
     '{"defaultWinRate":30}'),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f5003', 'opportunity_stage', 'QUOTED', '已报价', 3,
     '{"defaultWinRate":50}'),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f5004', 'opportunity_stage', 'NEGOTIATION', '商务谈判', 4,
     '{"defaultWinRate":80}'),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f5005', 'opportunity_stage', 'WON', '赢单', 5,
     '{"defaultWinRate":100}'),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f5006', 'opportunity_stage', 'LOST', '丢单', 6,
     '{"defaultWinRate":0}');
