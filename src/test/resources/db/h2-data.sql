-- 集成测试种子数据。
-- 仅初始化 AuthServiceImpl#register 流程会用到的默认 USER 角色（UUID 与源码中
-- AuthServiceImpl.DEFAULT_USER_ROLE_ID 严格保持一致，避免测试绕开生产常量）。
-- admin / testuser 等业务用户由各测试方法通过 /api/auth/register 真实落库，
-- 不在此处种子，保证测试覆盖到 register 的完整链路。

INSERT INTO t_role (id, role_name, role_code, description, status)
VALUES ('550e8400-e29b-41d4-a716-446655440102', 'User', 'USER', 'Regular user', 1);
