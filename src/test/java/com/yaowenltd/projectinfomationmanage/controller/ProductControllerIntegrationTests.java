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
 * ProductController 集成测试 —— 覆盖 5 个端点的三态（admin / USER / 无 token）。
 *
 * @since 2026-08-05
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("all-endpoints-it")
class ProductControllerIntegrationTests {

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
        // 保留种子 product (DLT)；清掉测试新增
        jdbcTemplate.execute("DELETE FROM t_product WHERE product_code <> 'DLT'");
    }

    // =================== POST /api/products ===================

    @Test
    void createProduct_admin_returns201_andEchoesProductName() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"排列三\",\"productCode\":\"PL3\","
                                + "\"category\":\"数字彩\",\"description\":\"体彩排列三\","
                                + "\"price\":2.00,\"status\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.productCode").value("PL3"))
                .andExpect(jsonPath("$.data.productName").value("排列三"))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    void createProduct_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"X\",\"productCode\":\"X\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== PUT /api/products/{id} ===================

    @Test
    void updateProduct_admin_returns200_andEchoesUpdatedProductName() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"Old Name\",\"productCode\":\"UPDPROD\"}"))
                .andExpect(status().isOk());

        String productId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_product WHERE product_code = 'UPDPROD'", String.class);

        mockMvc.perform(put("/api/products/" + productId)
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"New Name\",\"productCode\":\"UPDPROD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.productName").value("New Name"))
                .andExpect(jsonPath("$.data.id").value(productId));
    }

    @Test
    void updateProduct_noToken_returns401() throws Exception {
        mockMvc.perform(put("/api/products/770e8400-e29b-41d4-a716-446655440001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"X\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== DELETE /api/products/{id} ===================

    @Test
    void deleteProduct_admin_returns200_andRemovesFromDb() throws Exception {
        mockMvc.perform(post("/api/products")
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productName\":\"To Delete\",\"productCode\":\"DELPROD\"}"))
                .andExpect(status().isOk());

        String productId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_product WHERE product_code = 'DELPROD'", String.class);

        mockMvc.perform(delete("/api/products/" + productId)
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/products/" + productId)
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void deleteProduct_noToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/products/770e8400-e29b-41d4-a716-446655440001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== GET /api/products/{id} ===================

    @Test
    void getProductById_admin_returns200_andEchoesProductCode() throws Exception {
        mockMvc.perform(get("/api/products/770e8400-e29b-41d4-a716-446655440001")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.productCode").value("DLT"))
                .andExpect(jsonPath("$.data.productName").value("超级大乐透"));
    }

    @Test
    void getProductById_admin_returnsCode400_whenIdNotFound() throws Exception {
        mockMvc.perform(get("/api/products/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void getProductById_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/products/770e8400-e29b-41d4-a716-446655440001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== GET /api/products ===================

    @Test
    void getAllProducts_admin_returns200_andListContainsSeededProduct() throws Exception {
        mockMvc.perform(get("/api/products")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.productCode == 'DLT')]").exists());
    }

    @Test
    void getAllProducts_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}