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
 * DTO for role information, used for creating and updating roles.
 */
@Schema(description = "角色信息")
public class RoleDto {

    @Schema(description = "角色ID")
    private String id;

    @NotBlank(message = "role name cannot be empty")
    @Schema(description = "角色名称", example = "管理员")
    @Size(max = 64, message = "role name length must not exceed 64")
    private String roleName;

    @NotBlank(message = "role code cannot be empty")
    @Schema(description = "角色编码", example = "ADMIN")
    @Size(max = 64, message = "role code length must not exceed 64")
    private String roleCode;

    @Schema(description = "角色描述", example = "系统管理员，拥有全部权限")
    @Size(max = 256, message = "description length must not exceed 256")
    private String description;

    @Schema(description = "状态: 1=启用, 0=禁用", example = "1")
    private Integer status;

    @Schema(description = "权限ID列表")
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
