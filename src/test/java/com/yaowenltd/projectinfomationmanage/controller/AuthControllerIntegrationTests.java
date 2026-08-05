/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller;

import com.yaowenltd.projectinfomationmanage.mapper.UserMapper;
import com.yaowenltd.projectinfomationmanage.mapper.UserRoleMapper;
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
import org.springframework.test.web.servlet.MvcResult;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 集成测试 —— {@code @SpringBootTest} + H2 + 真实 {@code AuthService} / {@code JwtUtil}
 * / {@code AuthInterceptor}（不 mock 拦截器）。
 * <p>
 * 通过 {@code @ActiveProfiles("all-endpoints-it")} 激活专属 profile：
 * </p>
 * <ul>
 *   <li>H2 in-memory（{@code MODE=MySQL}）+ 自管 schema/data SQL（9 张表 + 种子）；</li>
 *   <li>Flyway / Nacos config / Nacos discovery / Service registration 全部关闭；</li>
 *   <li>{@code jwt.secret} 由 {@link DynamicPropertySource} 注入随机密钥，{@code jwt.expiration} 走默认 86400000。</li>
 * </ul>
 * <p>
 * <strong>关于 HTTP 状态码：</strong>本项目约定所有响应 HTTP 状态恒为 {@code 200}，
 * 业务语义码（201/400/401/500）落在响应体的 {@code code} 字段。鉴权失败走
 * {@code GlobalExceptionHandler} → HTTP 200 + body.code=401。OpenAI 兼容端点是唯一例外
 * （4xx/5xx 直接落到 HTTP status）。
 * </p>
 * <p>
 * <strong>关于 {@code @SkipAuth}：</strong>{@code AuthController#register} 与
 * {@code AuthController#login} 都标了 {@code @SkipAuth}，意味着即使带了过期 / 错误
 * token 调它们，仍然走真实业务逻辑返回 200 —— 这一行为由
 * {@code AuthPermissionMatrixIntegrationTests#F 行} 系统化覆盖；本类只挑一条
 * "login + 带过期 token → 200"做集中验证。
 * </p>
 *
 * @since 2026-08-05
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("all-endpoints-it")
class AuthControllerIntegrationTests {

    /** 与 {@code AuthServiceImpl.DEFAULT_USER_ROLE_ID} 一致 */
    private static final String DEFAULT_USER_ROLE_ID = "550e8400-e29b-41d4-a716-446655440102";

    /** admin 种子用户的 UUID（h2-data-all.sql 里 hard-coded） */
    private static final String ADMIN_USER_ID = "550e8400-e29b-41d4-a716-446655440201";

    /** testuser 种子用户的 UUID */
    private static final String TESTUSER_USER_ID = "550e8400-e29b-41d4-a716-446655440202";

    /** 每次 JVM 启动随机生成的 64-byte 密钥 base64 —— 避免真实密钥进仓库 */
    private static final String JWT_SECRET_BASE64 = generateRandomSecret();

    private static String generateRandomSecret() {
        byte[] keyBytes = new byte[64];
        new SecureRandom().nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    /**
     * 把随机生成的 jwt.secret 暴露为 Spring 环境属性，
     * 让 {@code @Value("${jwt.secret}")} 注入的 {@code JwtUtil} + {@link JwtTestTokenFactory} 都能拿到。
     */
    @DynamicPropertySource
    static void registerJwtSecret(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> JWT_SECRET_BASE64);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private JwtTestTokenFactory jwtFactory;

    /**
     * 每个 case 前清空用户与角色关联（FK + UNIQUE 约束下必须先删 t_user_role / t_llm_request_log，
     * 再删 t_user / t_llm_provider 等）。种子（admin / testuser / permission / role / provider）不删，
     * 让依赖这些种子的测试可重复运行。
     */
    @BeforeEach
    void resetState() {
        // 顺序：先删依赖，再删主体
        jdbcTemplate.execute("DELETE FROM t_user_role");
        jdbcTemplate.execute("DELETE FROM t_llm_request_log");
        jdbcTemplate.execute("DELETE FROM t_user WHERE id NOT IN ('" + ADMIN_USER_ID + "', '" + TESTUSER_USER_ID + "')");
        // 还原 admin / testuser 的密码（防止某个 case 改了它们）
        jdbcTemplate.update("UPDATE t_user SET password = ? WHERE id = ?",
                "$2a$10$FtMOPk5bSAG2p6udDM1wy.hf0GCFGeX5hbRg74Bh6Z1fVQxfByCOi", ADMIN_USER_ID);
        jdbcTemplate.update("UPDATE t_user SET password = ? WHERE id = ?",
                "$2a$10$aV7SsUHGnfAyHN5dIzpCW.ljxQbHSHQKwBxV3UPeZ2LR0pweelWbK", TESTUSER_USER_ID);
        // 重新绑定 admin/testuser 角色（防前序 case 删了 user_role）
        Integer adminBound = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_user_role WHERE user_id = ?", Integer.class, ADMIN_USER_ID);
        if (adminBound == null || adminBound == 0) {
            jdbcTemplate.update(
                    "INSERT INTO t_user_role (id, user_id, role_id) VALUES (?, ?, ?)",
                    "550e8400-e29b-41d4-a716-446655440301", ADMIN_USER_ID,
                    "550e8400-e29b-41d4-a716-446655440101");
        }
        Integer userBound = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_user_role WHERE user_id = ?", Integer.class, TESTUSER_USER_ID);
        if (userBound == null || userBound == 0) {
            jdbcTemplate.update(
                    "INSERT INTO t_user_role (id, user_id, role_id) VALUES (?, ?, ?)",
                    "550e8400-e29b-41d4-a716-446655440302", TESTUSER_USER_ID,
                    "550e8400-e29b-41d4-a716-446655440102");
        }
    }

    // =================== register ===================

    /**
     * 注册 happy —— 无 token 调通（@SkipAuth 短路），落库 + 默认 USER 角色绑定。
     */
    @Test
    void register_insertsUser_andBindsDefaultUserRole() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"pass1234\",\"realName\":\"Alice\","
                                + "\"email\":\"alice@example.com\",\"phone\":\"13800138000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.message").value("user registered successfully"));

        var alice = userMapper.findUserByUsername("alice");
        assertNotNull(alice, "alice should be persisted in H2");
        assertEquals("Alice", alice.getRealName());
        assertTrue(alice.getPassword().startsWith("$2a$"), "stored password should be BCrypt-hashed");

        var roles = userRoleMapper.findUserRolesByUserId(alice.getId());
        assertEquals(1, roles.size());
        assertEquals(DEFAULT_USER_ROLE_ID, roles.get(0).getRoleId());
    }

    /**
     * 注册带过期 token —— 仍然 200（@SkipAuth 不解析 Authorization header）。
     */
    @Test
    void register_succeeds_evenWithExpiredToken() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .header("Authorization", jwtFactory.expiredBearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bob\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201));
    }

    /**
     * 重复注册 → 400 + DB 仍仅 1 行。
     */
    @Test
    void register_returnsCode400_whenUsernameAlreadyExists() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"carol\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"carol\",\"password\":\"different123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("username already exists"));

        assertNotNull(userMapper.findUserByUsername("carol"));
    }

    /**
     * password 校验失败 → 400 + DB 无写入。
     */
    @Test
    void register_returnsCode400_whenPasswordBlank() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dave\",\"password\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        assertNull(userMapper.findUserByUsername("dave"));
    }

    // =================== login ===================

    /**
     * 合法凭证登录 → 200 + 返回 JWT（eyJ 开头）+ 真实字段。
     */
    @Test
    void login_returnsJwt_andParsesBackAsUsername() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.realName").value("Administrator"))
                .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        assertTrue(responseJson.contains("\"token\":\"") && responseJson.split("\"token\":\"")[1].startsWith("eyJ"),
                "login response must contain a JWT-shaped token starting with 'eyJ'");
    }

    /**
     * 密码错误 → 401（HTTP 200 + body.code=401）。
     */
    @Test
    void login_returnsCode401_whenPasswordIsIncorrect() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    /**
     * 用户不存在 → 401。
     */
    @Test
    void login_returnsCode401_whenUserNotFound() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"ghost\",\"password\":\"any\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    /**
     * 登录带过期 token —— 仍然 200（@SkipAuth 不解析 Authorization header）。
     */
    @Test
    void login_succeeds_evenWithExpiredToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header("Authorization", jwtFactory.expiredBearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    /**
     * 登录带错签名 token —— 仍然 200（@SkipAuth 不解析 Authorization header）。
     */
    @Test
    void login_succeeds_evenWithGarbageToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .header("Authorization", "Bearer not-a-jwt-at-all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // =================== currentUser ===================

    /**
     * 合法 admin token → 200 + 真实用户名 + roleCode = ADMIN。
     */
    @Test
    void currentUser_returnsUserWithRoleCode_whenAdminToken() throws Exception {
        String token = jwtFactory.bearer("admin");

        mockMvc.perform(get("/api/auth/currentUser")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.realName").value("Administrator"))
                .andExpect(jsonPath("$.data.roleCode").value("ADMIN"));
    }

    /**
     * 合法 testuser token → 200 + roleCode = USER。
     */
    @Test
    void currentUser_returnsUserWithRoleCode_whenTestuserToken() throws Exception {
        String token = jwtFactory.bearer("testuser");

        mockMvc.perform(get("/api/auth/currentUser")
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.roleCode").value("USER"));
    }

    /**
     * 完整链路：admin 登录拿 token → currentUser 解析回 admin + roleCode。
     */
    @Test
    void fullFlow_login_currentUser_returnsRoleCode() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String token = loginResult.getResponse().getContentAsString()
                .split("\"token\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/api/auth/currentUser")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.roleCode").value("ADMIN"));
    }

    /**
     * 过期 token → 401（AuthInterceptor 拦截，UnauthorizedException → GlobalExceptionHandler）。
     */
    @Test
    void currentUser_returnsCode401_whenTokenExpired() throws Exception {
        mockMvc.perform(get("/api/auth/currentUser")
                        .header("Authorization", jwtFactory.expiredBearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("no permission to call"));
    }

    /**
     * 无 token → 401（AuthInterceptor 拦截，缺少 Authorization 头）。
     */
    @Test
    void currentUser_returnsCode401_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/auth/currentUser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("no permission to call"));
    }

    /**
     * 错签名 token → 401（AuthInterceptor 通过 JwtUtil.validateToken 校验失败）。
     */
    @Test
    void currentUser_returnsCode401_whenTokenIsGarbage() throws Exception {
        mockMvc.perform(get("/api/auth/currentUser")
                        .header("Authorization", "Bearer not-a-jwt-at-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}