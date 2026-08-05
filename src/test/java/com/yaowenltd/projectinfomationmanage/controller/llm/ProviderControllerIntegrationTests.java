/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller.llm;

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
 * ProviderController 集成测试 —— 覆盖 4 个端点的 CRUD + api_key 字段脱敏 + enabled 过滤。
 * <p>
 * <strong>api_key 字段脱敏验证：</strong>{@code ProviderService.toMaskedDto} 调用
 * {@code dto.setApiKey(null)} 兜底 —— 响应里 {@code apiKey} 字段为 {@code null}。
 * {@code LlmProviderDto.apiKey} 字段本身<strong>没有</strong>{@code @JsonIgnore} 注解
 * （潜在缺陷 —— 应加注解或在 DTO 写 {@code @ToString.Exclude} 防止反射泄露）。
 * 测试断言响应里 {@code apiKey} 为 null（现状），并把这条缺陷 flag 在 javadoc。
 * </p>
 * <p>
 * <strong>权限现状：</strong>本项目没 RBAC —— USER 角色也能调所有管理类端点（assert 200）。
 * </p>
 *
 * @since 2026-08-05
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("all-endpoints-it")
class ProviderControllerIntegrationTests {

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
        // 还原 seed 3 个 provider：openai(depth 1) / deepseek(depth 1) / ollama(enabled=0)
        jdbcTemplate.execute("DELETE FROM t_llm_provider WHERE name NOT IN ('openai', 'deepseek', 'ollama')");
        jdbcTemplate.execute("UPDATE t_llm_provider SET enabled = 1 WHERE name IN ('openai', 'deepseek')");
        jdbcTemplate.execute("UPDATE t_llm_provider SET enabled = 0 WHERE name = 'ollama'");
    }

    // =================== GET /api/llm/providers ===================

    @Test
    void listProviders_admin_returns200_andContainsSeededProviders() throws Exception {
        mockMvc.perform(get("/api/llm/providers")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[?(@.name == 'openai')]").exists())
                .andExpect(jsonPath("$.data[?(@.name == 'deepseek')]").exists())
                .andExpect(jsonPath("$.data[?(@.name == 'ollama')]").exists());
    }

    /**
     * 验证 {@code LlmProviderDto.apiKey} 在响应里是 null（脱敏）。
     * <p>
     * 注意：DTO 字段本身没有 {@code @JsonIgnore}，是 {@code ProviderService.toMaskedDto}
     * 主动 {@code setApiKey(null)} 兜底 —— 任何忘记调 {@code toMaskedDto} 的路径都会泄露。
     * </p>
     */
    @Test
    void listProviders_admin_responseApiKeyFieldIsNull() throws Exception {
        mockMvc.perform(get("/api/llm/providers")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.apiKey != null)]").doesNotExist());
    }

    /**
     * enabled=0 的 provider 仍出现在 list 里（list 接口不过滤 enabled）.
     * <p>
     * 验证：list 返回所有 provider，不只 enabled 的。
     * </p>
     */
    @Test
    void listProviders_admin_includesDisabledProviders() throws Exception {
        mockMvc.perform(get("/api/llm/providers")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.name == 'ollama' && @.enabled == 0)]").exists());
    }

    @Test
    void listProviders_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/llm/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== POST /api/llm/providers ===================

    @Test
    void createProvider_admin_returns201_andEchoesName() throws Exception {
        mockMvc.perform(post("/api/llm/providers")
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"anthropic\",\"baseUrl\":\"https://api.anthropic.com\","
                                + "\"apiKey\":\"sk-ant-test-key\",\"enabled\":1,\"priority\":15}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.name").value("anthropic"))
                .andExpect(jsonPath("$.data.baseUrl").value("https://api.anthropic.com"))
                .andExpect(jsonPath("$.data.priority").value(15))
                // 响应里 apiKey 仍为 null（脱敏）
                .andExpect(jsonPath("$.data.apiKey").doesNotExist())
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    void createProvider_admin_returnsCode400_whenNameBlank() throws Exception {
        mockMvc.perform(post("/api/llm/providers")
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"baseUrl\":\"https://x.com\",\"apiKey\":\"key\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void createProvider_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/llm/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"baseUrl\":\"https://x.com\",\"apiKey\":\"key\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== PUT /api/llm/providers ===================

    @Test
    void updateProvider_admin_returns200_andEchoesUpdatedBaseUrl() throws Exception {
        // 取 openai 的 id
        String openaiId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_llm_provider WHERE name = 'openai'", String.class);

        mockMvc.perform(put("/api/llm/providers")
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + openaiId + "\","
                                + "\"name\":\"openai\",\"baseUrl\":\"https://api.openai.com/v2\","
                                + "\"enabled\":1,\"priority\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.baseUrl").value("https://api.openai.com/v2"))
                .andExpect(jsonPath("$.data.priority").value(5));
    }

    /**
     * 更新时传 enabled=0 → DB 更新为 0（验证 enabled 字段可写）。
     */
    @Test
    void updateProvider_admin_canDisableProvider() throws Exception {
        String openaiId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_llm_provider WHERE name = 'openai'", String.class);

        mockMvc.perform(put("/api/llm/providers")
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"" + openaiId + "\","
                                + "\"name\":\"openai\",\"baseUrl\":\"https://api.openai.com\","
                                + "\"enabled\":0,\"priority\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(0));
    }

    @Test
    void updateProvider_noToken_returns401() throws Exception {
        mockMvc.perform(put("/api/llm/providers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"x\",\"name\":\"x\",\"baseUrl\":\"x\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== DELETE /api/llm/providers/{id} ===================

    @Test
    void deleteProvider_admin_returns200_andRemovesFromDb() throws Exception {
        // 先创建一个临时 provider
        mockMvc.perform(post("/api/llm/providers")
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"todelete\",\"baseUrl\":\"https://x.com\",\"apiKey\":\"k\",\"enabled\":1}"))
                .andExpect(status().isOk());

        String providerId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_llm_provider WHERE name = 'todelete'", String.class);

        mockMvc.perform(delete("/api/llm/providers/" + providerId)
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_llm_provider WHERE id = ?", Integer.class, providerId);
        org.junit.jupiter.api.Assertions.assertEquals(0, count);
    }

    @Test
    void deleteProvider_admin_returnsCode400_whenIdNotFound() throws Exception {
        mockMvc.perform(delete("/api/llm/providers/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void deleteProvider_noToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/llm/providers/990e8400-e29b-41d4-a716-446655440001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}