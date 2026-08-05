/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller.openai;

import com.yaowenltd.projectinfomationmanage.testsupport.JwtTestTokenFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ModelsController 集成测试 —— 覆盖 {@code GET /v1/models} 的三态。
 * <p>
 * <strong>响应形态：</strong>本端点<strong>不</strong>包 {@code ResponseResult}，直接返回
 * {@code ResponseEntity<ModelListResponse>} 裸 JSON。响应结构：{@code {object: "list", data: [...]}}
 * </p>
 * <p>
 * seed 数据：{@code h2-data-all.sql} 放了 3 个 provider（openai / deepseek 都 enabled=1，
 * ollama enabled=0）。{@code OpenAiRelayService.listModels} 走 {@code providerRepository.findAllEnabled()}
 * 只取 enabled 的 —— 应返回 2 条。
 * </p>
 *
 * @since 2026-08-05
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("all-endpoints-it")
class ModelsControllerIntegrationTests {

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
    private JwtTestTokenFactory jwtFactory;

    @Test
    void listModels_admin_returns200_andContainsSeededProviders() throws Exception {
        mockMvc.perform(get("/v1/models")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                // 响应是裸 JSON —— 没有 $.code 包装
                .andExpect(jsonPath("$.object").value("list"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[?(@.id == 'openai:*')]").exists())
                .andExpect(jsonPath("$.data[?(@.id == 'deepseek:*')]").exists())
                .andExpect(jsonPath("$.data[?(@.id == 'ollama:*')]").doesNotExist())
                .andExpect(jsonPath("$.data[?(@.ownedBy == 'openai')]").exists());
    }

    /**
     * USER 角色也能调 /v1/models（当前没 RBAC）。
     */
    @Test
    void listModels_userRole_returns200() throws Exception {
        mockMvc.perform(get("/v1/models")
                        .header("Authorization", jwtFactory.bearer("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object").value("list"))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void listModels_noToken_returns401() throws Exception {
        mockMvc.perform(get("/v1/models"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("no permission to call"));
    }
}