-- Initial data for ProjectInfomationManage
-- Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.

-- Insert default permissions (skip if already exists)
INSERT IGNORE INTO t_permission (id, permission_name, permission_code, description) VALUES
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

-- Insert default roles (skip if already exists)
INSERT IGNORE INTO t_role (id, role_name, role_code, description) VALUES
('550e8400-e29b-41d4-a716-446655440101', 'Administrator', 'ADMIN', 'System administrator with full permissions'),
('550e8400-e29b-41d4-a716-446655440102', 'User', 'USER', 'Regular user');

-- Assign all permissions to admin role (skip if already exists)
INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT UUID(), '550e8400-e29b-41d4-a716-446655440101', id FROM t_permission;

-- Assign read-only permissions to user role (skip if already exists)
INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT UUID(), '550e8400-e29b-41d4-a716-446655440102', id FROM t_permission
WHERE permission_code IN ('user:read', 'role:read', 'product:read');

-- Insert default admin user (password: admin123, BCrypt encrypted, skip if already exists)
INSERT IGNORE INTO t_user (id, username, password, real_name, status) VALUES
('550e8400-e29b-41d4-a716-446655440201', 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Administrator', 1);

-- Assign admin role to admin user (skip if already exists)
INSERT IGNORE INTO t_user_role (id, user_id, role_id) VALUES
('550e8400-e29b-41d4-a716-446655440301', '550e8400-e29b-41d4-a716-446655440201', '550e8400-e29b-41d4-a716-446655440101');