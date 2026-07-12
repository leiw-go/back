/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 登录响应的 DTO.
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
     * 用 token、用户名和真实姓名构造 LoginResponse.
     *
     * @param token    JWT 令牌
     * @param username 用户名
     * @param realName 用户的真实姓名
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
