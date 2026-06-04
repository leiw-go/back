/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for role information, used for creating and updating roles.
 */
public class RoleDto {

    private String id;

    @NotBlank(message = "role name cannot be empty")
    @Size(max = 64, message = "role name length must not exceed 64")
    private String roleName;

    @NotBlank(message = "role code cannot be empty")
    @Size(max = 64, message = "role code length must not exceed 64")
    private String roleCode;

    @Size(max = 256, message = "description length must not exceed 256")
    private String description;

    private Integer status;

    private List<String> permissionIds;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public List<String> getPermissionIds() {
        return permissionIds;
    }

    public void setPermissionIds(List<String> permissionIds) {
        this.permissionIds = permissionIds;
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
