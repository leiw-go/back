/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller;

import com.yaowenltd.projectinfomationmanage.testsupport.JwtTestTokenFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserController 集成测试 —— 覆盖 5 个端点的三态（admin / USER / 无 token）。
 * <p>
 * <strong>权限现状（截至 2026-08-05）：</strong>整个项目除 {@code @SkipAuth} 与
 * {@code UsageController} 的 hardcoded admin 判断外，没有 role-based 鉴权 —— 即
 * USER 角色也能调所有管理类端点返回 200。这是横切改造（应独立 spec），
 * 不在本轮 IT 任务范围；测试 javadoc 与 {@code AuthPermissionMatrixIntegrationTests}
 * 明文记录。
 * </p>
 * <p>
 * <strong>HTTP 状态码约定：</strong>所有包 {@code ResponseResult} 的端点 HTTP 状态恒为 200，
 * 业务码在 body.code。401 也走 HTTP 200 + body.code=401。
 * </p>
 *
 * @since 2026-08-05
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("all-endpoints-it")
class UserControllerIntegrationTests {

    private static final String JWT_SECRET_BASE64 = generateRandomSecret();

    private static String generateRandomSecret() {
        byte[] keyBytes = new byte[64];
        new SecureRandom().nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    @DynamicPropertySource
    static void registerJwtSecret(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> JWT_SECRET_BASE64);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtTestTokenFactory jwtFactory;

    @BeforeEach
    void resetState() {
        // 保留种子 admin / testuser；清掉本测试新增的用户
        jdbcTemplate.execute("DELETE FROM t_user_role WHERE user_id NOT IN "
                + "('550e8400-e29b-41d4-a716-446655440201', '550e8400-e29b-41d4-a716-446655440202')");
        jdbcTemplate.execute("DELETE FROM t_user WHERE id NOT IN "
                + "('550e8400-e29b-41d4-a716-446655440201', '550e8400-e29b-41d4-a716-446655440202')");
    }

    // =================== POST /api/users ===================

    @Test
    void createUser_admin_returns201_andEchoesUsername() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser1\",\"password\":\"pass1234\","
                                + "\"realName\":\"New User 1\",\"email\":\"u1@example.com\","
                                + "\"phone\":\"13800138001\",\"status\":1,"
                                + "\"roleIds\":[\"550e8400-e29b-41d4-a716-446655440102\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.username").value("newuser1"))
                .andExpect(jsonPath("$.data.realName").value("New User 1"))
                .andExpect(jsonPath("$.data.id").exists());
    }

    /**
     * 当前实现没 RBAC —— USER 也能创建用户（断言 200，javadoc 注明）。
     */
    @Test
    void createUser_userRole_returns200_noRbacYet() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", jwtFactory.bearer("testuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser2\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201));
    }

    @Test
    void createUser_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newuser3\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== PUT /api/users/{id} ===================

    @Test
    void updateUser_admin_returns200_andEchoesUpdatedRealName() throws Exception {
        // 先创建一个用户
        mockMvc.perform(post("/api/users")
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"updateable\",\"password\":\"pass1234\","
                                + "\"realName\":\"Original\"}"))
                .andExpect(status().isOk());

        // 查到它的 id
        String userId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_user WHERE username = 'updateable'", String.class);

        mockMvc.perform(put("/api/users/" + userId)
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"updateable\",\"password\":\"pass1234\","
                                + "\"realName\":\"Updated Name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.realName").value("Updated Name"))
                .andExpect(jsonPath("$.data.id").value(userId));
    }

    @Test
    void updateUser_noToken_returns401() throws Exception {
        mockMvc.perform(put("/api/users/550e8400-e29b-41d4-a716-446655440201")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"realName\":\"Hacked\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== DELETE /api/users/{id} ===================

    @Test
    void deleteUser_admin_returns200_andRemovesFromDb() throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"deletable\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk());

        String userId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_user WHERE username = 'deletable'", String.class);

        mockMvc.perform(delete("/api/users/" + userId)
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 二次 GET 应 400（service 抛 IllegalArgumentException → GlobalExceptionHandler）
        mockMvc.perform(get("/api/users/" + userId)
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void deleteUser_noToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/users/550e8400-e29b-41d4-a716-446655440201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== GET /api/users/{id} ===================

    @Test
    void getUserById_admin_returns200_andEchoesUsername() throws Exception {
        mockMvc.perform(get("/api/users/550e8400-e29b-41d4-a716-446655440201")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("550e8400-e29b-41d4-a716-446655440201"))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    @Test
    void getUserById_admin_returnsCode400_whenIdNotFound() throws Exception {
        mockMvc.perform(get("/api/users/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void getUserById_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users/550e8400-e29b-41d4-a716-446655440201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== GET /api/users ===================

    @Test
    void getAllUsers_admin_returns200_andListContainsSeededUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))   // seed: admin + testuser
                .andExpect(jsonPath("$.data[?(@.username == 'admin')]").exists())
                .andExpect(jsonPath("$.data[?(@.username == 'testuser')]").exists());
    }

    @Test
    void getAllUsers_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}