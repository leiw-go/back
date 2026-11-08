-- V2: CRM 客户域（客户 / 联系人 / 标签）+ CRM 权限点
-- 约定（见 docs/crm-backend-requirements.md §5.1）：
--   * MySQL 8 / utf8mb4_unicode_ci
--   * 不建物理外键，关联关系由 Service 层维护（逻辑 mapping）
--   * 主键 VARCHAR(36)，应用层生成 UUID
--   * 逻辑删除 deleted BIGINT（0 = 未删除，删除时写入毫秒时间戳）
--   * 业务唯一索引必须带 deleted 列，保证"删了还能再建"

CREATE TABLE `t_customer` (
    `id` VARCHAR(36) NOT NULL,
    `company_name` VARCHAR(128) NOT NULL COMMENT '公司全称（可外文）',
    `company_name_norm` VARCHAR(128) NOT NULL COMMENT '归一化名称（大写去空格），用于去重',
    `short_name` VARCHAR(64) DEFAULT NULL COMMENT '简称',
    `country_code` CHAR(2) NOT NULL COMMENT 'ISO 3166-1 alpha-2',
    `province` VARCHAR(64) DEFAULT NULL,
    `city` VARCHAR(64) DEFAULT NULL,
    `address` VARCHAR(255) DEFAULT NULL COMMENT '地址（可外文）',
    `website` VARCHAR(128) DEFAULT NULL,
    `industry` VARCHAR(64) DEFAULT NULL,
    `source` VARCHAR(32) NOT NULL COMMENT '来源渠道，字典 customer_source',
    `level` VARCHAR(8) NOT NULL DEFAULT 'B' COMMENT '客户等级 A/B/C，字典 customer_level',
    `customer_status` VARCHAR(16) NOT NULL DEFAULT 'POTENTIAL' COMMENT 'POTENTIAL/FOLLOWING/DEALED/LOST',
    `owner_id` VARCHAR(36) NOT NULL COMMENT '负责人 → t_user.id',
    `first_deal_at` DATETIME DEFAULT NULL,
    `last_contact_at` DATETIME DEFAULT NULL,
    `remark` VARCHAR(1024) DEFAULT NULL,
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` BIGINT NOT NULL DEFAULT 0 COMMENT '0 = 未删除；删除时写入毫秒时间戳',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_company_norm` (`country_code`, `company_name_norm`, `deleted`),
    KEY `idx_owner` (`owner_id`),
    KEY `idx_status` (`customer_status`),
    KEY `idx_country` (`country_code`),
    KEY `idx_level` (`level`),
    KEY `idx_source` (`source`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 客户';

CREATE TABLE `t_customer_contact` (
    `id` VARCHAR(36) NOT NULL,
    `customer_id` VARCHAR(36) NOT NULL COMMENT '所属客户 → t_customer.id',
    `name` VARCHAR(64) NOT NULL COMMENT '姓名（可外文）',
    `position` VARCHAR(64) DEFAULT NULL COMMENT '职位（可外文）',
    `email` VARCHAR(128) DEFAULT NULL,
    `phone` VARCHAR(20) DEFAULT NULL COMMENT 'E.164，如 +14155552671',
    `whatsapp` VARCHAR(20) DEFAULT NULL COMMENT 'E.164',
    `language` VARCHAR(16) DEFAULT NULL COMMENT '沟通语言标记，不驱动任何系统文案切换',
    `timezone` VARCHAR(48) DEFAULT NULL,
    `is_primary` TINYINT NOT NULL DEFAULT 0 COMMENT '1 = 主要联系人',
    `remark` VARCHAR(512) DEFAULT NULL,
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_customer_email` (`customer_id`, `email`, `deleted`),
    KEY `idx_customer` (`customer_id`),
    KEY `idx_email` (`email`),
    KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 客户联系人';

CREATE TABLE `t_customer_tag` (
    `id` VARCHAR(36) NOT NULL,
    `name` VARCHAR(64) NOT NULL,
    `color` VARCHAR(16) DEFAULT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    `owner_id` VARCHAR(36) NOT NULL,
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`, `deleted`),
    KEY `idx_owner` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 客户标签';

CREATE TABLE `t_customer_tag_rel` (
    `id` VARCHAR(36) NOT NULL,
    `customer_id` VARCHAR(36) NOT NULL,
    `tag_id` VARCHAR(36) NOT NULL,
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rel` (`customer_id`, `tag_id`),
    KEY `idx_tag` (`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 客户-标签关联（物理删除）';

CREATE TABLE `t_dict_type` (
    `id` VARCHAR(36) NOT NULL,
    `type_code` VARCHAR(32) NOT NULL COMMENT '字典类型编码',
    `type_name` VARCHAR(64) NOT NULL,
    `description` VARCHAR(255) DEFAULT NULL,
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 字典类型（物理删除）';

CREATE TABLE `t_dict_item` (
    `id` VARCHAR(36) NOT NULL,
    `type_code` VARCHAR(32) NOT NULL,
    `item_code` VARCHAR(32) NOT NULL,
    `item_name` VARCHAR(64) NOT NULL,
    `item_value` VARCHAR(255) DEFAULT NULL,
    `sort_no` INT NOT NULL DEFAULT 0,
    `extra` JSON DEFAULT NULL COMMENT '扩展属性，如阶段默认赢率',
    `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_item` (`type_code`, `item_code`),
    KEY `idx_sort` (`type_code`, `sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='CRM 字典项（物理删除）';

-- CRM 权限点：只追加一个 crm:access，授给已有角色
INSERT INTO `t_permission` (`id`, `permission_name`, `permission_code`, `description`) VALUES
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f0001', 'CRM Access', 'crm:access', '可调用 CRM 全部接口');

INSERT INTO `t_role_permission` (`id`, `role_id`, `permission_id`)
SELECT UUID(), r.`id`, p.`id`
FROM `t_role` r
         JOIN `t_permission` p ON p.`permission_code` = 'crm:access';

-- 字典：客户来源渠道 / 客户等级
INSERT INTO `t_dict_type` (`id`, `type_code`, `type_name`, `description`) VALUES
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f1001', 'customer_source', '客户来源渠道', '客户建档时的来源'),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f1002', 'customer_level', '客户等级', 'A / B / C');

INSERT INTO `t_dict_item` (`id`, `type_code`, `item_code`, `item_name`, `sort_no`) VALUES
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f2001', 'customer_source', 'WEBSITE', '网站', 1),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f2002', 'customer_source', 'TRADE_SHOW', '展会', 2),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f2003', 'customer_source', 'ALIBABA', '阿里', 3),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f2004', 'customer_source', 'EMAIL', '邮件', 4),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f2005', 'customer_source', 'REFERRAL', '转介绍', 5),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f2006', 'customer_source', 'OTHER', '其他', 6),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f2007', 'customer_level', 'A', 'A 类（重点）', 1),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f2008', 'customer_level', 'B', 'B 类（常规）', 2),
    ('7c1b6f2a-6c1e-4f0a-9d4e-0a5c9b2f2009', 'customer_level', 'C', 'C 类（观察）', 3);
