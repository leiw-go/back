-- V3: CRM 产品域（产品目录 + 多币种参考价）
-- 命名说明：V1 已占用 `t_product`（彩票产品表），CRM 产品表改用 `t_crm_product`
-- 前缀，避免动到存量表；详见 docs/crm-backend-requirements.md §5.3.5。
--
-- 约定（见 docs/crm-backend-requirements.md §5.1）：
--   * MySQL 8 / utf8mb4_unicode_ci
--   * 不建物理外键，关联关系由 Service 层维护（逻辑 mapping）
--   * 主键 VARCHAR(36)，应用层生成 UUID
--   * 逻辑删除 deleted BIGINT（0 = 未删除，删除时写入毫秒时间戳）
--   * 业务唯一索引必须带 deleted 列，保证"删了还能再建"

CREATE TABLE `t_crm_product` (
    `id` VARCHAR(36) NOT NULL,
    `product_code` VARCHAR(64) NOT NULL COMMENT '产品编码',
    `product_name` VARCHAR(128) NOT NULL COMMENT '产品名称',
    `category` VARCHAR(64) DEFAULT NULL COMMENT '分类，字典 product_category',
    `specification` VARCHAR(255) DEFAULT NULL COMMENT '规格描述',
    `unit` VARCHAR(16) NOT NULL DEFAULT 'PCS' COMMENT '单位 PCS/KG/CBM/SET',
    `weight_kg` DECIMAL(12,4) DEFAULT NULL COMMENT '单件重量（kg），用于运费估算',
    `volume_cbm` DECIMAL(12,6) DEFAULT NULL COMMENT '单件体积（CBM），用于装柜量估算',
    `cost_price` DECIMAL(18,6) DEFAULT NULL COMMENT '成本单价（币种见 cost_currency）；为空则不能参与报价',
    `cost_currency` CHAR(3) NOT NULL DEFAULT 'USD' COMMENT '成本价币种；报价时必须与报价单币种相同',
    `default_price` DECIMAL(18,6) DEFAULT NULL COMMENT '默认销售价（参考）',
    `default_currency` CHAR(3) NOT NULL DEFAULT 'USD' COMMENT '默认币种（参考）',
    `moq` INT DEFAULT NULL COMMENT '起订量；报价明细数量必须 >= 此值',
    `hs_code` VARCHAR(32) DEFAULT NULL COMMENT 'HS 海关编码',
    `origin` VARCHAR(64) DEFAULT NULL COMMENT '产地',
    `product_status` TINYINT NOT NULL DEFAULT 1 COMMENT '1 在售 0 停售',
    `remark` VARCHAR(512) DEFAULT NULL,
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` BIGINT NOT NULL DEFAULT 0 COMMENT '0 = 未删除；删除时写入毫秒时间戳',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_code` (`product_code`, `deleted`),
    KEY `idx_category` (`category`),
    KEY `idx_status` (`product_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 产品目录';

-- 多币种参考价：仅用于目录展示与询盘目标价参考，**不是报价取价来源**（报价单价由成本价 + 加价率算出）。
-- 物理删除：参考价不被任何其他表引用（报价只做成本单价快照），且"整批替换"语义下
-- 软删会因同一批次 deleted 时间戳相同而撞唯一索引，故此处用物理删除。
CREATE TABLE `t_crm_product_price` (
    `id` VARCHAR(36) NOT NULL,
    `product_id` VARCHAR(36) NOT NULL COMMENT '产品 → t_crm_product.id',
    `currency_code` CHAR(3) NOT NULL COMMENT '币种',
    `price` DECIMAL(18,6) NOT NULL COMMENT '参考销售单价',
    `min_quantity` INT NOT NULL DEFAULT 0 COMMENT '阶梯起点',
    `valid_from` DATE DEFAULT NULL COMMENT '生效日',
    `valid_to` DATE DEFAULT NULL COMMENT '失效日',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_currency` (`product_id`, `currency_code`, `min_quantity`, `deleted`),
    KEY `idx_valid` (`valid_to`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 产品多币种参考价';

-- 字典：产品分类（具体分类项按业务实际维护，此处只建字典类型）
INSERT INTO `t_dict_type` (`id`, `type_code`, `type_name`, `description`) VALUES
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f3001', 'product_category', '产品分类', '产品目录分类，按业务实际维护');
