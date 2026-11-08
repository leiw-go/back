-- V5: CRM 询盘与报价域（核心交易链路）
-- 约定（见 docs/crm-backend-requirements.md §5.1）：
--   * MySQL 8 / utf8mb4_unicode_ci
--   * 不建物理外键，customer_id / inquiry_id / product_id 由 Service 层校验存在
--   * 主键 VARCHAR(36)，应用层生成 UUID
--   * 逻辑删除 deleted BIGINT（0 = 未删除，删除时写入毫秒时间戳）
--   * 金额：单头金额与利润由服务端算（§3.5.1），明细行不存币种（一单一币种）

CREATE TABLE `t_inquiry` (
    `id` VARCHAR(36) NOT NULL,
    `customer_id` VARCHAR(36) NOT NULL COMMENT '客户 → t_customer.id',
    `contact_id` VARCHAR(36) DEFAULT NULL COMMENT '联系人 → t_customer_contact.id',
    `inquiry_no` VARCHAR(64) NOT NULL COMMENT '询盘编号',
    `source` VARCHAR(32) NOT NULL COMMENT '来源渠道，字典 customer_source',
    `subject` VARCHAR(255) DEFAULT NULL COMMENT '主题',
    `target_price` DECIMAL(18,6) DEFAULT NULL COMMENT '客户目标单价（仅参考，不参与定价）',
    `currency_code` CHAR(3) NOT NULL DEFAULT 'USD' COMMENT '币种',
    `quantity` DECIMAL(18,4) DEFAULT NULL COMMENT '数量',
    `unit` VARCHAR(16) DEFAULT NULL COMMENT '单位',
    `expect_delivery_date` DATE DEFAULT NULL COMMENT '期望交期',
    `inquiry_status` VARCHAR(16) NOT NULL DEFAULT 'NEW' COMMENT 'NEW/PROCESSING/QUOTED/WON/LOST',
    `content` TEXT DEFAULT NULL COMMENT '原始需求内容（可外文）',
    `owner_id` VARCHAR(36) NOT NULL COMMENT '负责人 → t_user.id',
    `first_reply_at` DATETIME DEFAULT NULL COMMENT '首次回复时间（报价单创建时回填）',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` BIGINT NOT NULL DEFAULT 0 COMMENT '0 = 未删除；删除时写入毫秒时间戳',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_inquiry_no` (`inquiry_no`, `deleted`),
    KEY `idx_customer` (`customer_id`),
    KEY `idx_owner_status` (`owner_id`, `inquiry_status`),
    KEY `idx_source` (`source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 询盘';

CREATE TABLE `t_quotation` (
    `id` VARCHAR(36) NOT NULL,
    `quotation_no` VARCHAR(64) NOT NULL COMMENT '报价单号',
    `customer_id` VARCHAR(36) NOT NULL COMMENT '客户 → t_customer.id',
    `inquiry_id` VARCHAR(36) DEFAULT NULL COMMENT '来源询盘 → t_inquiry.id',
    `opportunity_id` VARCHAR(36) DEFAULT NULL COMMENT '关联商机 → t_opportunity.id',
    `version_no` INT NOT NULL DEFAULT 1 COMMENT '版本号（同一询盘 + 币种内递增）',
    `currency_code` CHAR(3) NOT NULL DEFAULT 'USD' COMMENT '币种（一单一币种）',
    `incoterm` VARCHAR(16) NOT NULL COMMENT '贸易术语 EXW/FOB/CFR/CIF/DDP',
    `markup_rate` DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '加价率 %（单头默认，明细可覆盖），0~1000',
    `discount_rate` DECIMAL(6,2) NOT NULL DEFAULT 0 COMMENT '折扣率 %（单头默认，明细可覆盖），0~100',
    `total_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '报价总额（原币，服务端算）',
    `total_cost` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '成本汇总（原币）',
    `profit_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '毛利额',
    `profit_rate` DECIMAL(8,2) NOT NULL DEFAULT 0 COMMENT '毛利率 %',
    `total_amount_base` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '本位币（USD）快照',
    `valid_until` DATE DEFAULT NULL COMMENT '有效期至',
    `quotation_status` VARCHAR(16) NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/SENT/CONFIRMED/REJECTED/EXPIRED',
    `reject_reason` VARCHAR(255) DEFAULT NULL COMMENT '拒绝原因（转 REJECTED 时必填）',
    `owner_id` VARCHAR(36) NOT NULL COMMENT '负责人 → t_user.id',
    `remark` VARCHAR(512) DEFAULT NULL,
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` BIGINT NOT NULL DEFAULT 0 COMMENT '0 = 未删除；删除时写入毫秒时间戳',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_quotation_no` (`quotation_no`, `deleted`),
    UNIQUE KEY `uk_inquiry_version` (`inquiry_id`, `currency_code`, `version_no`, `deleted`),
    KEY `idx_customer` (`customer_id`),
    KEY `idx_status` (`quotation_status`),
    KEY `idx_owner` (`owner_id`),
    KEY `idx_valid_until` (`valid_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 报价单';

CREATE TABLE `t_quotation_item` (
    `id` VARCHAR(36) NOT NULL,
    `quotation_id` VARCHAR(36) NOT NULL COMMENT '报价单 → t_quotation.id',
    `product_id` VARCHAR(36) DEFAULT NULL COMMENT '产品 → t_crm_product.id',
    `product_name` VARCHAR(128) NOT NULL COMMENT '名称快照',
    `specification` VARCHAR(255) DEFAULT NULL COMMENT '规格快照',
    `quantity` DECIMAL(18,4) NOT NULL COMMENT '数量（必须 >= 产品 MOQ）',
    `unit` VARCHAR(16) DEFAULT NULL COMMENT '单位快照',
    `cost_price` DECIMAL(18,6) NOT NULL COMMENT '成本单价快照（建明细时取自产品）',
    `markup_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '行覆盖加价率 %（空则用单头）',
    `discount_rate` DECIMAL(6,2) DEFAULT NULL COMMENT '行覆盖折扣率 %（空则用单头）',
    `unit_price` DECIMAL(18,6) NOT NULL COMMENT '单价（服务端算）',
    `amount` DECIMAL(18,2) NOT NULL COMMENT '行金额（服务端算）',
    `manual_price` TINYINT NOT NULL DEFAULT 0 COMMENT '1 = 手工改过单价，不随加价率/折扣重算',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_quotation` (`quotation_id`),
    KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 报价明细';
