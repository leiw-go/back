/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller;

import com.yaowenltd.projectinfomationmanage.common.GlobalExceptionHandler;
import com.yaowenltd.projectinfomationmanage.common.JwtUtil;
import com.yaowenltd.projectinfomationmanage.common.UnauthorizedException;
import com.yaowenltd.projectinfomationmanage.model.dto.CurrentUserResponse;
import com.yaowenltd.projectinfomationmanage.model.dto.LoginRequest;
import com.yaowenltd.projectinfomationmanage.model.dto.LoginResponse;
import com.yaowenltd.projectinfomationmanage.model.dto.RegisterRequest;
import com.yaowenltd.projectinfomationmanage.model.dto.RegisterResponse;
import com.yaowenltd.projectinfomationmanage.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 单元测试（Mockito + standalone MockMvc）。
 * <p>
 * 不启动 Spring 容器，不连接任何外部基础设施（含 MySQL / Nacos / Redis），
 * 不读取 {@code application-test.yml}。仅靠 {@link MockitoExtension} 装配
 * {@link AuthService} 与 {@link JwtUtil} 的 mock，双倍覆盖 controller 层的三条路由：
 * </p>
 * <ul>
 *   <li>{@code POST /api/auth/register}</li>
 *   <li>{@code POST /api/auth/login}</li>
 *   <li>{@code GET  /api/auth/currentUser}</li>
 * </ul>
 * <p>
 * 用例覆盖：正常路径、@Valid 校验失败、业务异常（{@code IllegalArgumentException} /
 * {@code UnauthorizedException}）。{@code GlobalExceptionHandler} 通过
 * {@code setControllerAdvice(...)} 显式装上，让 controller advice 的状态码语义也参与测试。
 * </p>
 * <p>
 * <strong>关于 HTTP 状态码的契约：</strong>
 * {@code AuthController} 走 {@code ResponseResult.created(...)} / {@code .success(...)}
 * 等静态工厂返回的是 POJO 而非 {@code ResponseEntity}；{@code GlobalExceptionHandler} 同
 * 样以 POJO 返回，{@code @ResponseStatus} 也没有挂在 controller 方法上。故本契约下
 * <strong>所有响应的 HTTP 状态码恒为 {@code 200}</strong>（即使业务含义是 4xx），语义
 * 状态码（包括 201/400/401/403 等）落在响应体的 {@code code} 字段里。本测试显式
 * 描述该行为，未来若有人给 controller/handler 加上 {@code @ResponseStatus} 或
 * 改返回 {@code ResponseEntity}，这些断言需要顺带更新。
 * </p>
 *
 * @since 2026-07-27
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerUnitTests {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthController controller;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    /**
     * 合法注册请求：业务上视为"创建成功"（body.code=201、message="created"），
     * 但因 AuthController 未挂 {@code @ResponseStatus}，HTTP 状态仍为 200。
     */
    @Test
    void register_returns200WithCode201_whenRequestIsValid() throws Exception {
        RegisterResponse stub = new RegisterResponse("user-id-1", "alice", "Alice",
                "user registered successfully");
        when(authService.register(any(RegisterRequest.class))).thenReturn(stub);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"pass1234\",\"realName\":\"Alice\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("created"))
                .andExpect(jsonPath("$.data.id").value("user-id-1"))
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.realName").value("Alice"));

        verify(authService).register(any(RegisterRequest.class));
    }

    /**
     * username 为空字符串触发校验（{@code @NotBlank} + {@code @Size(min=2)}） →
     * body.code=400，service 不被调用.
     */
    @Test
    void register_returns200WithCode400_whenUsernameIsBlank() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").exists());

        verify(authService, never()).register(any());
    }

    /**
     * password 缺失同样触发 {@code @NotBlank} → body.code=400.
     */
    @Test
    void register_returns200WithCode400_whenPasswordIsBlank() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        verify(authService, never()).register(any());
    }

    /**
     * Service 抛 {@code IllegalArgumentException}（重复 username）→ body.code=400 + 错误信息.
     */
    @Test
    void register_returns200WithCode400_whenUsernameAlreadyExists() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("username already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bob\",\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("username already exists"))
                .andExpect(jsonPath("$.errors[0]").value("username already exists"));
    }

    /**
     * 合法凭证登录 → 200 + token.
     */
    @Test
    void login_returns200_whenCredentialsAreValid() throws Exception {
        LoginResponse stub = new LoginResponse("jwt-token-abc", "admin", "Administrator");
        when(authService.login(any(LoginRequest.class))).thenReturn(stub);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token").value("jwt-token-abc"))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.realName").value("Administrator"));
    }

    /**
     * Service 抛 {@code UnauthorizedException}（密码错误）→ body.code=401，HTTP 仍为 200。
     */
    @Test
    void login_returns200WithCode401_whenPasswordIsIncorrect() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new UnauthorizedException("username or password is incorrect"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("username or password is incorrect"));
    }

    /**
     * {@code Authorization: Bearer ...} 正确解析：{@code JwtUtil.getUsernameFromToken}
     * 收到的应是去掉前缀后的纯 token，而非整段 header.
     */
    @Test
    void currentUser_returns200_andPassesTokenSubstringToJwtUtil() throws Exception {
        when(jwtUtil.getUsernameFromToken("real-jwt-payload")).thenReturn("admin");
        CurrentUserResponse stub = new CurrentUserResponse();
        stub.setId("u-1");
        stub.setUsername("admin");
        stub.setRoleCode("ADMIN");
        stub.setRealName("Administrator");
        stub.setEmail("admin@example.com");
        stub.setStatus(1);
        when(authService.getCurrentUser("admin")).thenReturn(stub);

        mockMvc.perform(get("/api/auth/currentUser")
                        .header("Authorization", "Bearer real-jwt-payload"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.roleCode").value("ADMIN"));

        verify(jwtUtil).getUsernameFromToken("real-jwt-payload");
        verify(authService).getCurrentUser("admin");
    }

    /**
     * Service 抛 {@code UnauthorizedException}（user not found）→ body.code=401，HTTP 仍为 200。
     */
    @Test
    void currentUser_returns200WithCode401_whenUserNotFound() throws Exception {
        when(jwtUtil.getUsernameFromToken("ghost-jwt")).thenReturn("ghost");
        when(authService.getCurrentUser("ghost"))
                .thenThrow(new UnauthorizedException("user not found"));

        mockMvc.perform(get("/api/auth/currentUser")
                        .header("Authorization", "Bearer ghost-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}
