/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller.openai;

import com.yaowenltd.projectinfomationmanage.service.openai.OpenAiRelayService;
import com.yaowenltd.projectinfomationmanage.service.openai.dto.EmbeddingRequest;
import com.yaowenltd.projectinfomationmanage.testsupport.JwtTestTokenFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EmbeddingsController 集成测试 —— 覆盖 {@code POST /v1/embeddings} 的三态。
 * <p>
 * Mock 策略同 {@link ChatCompletionsControllerIntegrationTests} —— {@code @MockBean OpenAiRelayService}。
 * </p>
 *
 * @since 2026-08-05
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("all-endpoints-it")
class EmbeddingsControllerIntegrationTests {

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

    @MockBean
    private OpenAiRelayService relay;

    /**
     * Mock 一个最小可用的 OpenAI 风格 embeddings 响应 —— 走
     * {@code ResponseEntity.ok(Map.of(...))} 这种通用形态避免引入额外 DTO。
     */
    @SuppressWarnings("unchecked")
    private static ResponseEntity<Map<String, Object>> sampleEmbeddingResponse() {
        Map<String, Object> body = Map.of(
                "object", "list",
                "data", List.of(
                        Map.of(
                                "object", "embedding",
                                "index", 0,
                                "embedding", List.of(0.1, 0.2, 0.3, 0.4))),
                "model", "text-embedding-3-small",
                "usage", Map.of(
                        "prompt_tokens", 5,
                        "total_tokens", 5));
        return ResponseEntity.ok(body);
    }

    @Test
    void embeddings_admin_returns200_andPassesThroughUpstreamShape() throws Exception {
        doReturn(sampleEmbeddingResponse()).when(relay).embedding(any(EmbeddingRequest.class), anyString(), any());

        mockMvc.perform(post("/v1/embeddings")
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"model\":\"openai:text-embedding-3-small\","
                                + "\"input\":[\"hello world\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object").value("list"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].embedding").isArray())
                .andExpect(jsonPath("$.data[0].embedding[0]").value(0.1))
                .andExpect(jsonPath("$.data[0].embedding[3]").value(0.4))
                .andExpect(jsonPath("$.model").value("text-embedding-3-small"));

        verify(relay).embedding(any(EmbeddingRequest.class), anyString(), any());
    }

    @Test
    void embeddings_userRole_returns200() throws Exception {
        doReturn(sampleEmbeddingResponse()).when(relay).embedding(any(EmbeddingRequest.class), anyString(), any());

        mockMvc.perform(post("/v1/embeddings")
                        .header("Authorization", jwtFactory.bearer("testuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"model\":\"openai:text-embedding-3-small\","
                                + "\"input\":[\"hello\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.object").value("list"));
    }

    @Test
    void embeddings_noToken_returns401() throws Exception {
        mockMvc.perform(post("/v1/embeddings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"model\":\"openai:text-embedding-3-small\",\"input\":[\"hi\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}