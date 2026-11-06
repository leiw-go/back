/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

-- V3 移除物理外键约束，统一改为应用层维护的"逻辑外键"。
--
-- 背景：
--   本项目不使用数据库层面的 FOREIGN KEY 约束。关联关系由应用层（MyBatis JOIN）
--   维护，引用完整性由 Service 层保证 —— 例如 UserServiceImpl#deleteUser 先清
--   t_user_role 再删 t_user，RoleServiceImpl#deleteRole 先清 t_user_role /
--   t_role_permission 再删 t_role。物理外键不仅冗余，还会因为 RESTRICT 语义
--   挡住本该成功的删除路径。
--
-- 为什么需要 V3：
--   V1 在"已有非空数据库"上会被 baseline-version=1 跳过，所以存量库上由旧
--   schema.sql 建的 FOREIGN KEY 约束仍然存在。本迁移负责把它们摘掉，
--   使存量库与"空库跑完 V1+V2+V3"收敛到同一形态。
--
-- 实现要点：
--   1. 约束名通过 information_schema 动态发现，不写死 —— MySQL 自动生成的
--      约束名（t_user_role_ibfk_1 等）依赖建表顺序，写死不可靠。
--   2. 用 PREPARE / EXECUTE 拼出单条 ALTER TABLE，一次删掉该表上的全部外键。
--      MySQL 的 PREPARE 不支持多语句，所以每张表一条。
--   3. 表上没有外键时（空库路径：V1 根本没建外键）生成 'SELECT 1'，
--      迁移就是空操作，不会报错 —— 保证两条路径都能跑通。
--
-- 注意：本迁移只删约束，不动任何业务数据。

-- ---------------------------------------------------------------------------
-- 1. t_user_role 上的外键（原先指向 t_user / t_role）
-- ---------------------------------------------------------------------------
SET @ddl = (
    SELECT CONCAT('ALTER TABLE `t_user_role` ',
                  GROUP_CONCAT(CONCAT('DROP FOREIGN KEY `', CONSTRAINT_NAME, '`') SEPARATOR ', '))
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_user_role'
);
SET @ddl = IFNULL(@ddl, 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 2. t_role_permission 上的外键（原先指向 t_role / t_permission）
-- ---------------------------------------------------------------------------
SET @ddl = (
    SELECT CONCAT('ALTER TABLE `t_role_permission` ',
                  GROUP_CONCAT(CONCAT('DROP FOREIGN KEY `', CONSTRAINT_NAME, '`') SEPARATOR ', '))
    FROM information_schema.REFERENTIAL_CONSTRAINTS
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 't_role_permission'
);
SET @ddl = IFNULL(@ddl, 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
