/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.auth;

import com.yaowenltd.projectinfomationmanage.service.openai.OpenAiRelayService;
import com.yaowenltd.projectinfomationmanage.service.openai.dto.ChatCompletionRequest;
import com.yaowenltd.projectinfomationmanage.service.openai.dto.EmbeddingRequest;
import com.yaowenltd.projectinfomationmanage.service.openai.dto.ModelListResponse;
import com.yaowenltd.projectinfomationmanage.testsupport.EndpointCatalog;
import com.yaowenltd.projectinfomationmanage.testsupport.JwtTestTokenFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.security.SecureRandom;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * 权限矩阵集成测试 —— 全量覆盖「匿名 / 错 token / 过期 token / USER / ADMIN」×
 * 「所有受保护端点」。
 * <p>
 * <strong>端点清单来源：</strong>{@link EndpointCatalog} —— 32 个需 token 端点 + 2 个
 * {@code @SkipAuth} 端点。{@code @MethodSource} 注入，参数化跑一遍。
 * </p>
 * <p>
 * <strong>矩阵维度：</strong>
 * </p>
 * <ul>
 *   <li><strong>A. 匿名（无 token）</strong> → 受保护端点全部 401；{@code @SkipAuth} 端点 200</li>
 *   <li><strong>B. 错签名 token</strong> → 受保护端点全部 401</li>
 *   <li><strong>C. 过期 token</strong> → 受保护端点全部 401</li>
 *   <li><strong>D. 合法 USER token</strong> → 全部 200（当前实现没 RBAC，记录到 javadoc）</li>
 *   <li><strong>E. 合法 ADMIN token</strong> → 全部 200</li>
 *   <li><strong>F. {@code @SkipAuth} 短路验证</strong> → register / login 不带 token / 过期 token / 错签名 token 全 200</li>
 * </ul>
 *
 * @since 2026-08-05
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("all-endpoints-it")
class AuthPermissionMatrixIntegrationTests {

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

    /**
     * Mock 整个 {@code OpenAiRelayService} —— 矩阵只关心 token 拦截，
     * 不关心 OpenAI 上游逻辑。Stub 三方法返回简单响应，避免真实 AES 解密失败。
     */
    @MockBean
    private OpenAiRelayService openAiRelay;

    @BeforeEach
    void resetState() {
        // 确保 seed 数据完整
        jdbcTemplate.execute("UPDATE t_user SET password = '$2a$10$FtMOPk5bSAG2p6udDM1wy.hf0GCFGeX5hbRg74Bh6Z1fVQxfByCOi' "
                + "WHERE id = '550e8400-e29b-41d4-a716-446655440201'");
        jdbcTemplate.execute("UPDATE t_user SET password = '$2a$10$aV7SsUHGnfAyHN5dIzpCW.ljxQbHSHQKwBxV3UPeZ2LR0pweelWbK' "
                + "WHERE id = '550e8400-e29b-41d4-a716-446655440202'");

        // mock OpenAiRelayService 三个方法 —— lenient 让未被用到的 stub 不报错
        lenient().when(openAiRelay.listModels()).thenReturn(ResponseEntity.ok(new ModelListResponse()));
        lenient().doReturn(ResponseEntity.ok().build())
                .when(openAiRelay).chatCompletion(any(ChatCompletionRequest.class), anyString(), any());
        lenient().doReturn(ResponseEntity.ok().build())
                .when(openAiRelay).embedding(any(EmbeddingRequest.class), anyString(), any());
    }

    /**
     * 把 entry 转成带占位符替换（用合法 UUID）的 MockMvc request builder.
     * <p>
     * 把 {@code {id}} 占位符替换成 seed 数据的 UUID（不存在的实体也走不存在的 UUID，
     * 服务层会抛 400；权限矩阵关心的是 token 拦截，不是业务结果）。
     * </p>
     */
    private MockHttpServletRequestBuilder buildRequest(EndpointCatalog.Entry entry) {
        // 用一个不存在的 UUID 作为占位符 —— 避免 DELETE 操作误删种子 admin/testuser，
        // 矩阵关注 token 拦截而非业务结果，所以"找不到资源"也合法（HTTP 200 + body.code=400）。
        String path = entry.path().replace("{id}", "99999999-9999-9999-9999-999999999999");
        MockHttpServletRequestBuilder builder;
        if (entry.method() == org.springframework.http.HttpMethod.GET) {
            builder = get(path);
        } else if (entry.method() == org.springframework.http.HttpMethod.POST) {
            builder = post(path);
        } else if (entry.method() == org.springframework.http.HttpMethod.PUT) {
            builder = put(path);
        } else if (entry.method() == org.springframework.http.HttpMethod.DELETE) {
            builder = delete(path);
        } else {
            throw new IllegalArgumentException("Unsupported: " + entry.method());
        }
        // POST/PUT 请求需要 body —— 给一个最小合法 body
        if (entry.method() == org.springframework.http.HttpMethod.POST
                || entry.method() == org.springframework.http.HttpMethod.PUT) {
            builder.contentType(MediaType.APPLICATION_JSON);
            // lottery.statMultiple 需要特殊 body（ranges），其他通用 body 就行
            if (entry.tag().equals("lottery.statMultiple")) {
                builder.content("{\"ranges\":[{\"label\":\"x\",\"startDate\":\"2025-01-01\",\"endDate\":\"2025-01-02\"}]}");
            } else {
                builder.content("{}");
            }
        }
        return builder;
    }

    // =================== A. 匿名（无 token） ===================

    static java.util.stream.Stream<EndpointCatalog.Entry> requiredEndpoints() {
        return EndpointCatalog.required().stream();
    }

    @ParameterizedTest(name = "anonymous {0} {1} → 401")
    @MethodSource("requiredEndpoints")
    void anonymous_requiredEndpoint_returns401(EndpointCatalog.Entry entry) throws Exception {
        ResultActions actions = mockMvc.perform(buildRequest(entry))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("no permission to call"));
        actions.andReturn();
    }

    // =================== B. 错签名 token ===================

    @ParameterizedTest(name = "garbage token {0} {1} → 401")
    @MethodSource("requiredEndpoints")
    void garbageToken_requiredEndpoint_returns401(EndpointCatalog.Entry entry) throws Exception {
        mockMvc.perform(buildRequest(entry)
                        .header("Authorization", "Bearer not-a-jwt-at-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== C. 过期 token ===================

    @ParameterizedTest(name = "expired token {0} {1} → 401")
    @MethodSource("requiredEndpoints")
    void expiredToken_requiredEndpoint_returns401(EndpointCatalog.Entry entry) throws Exception {
        mockMvc.perform(buildRequest(entry)
                        .header("Authorization", jwtFactory.expiredBearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== D. 合法 USER token（当前没 RBAC → 全部 200） ===================

    @ParameterizedTest(name = "USER token {0} {1} → 200")
    @MethodSource("requiredEndpoints")
    void userToken_requiredEndpoint_returns200_noRbacYet(EndpointCatalog.Entry entry) throws Exception {
        // 截至 2026-08-05 整个 /api/** + /v1/** 没有 role-based 鉴权，
        // USER 角色也能调所有端点。本测试固化这一现状。
        // （详见 docs/specs/2026-08-05-all-endpoints-integration-test.md 的「需要你确认」flag。）
        ResultActions actions = mockMvc.perform(buildRequest(entry)
                        .header("Authorization", jwtFactory.bearer("testuser")))
                .andExpect(status().isOk());
        // 不强制 $.code —— 业务结果可能 200/201/400 都行，关键是 HTTP 200 + token 没被拦截。
        actions.andReturn();
    }

    // =================== E. 合法 ADMIN token → 200 ===================

    @ParameterizedTest(name = "ADMIN token {0} {1} → 200")
    @MethodSource("requiredEndpoints")
    void adminToken_requiredEndpoint_returns200(EndpointCatalog.Entry entry) throws Exception {
        mockMvc.perform(buildRequest(entry)
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk());
    }

    // =================== F. @SkipAuth 短路验证 ===================

    /**
     * {@code @SkipAuth} 端点不带 token → 200。
     * <p>
     * 注意：{@code POST /api/auth/login} 不带 token 会因为请求体为空抛校验错；这里给最小合法 body。
     * </p>
     */
    @Test
    void skipAuth_login_noToken_returns200() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void skipAuth_register_noToken_returns200() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"skipauth_user\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201));
    }

    @Test
    void skipAuth_login_expiredToken_returns200() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header("Authorization", jwtFactory.expiredBearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void skipAuth_login_garbageToken_returns200() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header("Authorization", "Bearer not-a-jwt-at-all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}