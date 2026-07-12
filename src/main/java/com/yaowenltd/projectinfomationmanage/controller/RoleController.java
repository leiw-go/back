/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.model.dto.RoleDto;
import com.yaowenltd.projectinfomationmanage.model.entity.Permission;
import com.yaowenltd.projectinfomationmanage.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 负责角色与权限管理相关操作的 HTTP 接口.
 */
@RestController
@RequestMapping("/api/roles")
@Tag(name = "Role Management", description = "Role and permission management APIs")
public class RoleController {

    private final RoleService roleService;

    /**
     * 使用给定的 RoleService 构造 RoleController.
     *
     * @param roleService 角色服务
     */
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 创建一个新角色.
     *
     * @param roleDto 角色数据
     * @return 已创建的角色
     */
    @PostMapping
    @Operation(summary = "Create role", description = "Create a new role")
    public ResponseResult<RoleDto> createRole(@Valid @RequestBody RoleDto roleDto) {
        RoleDto created = roleService.createRole(roleDto);
        return ResponseResult.created(created);
    }

    /**
     * 更新已存在的角色.
     *
     * @param id      角色 ID
     * @param roleDto 更新后的角色数据
     * @return 更新后的角色
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update role", description = "Update an existing role")
    public ResponseResult<RoleDto> updateRole(@PathVariable String id, @Valid @RequestBody RoleDto roleDto) {
        roleDto.setId(id);
        RoleDto updated = roleService.updateRole(roleDto);
        return ResponseResult.success(updated);
    }

    /**
     * 根据 ID 删除角色.
     *
     * @param id 角色 ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete role", description = "Delete a role by ID")
    public ResponseResult<Void> deleteRole(@PathVariable String id) {
        roleService.deleteRole(id);
        return ResponseResult.success();
    }

    /**
     * 根据 ID 查找角色.
     *
     * @param id 角色 ID
     * @return 角色数据
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID", description = "Get role details by ID")
    public ResponseResult<RoleDto> getRoleById(@PathVariable String id) {
        RoleDto roleDto = roleService.findRoleById(id);
        return ResponseResult.success(roleDto);
    }

    /**
     * 返回所有角色.
     *
     * @return 全部角色列表
     */
    @GetMapping
    @Operation(summary = "List all roles", description = "Get all roles")
    public ResponseResult<List<RoleDto>> getAllRoles() {
        List<RoleDto> roles = roleService.findAllRoles();
        return ResponseResult.success(roles);
    }

    /**
     * 返回所有权限.
     *
     * @return 全部权限列表
     */
    @GetMapping("/permissions")
    @Operation(summary = "List all permissions", description = "Get all permissions")
    public ResponseResult<List<Permission>> getAllPermissions() {
        List<Permission> permissions = roleService.findAllPermissions();
        return ResponseResult.success(permissions);
    }
}
