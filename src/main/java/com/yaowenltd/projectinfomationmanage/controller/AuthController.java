/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.config.SkipAuth;
import com.yaowenltd.projectinfomationmanage.model.dto.LoginRequest;
import com.yaowenltd.projectinfomationmanage.model.dto.LoginResponse;
import com.yaowenltd.projectinfomationmanage.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for user authentication (login/logout).
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthService authService;

    /**
     * Constructs an AuthController with the given AuthService.
     *
     * @param authService the authentication service
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
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
}
