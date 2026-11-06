/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

-- project_info_manage V1 基线。
-- 已有非空数据库通过 baseline-version=1 跳过本文件；新空库执行本文件完成初始化。
--
-- 表结构约定：**不建物理外键约束**，关联关系一律由应用层（MyBatis JOIN）维护，
-- 即"逻辑外键"。引用完整性由 Service 层保证（删除用户/角色时先清关联表）。
-- 关联列上仍显式建索引（KEY user_id / role_id / permission_id），供逻辑 JOIN 使用。
--
-- 注意：本文件一旦在某个库上执行过就不可再修改（Flyway 校验和会不匹配导致启动失败）。
--       改表结构请新增递增版本的迁移（V2__xxx.sql、V3__xxx.sql...）。

CREATE TABLE `t_user` (
    `id` VARCHAR(36) NOT NULL,
    `username` VARCHAR(64) NOT NULL,
    `password` VARCHAR(256) NOT NULL,
    `real_name` VARCHAR(64) DEFAULT NULL,
    `email` VARCHAR(128) DEFAULT NULL,
    `phone` VARCHAR(20) DEFAULT NULL,
    `status` TINYINT DEFAULT '1' COMMENT '1: active, 0: inactive',
    `create_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `t_role` (
    `id` VARCHAR(36) NOT NULL,
    `role_name` VARCHAR(64) NOT NULL,
    `role_code` VARCHAR(64) NOT NULL,
    `description` VARCHAR(256) DEFAULT NULL,
    `status` TINYINT DEFAULT '1' COMMENT '1: active, 0: inactive',
    `create_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `role_name` (`role_name`),
    UNIQUE KEY `role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `t_permission` (
    `id` VARCHAR(36) NOT NULL,
    `permission_name` VARCHAR(64) NOT NULL,
    `permission_code` VARCHAR(64) NOT NULL,
    `description` VARCHAR(256) DEFAULT NULL,
    `create_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `t_product` (
    `id` VARCHAR(36) NOT NULL,
    `product_name` VARCHAR(128) NOT NULL,
    `product_code` VARCHAR(64) NOT NULL,
    `category` VARCHAR(64) DEFAULT NULL,
    `description` VARCHAR(512) DEFAULT NULL,
    `price` DECIMAL(10,2) DEFAULT NULL,
    `status` TINYINT DEFAULT '1' COMMENT '1: active, 0: inactive',
    `create_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `product_code` (`product_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `t_lottery_period` (
    `id` VARCHAR(36) NOT NULL,
    `period` VARCHAR(20) NOT NULL COMMENT '期数',
    `draw_date` DATE NOT NULL COMMENT '开奖日期',
    `front_1` INT NOT NULL COMMENT '前区一号',
    `front_2` INT NOT NULL COMMENT '前区二号',
    `front_3` INT NOT NULL COMMENT '前区三号',
    `front_4` INT NOT NULL COMMENT '前区四号',
    `front_5` INT NOT NULL COMMENT '前区五号',
    `back_1` INT NOT NULL COMMENT '后区一号',
    `back_2` INT NOT NULL COMMENT '后区二号',
    `create_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `period` (`period`),
    KEY `idx_draw_date` (`draw_date`),
    KEY `idx_period` (`period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `t_user_role` (
    `id` VARCHAR(36) NOT NULL,
    `user_id` VARCHAR(36) NOT NULL,
    `role_id` VARCHAR(36) NOT NULL,
    `create_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `user_id` (`user_id`),
    KEY `role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `t_role_permission` (
    `id` VARCHAR(36) NOT NULL,
    `role_id` VARCHAR(36) NOT NULL,
    `permission_id` VARCHAR(36) NOT NULL,
    `create_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `role_id` (`role_id`),
    KEY `permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO `t_permission` (`id`, `permission_name`, `permission_code`, `description`) VALUES
    ('550e8400-e29b-41d4-a716-446655440001', 'User Create', 'user:create', 'Create user'),
    ('550e8400-e29b-41d4-a716-446655440002', 'User Read', 'user:read', 'Read user information'),
    ('550e8400-e29b-41d4-a716-446655440003', 'User Update', 'user:update', 'Update user information'),
    ('550e8400-e29b-41d4-a716-446655440004', 'User Delete', 'user:delete', 'Delete user'),
    ('550e8400-e29b-41d4-a716-446655440005', 'Role Create', 'role:create', 'Create role'),
    ('550e8400-e29b-41d4-a716-446655440006', 'Role Read', 'role:read', 'Read role information'),
    ('550e8400-e29b-41d4-a716-446655440007', 'Role Update', 'role:update', 'Update role information'),
    ('550e8400-e29b-41d4-a716-446655440008', 'Role Delete', 'role:delete', 'Delete role'),
    ('550e8400-e29b-41d4-a716-446655440009', 'Product Create', 'product:create', 'Create product'),
    ('550e8400-e29b-41d4-a716-446655440010', 'Product Read', 'product:read', 'Read product information'),
    ('550e8400-e29b-41d4-a716-446655440011', 'Product Update', 'product:update', 'Update product information'),
    ('550e8400-e29b-41d4-a716-446655440012', 'Product Delete', 'product:delete', 'Delete product');

INSERT INTO `t_role` (`id`, `role_name`, `role_code`, `description`) VALUES
    ('550e8400-e29b-41d4-a716-446655440101', 'Administrator', 'ADMIN', 'System administrator with full permissions'),
    ('550e8400-e29b-41d4-a716-446655440102', 'User', 'USER', 'Regular user');

INSERT INTO `t_role_permission` (`id`, `role_id`, `permission_id`)
SELECT UUID(), '550e8400-e29b-41d4-a716-446655440101', `id`
FROM `t_permission`;

INSERT INTO `t_role_permission` (`id`, `role_id`, `permission_id`)
SELECT UUID(), '550e8400-e29b-41d4-a716-446655440102', `id`
FROM `t_permission`
WHERE `permission_code` IN ('user:read', 'role:read', 'product:read');

INSERT INTO `t_user` (`id`, `username`, `password`, `real_name`, `status`) VALUES
    ('550e8400-e29b-41d4-a716-446655440201', 'admin',
     '$2a$10$FtMOPk5bSAG2p6udDM1wy.hf0GCFGeX5hbRg74Bh6Z1fVQxfByCOi', 'Administrator', 1);

INSERT INTO `t_user_role` (`id`, `user_id`, `role_id`) VALUES
    ('550e8400-e29b-41d4-a716-446655440301', '550e8400-e29b-41d4-a716-446655440201',
     '550e8400-e29b-41d4-a716-446655440101');

INSERT INTO `t_user` (`id`, `username`, `password`, `real_name`, `email`, `phone`, `status`) VALUES
    ('550e8400-e29b-41d4-a716-446655440202', 'testuser',
     '$2a$10$aV7SsUHGnfAyHN5dIzpCW.ljxQbHSHQKwBxV3UPeZ2LR0pweelWbK',
     'TestUser', 'testuser@example.com', '13900139000', 1);

INSERT INTO `t_user_role` (`id`, `user_id`, `role_id`) VALUES
    ('550e8400-e29b-41d4-a716-446655440302', '550e8400-e29b-41d4-a716-446655440202',
     '550e8400-e29b-41d4-a716-446655440102');
