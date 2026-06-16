/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for login response.
 */
@Schema(description = "登录响应结果")
public class LoginResponse {

    @Schema(description = "JWT Token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String token;

    @Schema(description = "用户名", example = "admin")
    private String username;

    @Schema(description = "真实姓名", example = "管理员")
    private String realName;

    /**
     * Constructs a LoginResponse with token, username and real name.
     *
     * @param token    the JWT token
     * @param username the username
     * @param realName the real name of the user
     */
    public LoginResponse(String token, String username, String realName) {
        this.token = token;
        this.username = username;
        this.realName = realName;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }
}
