/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

-- V4 清理关联表中的历史重复行。
--
-- 背景：
--   Flyway 接入之前，项目用 `spring.sql.init.mode: always` 建表灌种子数据 ——
--   每次应用启动都会重跑一遍 seed 脚本。而当时的种子脚本里，
--   t_role_permission 用的是 `INSERT IGNORE ... SELECT UUID()`：UUID 每次都不同，
--   INSERT IGNORE 按 PK 去重失效，于是**每重启一次就多插一份完整的关联行**。
--   t_user_role 用的是固定 id，所以没有这个问题。
--
--   后果：ADMIN 角色的 12 条权限关联被复制成 N 份，
--   `PermissionMapper#findPermissionsByRoleId` 会把每条权限重复返回 N 次。
--
-- 本迁移：
--   对每个 (role_id, permission_id) / (user_id, role_id) 组合，只保留**最早插入**
--   的一行（按 create_time 排序，create_time 相同则按 id 排序），
--   其余的语义上完全冗余的行删掉。
--
-- 幂等：
--   数据已经干净时本迁移是空操作（不删任何行），可以重复执行。
--
-- 注意：只删"同一关联组合的多余副本"，不触碰任何用户 / 角色 / 权限主体数据。

-- ---------------------------------------------------------------------------
-- 1. t_role_permission：同一 (role_id, permission_id) 只保留最早的一行
-- ---------------------------------------------------------------------------
DELETE rp
FROM t_role_permission rp
         JOIN t_role_permission keep
              ON rp.role_id = keep.role_id
                  AND rp.permission_id = keep.permission_id
                  AND (rp.create_time > keep.create_time
                       OR (rp.create_time = keep.create_time AND rp.id > keep.id));

-- ---------------------------------------------------------------------------
-- 2. t_user_role：同一 (user_id, role_id) 只保留最早的一行
--    当前写入路径用的是固定 id，本表通常没有重复；这里一并兜底。
-- ---------------------------------------------------------------------------
DELETE ur
FROM t_user_role ur
         JOIN t_user_role keep
              ON ur.user_id = keep.user_id
                  AND ur.role_id = keep.role_id
                  AND (ur.create_time > keep.create_time
                       OR (ur.create_time = keep.create_time AND ur.id > keep.id));
