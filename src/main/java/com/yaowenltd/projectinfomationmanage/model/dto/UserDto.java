/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户信息的 DTO，用于用户的创建和更新.
 */
@Schema(description = "用户信息")
public class UserDto {

    @Schema(description = "用户ID")
    private String id;

    @NotBlank(message = "username cannot be empty")
    @Schema(description = "用户名", example = "zhangsan")
    @Size(min = 2, max = 64, message = "username length must be between 2 and 64")
    private String username;

    @Schema(description = "密码", example = "123456")
    @Size(min = 6, max = 128, message = "password length must be between 6 and 128")
    private String password;

    @Schema(description = "真实姓名", example = "张三")
    @Size(max = 64, message = "real name length must not exceed 64")
    private String realName;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    @Size(max = 128, message = "email length must not exceed 128")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    @Size(max = 20, message = "phone length must not exceed 20")
    private String phone;

    @Schema(description = "状态: 1=启用, 0=禁用", example = "1")
    private Integer status;

    @Schema(description = "角色ID列表")
    private List<String> roleIds;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<String> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<String> roleIds) {
        this.roleIds = roleIds;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
