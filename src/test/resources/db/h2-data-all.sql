/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

-- 测试种子数据 —— 覆盖 9 张表。
--   - 沿用 V1 的 admin/admin123 + testuser/test123456，BCrypt 哈希来自 BCryptPasswordEncoderTests 已验证
--   - 2 个 role (ADMIN + USER)，12 个 permission，admin 拿到全部 12 个、testuser 只拿到 3 个 user:read
--   - 1 个 product（ProductController 测试用）
--   - 3 个 lottery period（statistics 测试覆盖区间用）
--   - 2 个 enabled provider + 1 个 disabled provider（OpenAI + Provider 测试用）
--   - 几条 llm_request_log（UsageController 聚合测试用）

-- permission 种子（12 条）
INSERT INTO t_permission (id, permission_name, permission_code, description) VALUES
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

-- role 种子（2 条）
INSERT INTO t_role (id, role_name, role_code, description) VALUES
    ('550e8400-e29b-41d4-a716-446655440101', 'Administrator', 'ADMIN', 'System administrator with full permissions'),
    ('550e8400-e29b-41d4-a716-446655440102', 'User', 'USER', 'Regular user');

-- role_permission 关联（admin 全部 12 条；user 只 3 个 read 权限）
INSERT INTO t_role_permission (id, role_id, permission_id) VALUES
    ('660e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440101', '550e8400-e29b-41d4-a716-446655440001'),
    ('660e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440101', '550e8400-e29b-41d4-a716-446655440002'),
    ('660e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440101', '550e8400-e29b-41d4-a716-446655440003'),
    ('660e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440101', '550e8400-e29b-41d4-a716-446655440004'),
    ('660e8400-e29b-41d4-a716-446655440005', '550e8400-e29b-41d4-a716-446655440101', '550e8400-e29b-41d4-a716-446655440005'),
    ('660e8400-e29b-41d4-a716-446655440006', '550e8400-e29b-41d4-a716-446655440101', '550e8400-e29b-41d4-a716-446655440006'),
    ('660e8400-e29b-41d4-a716-446655440007', '550e8400-e29b-41d4-a716-446655440101', '550e8400-e29b-41d4-a716-446655440007'),
    ('660e8400-e29b-41d4-a716-446655440008', '550e8400-e29b-41d4-a716-446655440101', '550e8400-e29b-41d4-a716-446655440008'),
    ('660e8400-e29b-41d4-a716-446655440009', '550e8400-e29b-41d4-a716-446655440101', '550e8400-e29b-41d4-a716-446655440009'),
    ('660e8400-e29b-41d4-a716-446655440010', '550e8400-e29b-41d4-a716-446655440101', '550e8400-e29b-41d4-a716-446655440010'),
    ('660e8400-e29b-41d4-a716-446655440011', '550e8400-e29b-41d4-a716-446655440101', '550e8400-e29b-41d4-a716-446655440011'),
    ('660e8400-e29b-41d4-a716-446655440012', '550e8400-e29b-41d4-a716-446655440101', '550e8400-e29b-41d4-a716-446655440012'),
    ('660e8400-e29b-41d4-a716-446655440013', '550e8400-e29b-41d4-a716-446655440102', '550e8400-e29b-41d4-a716-446655440002'),
    ('660e8400-e29b-41d4-a716-446655440014', '550e8400-e29b-41d4-a716-446655440102', '550e8400-e29b-41d4-a716-446655440006'),
    ('660e8400-e29b-41d4-a716-446655440015', '550e8400-e29b-41d4-a716-446655440102', '550e8400-e29b-41d4-a716-446655440010');

-- user 种子（admin + testuser，BCrypt 哈希来自 BCryptPasswordEncoderTests）
INSERT INTO t_user (id, username, password, real_name, status) VALUES
    ('550e8400-e29b-41d4-a716-446655440201', 'admin',
     '$2a$10$FtMOPk5bSAG2p6udDM1wy.hf0GCFGeX5hbRg74Bh6Z1fVQxfByCOi', 'Administrator', 1);

INSERT INTO t_user (id, username, password, real_name, email, phone, status) VALUES
    ('550e8400-e29b-41d4-a716-446655440202', 'testuser',
     '$2a$10$aV7SsUHGnfAyHN5dIzpCW.ljxQbHSHQKwBxV3UPeZ2LR0pweelWbK',
     'TestUser', 'testuser@example.com', '13900139000', 1);

-- user_role 关联（admin→ADMIN role；testuser→USER role）
INSERT INTO t_user_role (id, user_id, role_id) VALUES
    ('550e8400-e29b-41d4-a716-446655440301', '550e8400-e29b-41d4-a716-446655440201',
     '550e8400-e29b-41d4-a716-446655440101'),
    ('550e8400-e29b-41d4-a716-446655440302', '550e8400-e29b-41d4-a716-446655440202',
     '550e8400-e29b-41d4-a716-446655440102');

-- product 种子（1 条，ProductController 测试覆盖用）
INSERT INTO t_product (id, product_name, product_code, category, description, price, status) VALUES
    ('770e8400-e29b-41d4-a716-446655440001', '超级大乐透', 'DLT', '数字彩', '体彩大乐透', 2.00, 1);

-- lottery_period 种子（3 条，statistics 测试区间用）
INSERT INTO t_lottery_period (id, period, draw_date, front_1, front_2, front_3, front_4, front_5, back_1, back_2) VALUES
    ('880e8400-e29b-41d4-a716-446655440001', '25001', '2025-01-04',  5, 12, 23, 28, 34,  3, 10),
    ('880e8400-e29b-41d4-a716-446655440002', '25002', '2025-01-06',  7, 11, 19, 25, 33,  2,  9),
    ('880e8400-e29b-41d4-a716-446655440003', '25003', '2025-01-08',  1,  8, 15, 22, 31,  4, 11);

-- llm_provider 种子（3 条：openai + deepseek 启用，ollama 停用 —— OpenAI/Provider 测试用）
-- api_key_ciphertext 写 16 字节 dummy（H2 VARBINARY 列只要非空即可；本测试不验证解密）
INSERT INTO t_llm_provider (id, name, base_url, api_key_ciphertext, enabled, priority) VALUES
    ('990e8400-e29b-41d4-a716-446655440001', 'openai',   'https://api.openai.com',   CAST('DUMMYOPENAIKEY' AS VARBINARY),   1, 10),
    ('990e8400-e29b-41d4-a716-446655440002', 'deepseek', 'https://api.deepseek.com', CAST('DUMMYDEEPSEEKKEY' AS VARBINARY), 1, 20),
    ('990e8400-e29b-41d4-a716-446655440003', 'ollama',   'http://localhost:11434',   CAST('DUMMYOLLAMAKEY' AS VARBINARY),   0, 30);

-- llm_request_log 种子（4 条：admin 调了 openai+chat 与 openai+embeddings；testuser 调了 deepseek+chat）
INSERT INTO t_llm_request_log (id, user_id, provider, model, endpoint, prompt_tokens, completion_tokens, total_tokens, status_code, latency_ms) VALUES
    ('aa0e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440201', 'openai',   'openai:gpt-4o-mini',  'chat_completions', 100, 50, 150, 200, 1200),
    ('aa0e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440201', 'openai',   'openai:gpt-4o-mini',  'embeddings',       80,  0,  80, 200,  300),
    ('aa0e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440202', 'deepseek', 'deepseek:deepseek-chat', 'chat_completions', 200, 100, 300, 200, 2500),
    ('aa0e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440201', 'openai',   'openai:gpt-4o-mini',  'chat_completions',  50, 25,  75, 500,  600);