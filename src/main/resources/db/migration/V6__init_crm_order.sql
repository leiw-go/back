-- V6: CRM 订单域（订单 + 发货 + 应收）
-- 约定（见 docs/crm-backend-requirements.md §5.1）：
--   * MySQL 8 / utf8mb4_unicode_ci
--   * 不建物理外键，customer_id / order_id / quotation_id 由 Service 层校验存在
--   * 主键 VARCHAR(36)，应用层生成 UUID
--   * 逻辑删除 deleted BIGINT（0 = 未删除，删除时写入毫秒时间戳）
--   * 一单一币种：币种只在单头，明细不存币种

CREATE TABLE `t_order` (
    `id` VARCHAR(36) NOT NULL,
    `order_no` VARCHAR(64) NOT NULL COMMENT '订单号',
    `customer_id` VARCHAR(36) NOT NULL COMMENT '客户 → t_customer.id',
    `quotation_id` VARCHAR(36) DEFAULT NULL COMMENT '来源报价单 → t_quotation.id',
    `opportunity_id` VARCHAR(36) DEFAULT NULL COMMENT '来源商机 → t_opportunity.id',
    `currency_code` CHAR(3) NOT NULL DEFAULT 'USD' COMMENT '币种（一单一币种）',
    `total_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '订单金额（原币）',
    `total_amount_base` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '本位币（USD）快照',
    `incoterm` VARCHAR(16) DEFAULT NULL COMMENT '贸易术语',
    `payment_term` VARCHAR(24) DEFAULT NULL COMMENT '付款方式 TT/LC/DP/DA/OA/PAYPAL',
    `deposit_rate` INT DEFAULT NULL COMMENT '定金比例 %（决定应收拆分）',
    `order_status` VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED' COMMENT 'CONFIRMED/IN_PRODUCTION/SHIPPED/DELIVERED/COMPLETED/CANCELLED',
    `ship_date` DATE DEFAULT NULL COMMENT '发货日期',
    `expect_arrive_date` DATE DEFAULT NULL COMMENT '预计到港日',
    `owner_id` VARCHAR(36) NOT NULL COMMENT '负责人 → t_user.id',
    `remark` VARCHAR(1024) DEFAULT NULL,
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` BIGINT NOT NULL DEFAULT 0 COMMENT '0 = 未删除；删除时写入毫秒时间戳',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`, `deleted`),
    KEY `idx_customer` (`customer_id`),
    KEY `idx_owner_status` (`owner_id`, `order_status`),
    KEY `idx_ship_date` (`ship_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 订单';

CREATE TABLE `t_order_item` (
    `id` VARCHAR(36) NOT NULL,
    `order_id` VARCHAR(36) NOT NULL COMMENT '订单 → t_order.id',
    `product_id` VARCHAR(36) DEFAULT NULL COMMENT '产品 → t_crm_product.id',
    `product_name` VARCHAR(128) NOT NULL COMMENT '名称快照',
    `specification` VARCHAR(255) DEFAULT NULL COMMENT '规格快照',
    `quantity` DECIMAL(18,4) NOT NULL COMMENT '数量',
    `unit` VARCHAR(16) DEFAULT NULL COMMENT '单位快照',
    `unit_price` DECIMAL(18,6) NOT NULL COMMENT '单价（由报价单快照而来）',
    `amount` DECIMAL(18,2) NOT NULL COMMENT '行金额（由报价单快照而来）',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`),
    KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 订单明细';

CREATE TABLE `t_shipment` (
    `id` VARCHAR(36) NOT NULL,
    `order_id` VARCHAR(36) NOT NULL COMMENT '订单 → t_order.id',
    `shipping_no` VARCHAR(64) DEFAULT NULL COMMENT '运单 / 提单号',
    `shipping_type` VARCHAR(16) NOT NULL COMMENT 'SEA/AIR/EXPRESS/TRUCK',
    `departure_port` VARCHAR(64) DEFAULT NULL COMMENT '起运港',
    `destination_port` VARCHAR(64) DEFAULT NULL COMMENT '目的港',
    `ship_date` DATE DEFAULT NULL COMMENT '发货日期',
    `expect_arrive_date` DATE DEFAULT NULL COMMENT '预计到港日',
    `actual_arrive_date` DATE DEFAULT NULL COMMENT '实际到港日',
    `total_cbm` DECIMAL(12,3) DEFAULT NULL COMMENT '总体积 CBM',
    `total_weight` DECIMAL(14,3) DEFAULT NULL COMMENT '总重量 kg',
    `shipment_status` VARCHAR(16) NOT NULL DEFAULT 'PREPARING' COMMENT 'PREPARING/SHIPPED/ARRIVED',
    `remark` VARCHAR(512) DEFAULT NULL,
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`),
    KEY `idx_shipping_no` (`shipping_no`),
    KEY `idx_ship_date` (`ship_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 发货单';

CREATE TABLE `t_receivable` (
    `id` VARCHAR(36) NOT NULL,
    `order_id` VARCHAR(36) NOT NULL COMMENT '订单 → t_order.id',
    `currency_code` CHAR(3) NOT NULL DEFAULT 'USD' COMMENT '币种（继承订单）',
    `receivable_type` VARCHAR(16) NOT NULL DEFAULT 'BALANCE' COMMENT 'DEPOSIT 定金 / BALANCE 尾款 / FULL 全款',
    `receivable_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '应收总额',
    `received_amount` DECIMAL(18,2) NOT NULL DEFAULT 0 COMMENT '已收金额（累计值，无流水表）',
    `due_date` DATE DEFAULT NULL COMMENT '约定收款日',
    `receivable_status` VARCHAR(16) NOT NULL DEFAULT 'UNPAID' COMMENT 'UNPAID/PARTIAL/PAID/OVERDUE',
    `overdue_days` INT NOT NULL DEFAULT 0 COMMENT '逾期天数（查询时按 due_date 实时算）',
    `owner_id` VARCHAR(36) NOT NULL COMMENT '负责人 → t_user.id',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_order` (`order_id`),
    KEY `idx_owner_status` (`owner_id`, `receivable_status`),
    KEY `idx_due` (`due_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 应收';

-- 字典：贸易术语 / 付款方式
INSERT INTO `t_dict_type` (`id`, `type_code`, `type_name`, `description`) VALUES
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f6001', 'incoterm', '贸易术语', 'EXW / FOB / CFR / CIF / DDP'),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f6002', 'payment_term', '付款方式', 'TT / LC / DP / DA / OA / PAYPAL');

INSERT INTO `t_dict_item` (`id`, `type_code`, `item_code`, `item_name`, `sort_no`) VALUES
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f7001', 'incoterm', 'EXW', '工厂交货', 1),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f7002', 'incoterm', 'FOB', '装运港船上交货', 2),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f7003', 'incoterm', 'CFR', '成本加运费', 3),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f7004', 'incoterm', 'CIF', '成本保险加运费', 4),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f7005', 'incoterm', 'DDP', '完税后交货', 5),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f7006', 'payment_term', 'TT', '电汇', 1),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f7007', 'payment_term', 'LC', '信用证', 2),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f7008', 'payment_term', 'DP', '付款交单', 3),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f7009', 'payment_term', 'DA', '赊销', 4),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f700a', 'payment_term', 'OA', '记账赊销', 5),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f700b', 'payment_term', 'PAYPAL', 'PayPal', 6);
