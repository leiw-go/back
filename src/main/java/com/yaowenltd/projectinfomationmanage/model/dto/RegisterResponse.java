/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for user registration response.
 */
@Schema(description = "用户注册响应结果")
public class RegisterResponse {

    @Schema(description = "用户ID", example = "550e8400-e29b-41d4-a716-446655440201")
    private String id;

    @Schema(description = "用户名", example = "newuser")
    private String username;

    @Schema(description = "真实姓名", example = "新用户")
    private String realName;

    @Schema(description = "注册成功消息")
    private String message;

    public RegisterResponse() {
    }

    public RegisterResponse(String id, String username, String realName, String message) {
        this.id = id;
        this.username = username;
        this.realName = realName;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
