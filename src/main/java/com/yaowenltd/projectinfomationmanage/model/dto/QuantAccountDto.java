/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * DTO for JoinQuant account configuration.
 */
@Schema(description = "聚宽账户信息")
public class QuantAccountDto {

    @Schema(description = "账户ID")
    private String id;

    @NotBlank(message = "account name cannot be empty")
    @Schema(description = "账户别名", example = "主账户")
    @Size(max = 128, message = "account name length must not exceed 128")
    private String accountName;

    @NotBlank(message = "username cannot be empty")
    @Schema(description = "聚宽用户名", example = "13800000000")
    @Size(max = 128, message = "username length must not exceed 128")
    private String username;

    @Schema(description = "聚宽密码 (创建时必填，更新时可选)")
    @Size(max = 256, message = "password length must not exceed 256")
    private String password;

    @Schema(description = "绑定的手机号", example = "13800000000")
    @Size(max = 20, message = "phone length must not exceed 20")
    private String phone;

    @Schema(description = "是否启用: 1=启用, 0=禁用", example = "1")
    private Integer isActive;

    private LocalDateTime lastLoginTime;

    private String lastLoginStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getLastLoginTime() {
        return lastLoginTime;
    }

    public void setLastLoginTime(LocalDateTime lastLoginTime) {
        this.lastLoginTime = lastLoginTime;
    }

    public String getLastLoginStatus() {
        return lastLoginStatus;
    }

    public void setLastLoginStatus(String lastLoginStatus) {
        this.lastLoginStatus = lastLoginStatus;
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
