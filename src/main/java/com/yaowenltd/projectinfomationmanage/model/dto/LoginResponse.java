/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.model.dto;

/**
 * DTO for login response.
 */
public class LoginResponse {

    private String token;

    private String username;

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
