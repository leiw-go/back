/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for user registration request.
 */
@Schema(description = "用户注册请求参数")
public class RegisterRequest {

    @NotBlank(message = "username cannot be empty")
    @Size(min = 2, max = 64, message = "username length must be between 2 and 64")
    @Schema(description = "用户名", example = "newuser")
    private String username;

    @NotBlank(message = "password cannot be empty")
    @Size(min = 6, max = 128, message = "password length must be between 6 and 128")
    @Schema(description = "密码", example = "password123")
    private String password;

    @Schema(description = "真实姓名", example = "新用户")
    @Size(max = 64, message = "real name length must not exceed 64")
    private String realName;

    @Schema(description = "邮箱", example = "newuser@example.com")
    @Size(max = 128, message = "email length must not exceed 128")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    @Size(max = 20, message = "phone length must not exceed 20")
    private String phone;

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

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
