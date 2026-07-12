/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 当前已认证用户响应的 DTO.
 * 由 GET /api/auth/currentUser 端点返回.
 */
@Schema(description = "当前登录用户信息")
public class CurrentUserResponse {

    @Schema(description = "用户ID")
    private String id;

    @Schema(description = "用户名", example = "admin")
    private String username;

    private String roleCode;

    @Schema(description = "真实姓名", example = "管理员")
    private String realName;

    @Schema(description = "邮箱", example = "admin@example.com")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    private Integer status;

    /**
     * 默认构造器（用于 JSON 反序列化等场景）.
     */
    public CurrentUserResponse() {
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

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}