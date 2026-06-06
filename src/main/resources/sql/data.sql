-- Initial data for ProjectInfomationManage
-- Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.

-- Insert default permissions (skip if already exists)
INSERT IGNORE INTO t_permission (id, permission_name, permission_code, description) VALUES
('p001', 'User Create', 'user:create', 'Create user'),
('p002', 'User Read', 'user:read', 'Read user information'),
('p003', 'User Update', 'user:update', 'Update user information'),
('p004', 'User Delete', 'user:delete', 'Delete user'),
('p005', 'Role Create', 'role:create', 'Create role'),
('p006', 'Role Read', 'role:read', 'Read role information'),
('p007', 'Role Update', 'role:update', 'Update role information'),
('p008', 'Role Delete', 'role:delete', 'Delete role'),
('p009', 'Product Create', 'product:create', 'Create product'),
('p010', 'Product Read', 'product:read', 'Read product information'),
('p011', 'Product Update', 'product:update', 'Update product information'),
('p012', 'Product Delete', 'product:delete', 'Delete product');

-- Insert default roles (skip if already exists)
INSERT IGNORE INTO t_role (id, role_name, role_code, description) VALUES
('r001', 'Administrator', 'ADMIN', 'System administrator with full permissions'),
('r002', 'User', 'USER', 'Regular user');

-- Assign all permissions to admin role (skip if already exists)
INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT CONCAT('rp', ROW_NUMBER() OVER()) AS id, 'r001' AS role_id, id AS permission_id FROM t_permission;

-- Assign read-only permissions to user role (skip if already exists)
INSERT IGNORE INTO t_role_permission (id, role_id, permission_id)
SELECT CONCAT('rpu', ROW_NUMBER() OVER()) AS id, 'r002', id FROM t_permission
WHERE permission_code IN ('user:read', 'role:read', 'product:read');

-- Insert default admin user (password: admin123, BCrypt encrypted, skip if already exists)
INSERT IGNORE INTO t_user (id, username, password, real_name, status) VALUES
('u001', 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Administrator', 1);

-- Assign admin role to admin user (skip if already exists)
INSERT IGNORE INTO t_user_role (id, user_id, role_id) VALUES
('ur001', 'u001', 'r001');
