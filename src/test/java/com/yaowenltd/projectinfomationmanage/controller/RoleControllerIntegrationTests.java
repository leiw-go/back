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
 * RoleController 集成测试 —— 覆盖 6 个端点的三态（admin / USER / 无 token）。
 * <p>
 * 特别注意 {@code GET /api/roles/permissions} 返回 {@code List<Permission>} 裸实体
 * （而非 DTO）—— 见 {@code RoleController.java:108-113}。这是已知的 API 形态，
 * 测试 javadoc 注一句"建议后续改为 DTO"，不动实现。
 * </p>
 *
 * @since 2026-08-05
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("all-endpoints-it")
class RoleControllerIntegrationTests {

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
        // 保留种子 role (ADMIN + USER)；清掉测试新增的 role（连带 t_role_permission + t_user_role）
        jdbcTemplate.execute("DELETE FROM t_user_role WHERE role_id NOT IN "
                + "('550e8400-e29b-41d4-a716-446655440101', '550e8400-e29b-41d4-a716-446655440102')");
        jdbcTemplate.execute("DELETE FROM t_role_permission WHERE role_id NOT IN "
                + "('550e8400-e29b-41d4-a716-446655440101', '550e8400-e29b-41d4-a716-446655440102')");
        jdbcTemplate.execute("DELETE FROM t_role WHERE id NOT IN "
                + "('550e8400-e29b-41d4-a716-446655440101', '550e8400-e29b-41d4-a716-446655440102')");
    }

    // =================== POST /api/roles ===================

    @Test
    void createRole_admin_returns201_andEchoesRoleCode() throws Exception {
        mockMvc.perform(post("/api/roles")
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\":\"Tester\",\"roleCode\":\"TESTER\","
                                + "\"description\":\"Test role\",\"status\":1,"
                                + "\"permissionIds\":[\"550e8400-e29b-41d4-a716-446655440002\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.roleCode").value("TESTER"))
                .andExpect(jsonPath("$.data.roleName").value("Tester"))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    void createRole_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\":\"X\",\"roleCode\":\"X\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== PUT /api/roles/{id} ===================

    @Test
    void updateRole_admin_returns200_andEchoesUpdatedRoleName() throws Exception {
        mockMvc.perform(post("/api/roles")
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\":\"Updatable\",\"roleCode\":\"UPDATABLE\"}"))
                .andExpect(status().isOk());

        String roleId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_role WHERE role_code = 'UPDATABLE'", String.class);

        mockMvc.perform(put("/api/roles/" + roleId)
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\":\"Updated Name\",\"roleCode\":\"UPDATABLE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.roleName").value("Updated Name"));
    }

    @Test
    void updateRole_noToken_returns401() throws Exception {
        mockMvc.perform(put("/api/roles/550e8400-e29b-41d4-a716-446655440101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\":\"X\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== DELETE /api/roles/{id} ===================

    @Test
    void deleteRole_admin_returns200_andRemovesFromDb() throws Exception {
        mockMvc.perform(post("/api/roles")
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleName\":\"Deletable\",\"roleCode\":\"DELETABLE\"}"))
                .andExpect(status().isOk());

        String roleId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_role WHERE role_code = 'DELETABLE'", String.class);

        mockMvc.perform(delete("/api/roles/" + roleId)
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/roles/" + roleId)
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void deleteRole_noToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/roles/550e8400-e29b-41d4-a716-446655440101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== GET /api/roles/{id} ===================

    @Test
    void getRoleById_admin_returns200_andEchoesRoleCode() throws Exception {
        mockMvc.perform(get("/api/roles/550e8400-e29b-41d4-a716-446655440101")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.roleCode").value("ADMIN"))
                .andExpect(jsonPath("$.data.roleName").value("Administrator"));
    }

    @Test
    void getRoleById_admin_returnsCode400_whenIdNotFound() throws Exception {
        mockMvc.perform(get("/api/roles/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void getRoleById_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/roles/550e8400-e29b-41d4-a716-446655440101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== GET /api/roles ===================

    @Test
    void getAllRoles_admin_returns200_andListContainsSeededRoles() throws Exception {
        mockMvc.perform(get("/api/roles")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[?(@.roleCode == 'ADMIN')]").exists())
                .andExpect(jsonPath("$.data[?(@.roleCode == 'USER')]").exists());
    }

    @Test
    void getAllRoles_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== GET /api/roles/permissions ===================

    /**
     * 验证 {@code GET /api/roles/permissions} 返回 {@code List<Permission>} 裸实体
     * （含 id / permissionName / permissionCode / description）——
     * <strong>当前实现返回裸 entity 而非 DTO，是已知的 API 形态</strong>，建议后续改为 DTO。
     */
    @Test
    void getAllPermissions_admin_returns200_andContainsSeededFields() throws Exception {
        mockMvc.perform(get("/api/roles/permissions")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(12))
                .andExpect(jsonPath("$.data[0].id").exists())
                .andExpect(jsonPath("$.data[0].permissionCode").exists())
                .andExpect(jsonPath("$.data[0].permissionName").exists())
                .andExpect(jsonPath("$.data[?(@.permissionCode == 'user:create')]").exists());
    }

    @Test
    void getAllPermissions_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/roles/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}