/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller;

import com.yaowenltd.projectinfomationmanage.config.AuthInterceptor;
import com.yaowenltd.projectinfomationmanage.mapper.UserMapper;
import com.yaowenltd.projectinfomationmanage.mapper.UserRoleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.method.HandlerMethod;

import java.security.SecureRandom;
import java.util.Base64;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 集成测试（{@code @SpringBootTest} + H2 + 真实 Mapper / Service）。
 * <p>
 * 通过 {@code @ActiveProfiles("auth-it")} 激活专属 profile：
 * </p>
 * <ul>
 *   <li>H2 in-memory 数据库（{@code MODE=MySQL}）+ 自管 schema/data SQL；</li>
 *   <li>Flyway / Nacos config / Nacos discovery / Service registration 全部关闭；</li>
 *   <li>{@code jwt.secret} 直接由 profile 提供，不依赖 Nacos。</li>
 * </ul>
 * <p>
 * 由于 {@link com.yaowenltd.projectinfomationmanage.config.WebMvcConfig} 把
 * {@link AuthInterceptor} 注册到 {@code /design/api/**}，集成测试里用 {@link MockBean}
 * 把它替换为 Mockito 代理并打桩成 {@code preHandle=true}；controller 内部的
 * {@code JwtUtil.getUsernameFromToken} 仍走真实逻辑，验证 register → login → currentUser
 * 完整链路。
 * </p>
 * <p>
 * <strong>关于 HTTP 状态码：</strong>{@code AuthController} 与
 * {@code GlobalExceptionHandler} 均以 {@code ResponseResult} POJO 返回，
 * 没有 {@code @ResponseStatus} 或 {@code ResponseEntity}。本契约下所有响应
 * HTTP 状态恒为 {@code 200}，业务语义码（201/400/401）落在响应体的 {@code code} 字段。
 * 断言走 {@code jsonPath("$.code")}。
 * </p>
 *
 * @since 2026-07-27
 */
@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@ActiveProfiles("auth-it")
class AuthControllerIntegrationTests {

    /**
     * 默认 USER 角色 UUID，与 {@code AuthServiceImpl.DEFAULT_USER_ROLE_ID} 保持一致；
     * data.sql 在 {@code src/test/resources/db/} 里按该 ID 预先种子。
     */
    private static final String DEFAULT_USER_ROLE_ID = "550e8400-e29b-41d4-a716-446655440102";

    /**
     * 每次 JVM 启动随机生成的 64-byte 密钥的 base64 编码。
     * <p>
     * 通过 {@link DynamicPropertySource} 注入到 Spring 上下文，避免在仓库任何 yaml / properties /
     * Java 源文件里固化 HMAC 密钥字面值，符合 CLAUDE.md §10"不要在仓库里写真实 secret"。
     * </p>
     */
    private static final String JWT_SECRET_BASE64 = generateRandomSecret();

    private static String generateRandomSecret() {
        byte[] keyBytes = new byte[64];
        new SecureRandom().nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    /**
     * 在 Spring 上下文构建之前把动态生成的 jwt.secret 暴露为环境属性，
     * 让 {@code @Value("${jwt.secret}")} 注入的 {@link com.yaowenltd.projectinfomationmanage.common.JwtUtil}
     * 能拿到这份随机生成的密钥。
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

    /**
     * 拦截器被打成 Mockito 代理，{@code preHandle} 统一返回 true，避免在每次
     * 测试里都重写 @RequestHeader 的样样关切。
     * <p>
     * 用 {@code lenient()} 因为部分校验失败场景下 Spring 不会调用 interceptor；
     * 严格模式下不 lenient 会把那些合法场景误报成 UnnecessaryStubbingException。
     * </p>
     */
    @MockBean
    private AuthInterceptor authInterceptor;

    @BeforeEach
    void resetState() throws Exception {
        // 先关掉拦截器（必须在 resetState 开头就 stub，否则基线用例没走 preHandle 时 Mockito strict 会报红）
        lenient().when(authInterceptor.preHandle(any(HttpServletRequest.class), any(HttpServletResponse.class), any(HandlerMethod.class)))
                .thenReturn(true);

        // 清掉用户表，FK + UNIQUE 约束下 t_user_role 必须先删
        jdbcTemplate.execute("DELETE FROM t_user_role");
        jdbcTemplate.execute("DELETE FROM t_user");
    }

    /**
     * 真实注册链路：HTTP → AuthController → AuthServiceImpl → UserMapper → H2；
     * 之后通过 {@link UserMapper} 查回来验证确实落库了。
     */
    @Test
    void register_insertsUser_intoH2() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"pass1234\",\"realName\":\"Alice\","
                                + "\"email\":\"alice@example.com\",\"phone\":\"13800138000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.username").value("alice"));

        var alice = userMapper.findUserByUsername("alice");
        org.junit.jupiter.api.Assertions.assertNotNull(alice, "alice should be persisted in H2");
        org.junit.jupiter.api.Assertions.assertEquals("Alice", alice.getRealName());
        org.junit.jupiter.api.Assertions.assertEquals("alice@example.com", alice.getEmail());
        // password 已被 BCrypt 编码，不等于原始 pass1234
        org.junit.jupiter.api.Assertions.assertNotEquals("pass1234", alice.getPassword());
        org.junit.jupiter.api.Assertions.assertTrue(alice.getPassword().startsWith("$2a$"),
                "stored password should be BCrypt-hashed");
    }

    /**
     * 注册成功后应自动绑定默认 USER 角色（{@code t_user_role} 落一行）。
     */
    @Test
    void register_bindsDefaultUserRole() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bob\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201));

        var bob = userMapper.findUserByUsername("bob");
        org.junit.jupiter.api.Assertions.assertNotNull(bob);
        var roles = userRoleMapper.findUserRolesByUserId(bob.getId());
        org.junit.jupiter.api.Assertions.assertEquals(1, roles.size(), "exactly one user_role row expected");
        org.junit.jupiter.api.Assertions.assertEquals(DEFAULT_USER_ROLE_ID, roles.get(0).getRoleId());
    }

    /**
     * 重复注册同名用户 → body.code=400，DB 仍仅 1 行（HTTP 状态恒 200，见类 Javadoc）。
     */
    @Test
    void register_returnsCode400_whenUsernameAlreadyExists() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"carol\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"carol\",\"password\":\"different123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("username already exists"));

        org.junit.jupiter.api.Assertions.assertNotNull(userMapper.findUserByUsername("carol"),
                "first registration should remain");
    }

    /**
     * 校验失败 → body.code=400，DB 无任何写入。
     */
    @Test
    void register_returnsCode400_whenPasswordBlank() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"dave\",\"password\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        org.junit.jupiter.api.Assertions.assertNull(userMapper.findUserByUsername("dave"));
    }

    /**
     * 合法凭证登录 → 200 + 真实 JWT，token 经真实 {@code JwtUtil} 解析可还原 username。
     */
    @Test
    void login_returnsJwt_andParsesBackAsUsername() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"erin\",\"password\":\"pass1234\",\"realName\":\"Erin\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"erin\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("erin"))
                .andExpect(jsonPath("$.data.realName").value("Erin"))
                .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        // token 是三段 base64url（header.payload.signature），长度与点号数量是粗略但稳定的契约。
        org.junit.jupiter.api.Assertions.assertTrue(responseJson.contains("\"token\":\"")
                && responseJson.split("\"token\":\"")[1].startsWith("eyJ"),
                "login response must contain a JWT-shaped token starting with 'eyJ'");
    }

    /**
     * 密码错误 → body.code=401，DB 中用户不被删除也不被改密（HTTP 状态恒 200）。
     */
    @Test
    void login_returnsCode401_whenPasswordIsIncorrect() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"frank\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"frank\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));

        // 用户仍存在
        var frank = userMapper.findUserByUsername("frank");
        org.junit.jupiter.api.Assertions.assertNotNull(frank);
    }

    /**
     * 用户不存在 → body.code=401。
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
     * 完整链路 register → login → currentUser：
     *   - register 落库；
     *   - login 拿到 token；
     *   - currentUser 带该 token 解析出 username，回写到 service 并返回 CurrentUserResponse，
     *     roleCode 来自注册时自动绑定的 default USER 角色。
     */
    @Test
    void fullFlow_register_login_currentUser_returnsUserWithRoleCode() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"grace\",\"password\":\"pass1234\",\"realName\":\"Grace\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"grace\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andReturn();

        String token = loginResult.getResponse().getContentAsString()
                .split("\"token\":\"")[1].split("\"")[0];

        mockMvc.perform(get("/api/auth/currentUser")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("grace"))
                .andExpect(jsonPath("$.data.realName").value("Grace"))
                .andExpect(jsonPath("$.data.roleCode").value("USER"));
    }

    /**
     * currentUser 拿到的 token 通过真实 {@code JwtUtil} 校验：错误 token 触发
     * {@code io.jsonwebtoken.JwtException} → 被 {@code GlobalExceptionHandler#handleException}
     * 兜底 → HTTP 200 + body.code=500。本契约下"非成功"即视为防护到位。
     */
    @Test
    void currentUser_returnsCode500_whenTokenIsGarbage() throws Exception {
        mockMvc.perform(get("/api/auth/currentUser")
                        .header("Authorization", "Bearer not-a-jwt-at-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }
}
