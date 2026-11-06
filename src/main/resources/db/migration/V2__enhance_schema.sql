/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

-- V2 增强迁移：字符集归一 + update_time 自动更新 + 状态/时间维度二级索引。
--
-- 为什么需要单独的 V2：
--   V1 在"已有非空数据库"上会被 baseline-version=1 跳过，也就是说 V1 里写好的
--   ENGINE / CHARSET 等改进永远落不到存量库上。本迁移负责把存量库补齐到与
--   "空库跑完 V1+V2"完全一致的形态。
--
-- 全部语句均为幂等：
--   - 新空库   ：V1 建表 -> V2 执行（等价于存量库只跑 V2）
--   - 存量库   ：baseline 跳过 V1 -> V2 执行
--   两条路径最终 schema 一致。
--
-- 本迁移只做结构增强，不删改任何业务数据。

-- ---------------------------------------------------------------------------
-- 1. 字符集 / 排序规则归一
--
--    MySQL 的两个坑（都已实测）：
--      a) 不支持多表 CONVERT。`ALTER TABLE t1, t2 CONVERT TO ...` 和
--         `ALTER TABLE t1 CONVERT TO ..., t2 CONVERT TO ...` 都是语法错误(1064)，
--         必须一张表一条语句。
--      b) 逐表转换时若外键两端字符集不一致会报 3780
--         "Referencing column ... and referenced column ... are incompatible"。
--         所以先关掉外键检查，转完再打开。
--
--    FOREIGN_KEY_CHECKS 是会话级变量，Flyway 同一个迁移里的语句走同一连接，
--    因此这里的设置对本迁移有效。
-- ---------------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE `t_user` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `t_role` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `t_permission` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `t_user_role` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `t_role_permission` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `t_product` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE `t_lottery_period` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------------
-- 2. update_time 自动更新时间
--    AGENTS.md §6 的约定一直写着 "ON UPDATE CURRENT_TIMESTAMP"，但旧的 schema.sql
--    并没有落实。这里补上。t_permission / t_user_role / t_role_permission 无此列，不动。
-- ---------------------------------------------------------------------------
ALTER TABLE `t_user`
    MODIFY COLUMN `update_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE `t_role`
    MODIFY COLUMN `update_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE `t_product`
    MODIFY COLUMN `update_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE `t_lottery_period`
    MODIFY COLUMN `update_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- ---------------------------------------------------------------------------
-- 3. 二级索引
--    这些索引名在旧 schema 里没有同名索引，存量库上 ADD INDEX 不会撞名。
--    t_lottery_period 的 idx_draw_date / idx_period 旧库已有，不重复添加。
-- ---------------------------------------------------------------------------
ALTER TABLE `t_user`
    ADD INDEX `idx_status` (`status`),
    ADD INDEX `idx_create_time` (`create_time`);

ALTER TABLE `t_role`
    ADD INDEX `idx_status` (`status`);

ALTER TABLE `t_product`
    ADD INDEX `idx_status` (`status`),
    ADD INDEX `idx_category` (`category`);
