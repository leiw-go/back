/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller;

import com.yaowenltd.projectinfomationmanage.common.JwtUtil;
import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.config.SkipAuth;
import com.yaowenltd.projectinfomationmanage.model.dto.CurrentUserResponse;
import com.yaowenltd.projectinfomationmanage.model.dto.LoginRequest;
import com.yaowenltd.projectinfomationmanage.model.dto.LoginResponse;
import com.yaowenltd.projectinfomationmanage.model.dto.RegisterRequest;
import com.yaowenltd.projectinfomationmanage.model.dto.RegisterResponse;
import com.yaowenltd.projectinfomationmanage.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 负责用户认证（登录/登出）与当前用户信息查询的 HTTP 接口.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    private final JwtUtil jwtUtil;

    /**
     * 使用给定的 AuthService 和 JwtUtil 构造 AuthController.
     *
     * @param authService 认证服务
     * @param jwtUtil     用于解析 Token 的 JWT 工具类
     */
    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 注册一个新用户，并赋予默认的 USER 角色.
     *
     * @param registerRequest 注册数据
     * @return 注册响应
     */
    @PostMapping("/register")
    @SkipAuth
    @Operation(summary = "User registration", description = "Register a new user with default USER role")
    public ResponseResult<RegisterResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        RegisterResponse response = authService.register(registerRequest);
        return ResponseResult.created(response);
    }

    /**
     * 校验用户身份并返回 JWT Token.
     *
     * @param loginRequest 登录凭据
     * @return 包含 Token 与用户信息的响应
     */
    @PostMapping("/login")
    @SkipAuth
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    public ResponseResult<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse = authService.login(loginRequest);
        return ResponseResult.success(loginResponse);
    }

    /**
     * 通过解析 Authorization 请求头中的 JWT Token，获取当前已登录用户的信息.
     *
     * @param authorization 包含 Bearer Token 的 Authorization 请求头
     * @return 包含当前用户详情与角色的响应
     */
    @GetMapping("/currentUser")
    @Operation(summary = "Get current user", description = "Get current authenticated user information")
    public ResponseResult<CurrentUserResponse> getCurrentUser(
            @RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(BEARER_PREFIX.length());
        String username = jwtUtil.getUsernameFromToken(token);
        CurrentUserResponse currentUser = authService.getCurrentUser(username);
        return ResponseResult.success(currentUser);
    }
}
