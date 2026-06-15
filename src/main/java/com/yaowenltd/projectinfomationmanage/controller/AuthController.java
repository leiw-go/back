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
 * Controller for user authentication (login/logout) and current user info.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    private final JwtUtil jwtUtil;

    /**
     * Constructs an AuthController with the given AuthService and JwtUtil.
     *
     * @param authService the authentication service
     * @param jwtUtil     the JWT utility for parsing tokens
     */
    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param loginRequest the login credentials
     * @return the response containing token and user info
     */
    @PostMapping("/login")
    @SkipAuth
    @Operation(summary = "User login", description = "Authenticate user and return JWT token")
    public ResponseResult<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse = authService.login(loginRequest);
        return ResponseResult.success(loginResponse);
    }

    /**
     * Retrieves the current authenticated user's information by parsing the JWT token
     * from the Authorization header.
     *
     * @param authorization the Authorization header containing the Bearer token
     * @return the response containing current user details and role
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