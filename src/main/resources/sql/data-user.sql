-- Data initialization for normal test user
-- Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.

-- Insert a normal test user (password: test123456, BCrypt encrypted, skip if already exists)
INSERT IGNORE INTO t_user (id, username, password, real_name, email, phone, status) VALUES
('550e8400-e29b-41d4-a716-446655440202', 'testuser', '$2a$10$aV7SsUHGnfAyHN5dIzpCW.ljxQbHSHQKwBxV3UPeZ2LR0pweelWbK', 'TestUser', 'testuser@example.com', '13900139000', 1);

-- Assign USER role to test user (skip if already exists)
INSERT IGNORE INTO t_user_role (id, user_id, role_id) VALUES
('550e8400-e29b-41d4-a716-446655440302', '550e8400-e29b-41d4-a716-446655440202', '550e8400-e29b-41d4-a716-446655440102');