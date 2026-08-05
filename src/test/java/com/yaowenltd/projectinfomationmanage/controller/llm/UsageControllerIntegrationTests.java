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
import org.springframework.jdbc.core.JdbcTemplate;
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
 * UsageController 集成测试 —— 覆盖 admin / USER / groupBy 三条分支 + 无 token 401。
 * <p>
 * <strong>设计缺陷标注（×2）：</strong>
 * </p>
 * <ol>
 *   <li>{@code UsageController#isAdmin(String)} 硬编码 {@code "admin".equals(username)}
 *       （{@code UsageController.java:83-85}），标了 {@code TODO: 接入 RoleController 后替换为真实角色判断}。</li>
 *   <li>{@code UsageService#aggregate} 非 ADMIN 分支传 {@code currentUser}（JWT subject，是 username
 *       字符串）到 {@code t_llm_request_log.user_id} 过滤，但该列存的是 UUID —— <strong>真实非 ADMIN 用户
 *       永远查不到自己的用量</strong>。SQL {@code user_id = "testuser"} 匹配不到任何行。本测试固化这个
 *       bug 行为（断言 0 行），详见 {@code queryUsage_userRole_returnsEmpty_becauseUsernameIsNotUserId}。</li>
 * </ol>
 * <p>
 * 两个 bug 都已 flag 在 {@code docs/specs/2026-08-05-all-endpoints-integration-test.md} 的
 * 「需要你确认」节；本轮只测现状，不修。
 * </p>
 * <p>
 * seed 数据 ({@code h2-data-all.sql}) 在 {@code t_llm_request_log} 里放了 4 条
 * 聚合行：admin 调 openai+chat/embeddings 3 条、testuser 调 deepseek+chat 1 条。
 * </p>
 *
 * @since 2026-08-05
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("all-endpoints-it")
class UsageControllerIntegrationTests {

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
        // 还原 seed t_llm_request_log：清掉测试可能改动的内容后重新插入 4 条
        jdbcTemplate.execute("DELETE FROM t_llm_request_log");
        jdbcTemplate.update("INSERT INTO t_llm_request_log (id, user_id, provider, model, endpoint, "
                + "prompt_tokens, completion_tokens, total_tokens, status_code, latency_ms) VALUES "
                + "('aa0e8400-e29b-41d4-a716-446655440001', '550e8400-e29b-41d4-a716-446655440201', "
                + "'openai', 'openai:gpt-4o-mini', 'chat_completions', 100, 50, 150, 200, 1200), "
                + "('aa0e8400-e29b-41d4-a716-446655440002', '550e8400-e29b-41d4-a716-446655440201', "
                + "'openai', 'openai:gpt-4o-mini', 'embeddings', 80, 0, 80, 200, 300), "
                + "('aa0e8400-e29b-41d4-a716-446655440003', '550e8400-e29b-41d4-a716-446655440202', "
                + "'deepseek', 'deepseek:deepseek-chat', 'chat_completions', 200, 100, 300, 200, 2500), "
                + "('aa0e8400-e29b-41d4-a716-446655440004', '550e8400-e29b-41d4-a716-446655440201', "
                + "'openai', 'openai:gpt-4o-mini', 'chat_completions', 50, 25, 75, 500, 600)");
    }

    // =================== GET /api/llm/usage ===================

    /**
     * admin token + 无 userId → 200 + 聚合列表（按 user 分组）。
     * <p>
     * 验证 admin 走 hardcoded 分支看到全部行（含 testuser 的）。
     * </p>
     */
    @Test
    void queryUsage_admin_returns200_andAggregateListIncludesAllUsers() throws Exception {
        mockMvc.perform(get("/api/llm/usage")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                // 含 admin 和 testuser 两个 dimensionValue（按 user 分组）
                .andExpect(jsonPath("$.data[?(@.dimensionValue == '550e8400-e29b-41d4-a716-446655440201')]").exists())
                .andExpect(jsonPath("$.data[?(@.dimensionValue == '550e8400-e29b-41d4-a716-446655440202')]").exists())
                .andExpect(jsonPath("$.data[0].dimension").value("user"))
                .andExpect(jsonPath("$.data[0].totalCalls").exists())
                .andExpect(jsonPath("$.data[0].totalTokens").exists());
    }

    /**
     * USER token + 无 userId → 200 + 聚合列表为空（已知 BUG —— 见类 Javadoc）。
     * <p>
     * <strong>现状：</strong>{@code UsageService} 非 ADMIN 分支把 JWT subject（{@code "testuser"}）
     * 传给 SQL {@code user_id =}，但该列存 UUID。匹配不到任何行 → 返回空数组。
     * </p>
     */
    @Test
    void queryUsage_userRole_returnsEmpty_becauseUsernameIsNotUserId() throws Exception {
        mockMvc.perform(get("/api/llm/usage")
                        .header("Authorization", jwtFactory.bearer("testuser")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    /**
     * admin token + groupBy=model → 200 + 按 model 分组（应至少 2 条：openai 模型 + deepseek 模型）。
     */
    @Test
    void queryUsage_admin_groupByModel_returns200_andGroupsByModel() throws Exception {
        mockMvc.perform(get("/api/llm/usage?groupBy=model")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[?(@.dimension == 'model')]").exists())
                .andExpect(jsonPath("$.data[?(@.dimensionValue == 'openai:gpt-4o-mini')]").exists())
                .andExpect(jsonPath("$.data[?(@.dimensionValue == 'deepseek:deepseek-chat')]").exists());
    }

    /**
     * admin token + 指定 userId → 200 + 只返回该用户的聚合。
     */
    @Test
    void queryUsage_admin_withUserId_returns200_andFiltersByUserId() throws Exception {
        mockMvc.perform(get("/api/llm/usage?userId=550e8400-e29b-41d4-a716-446655440202")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].dimensionValue").value("550e8400-e29b-41d4-a716-446655440202"));
    }

    @Test
    void queryUsage_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/llm/usage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}