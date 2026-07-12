/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求的 DTO.
 */
@Schema(description = "登录请求参数")
public class LoginRequest {

    @NotBlank(message = "username cannot be empty")
    @Schema(description = "用户名", example = "admin")
    private String username;

    @NotBlank(message = "password cannot be empty")
    @Schema(description = "密码", example = "admin123")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
