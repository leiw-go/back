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
 * Controller for role and permission management operations.
 */
@RestController
@RequestMapping("/api/roles")
@Tag(name = "Role Management", description = "Role and permission management APIs")
public class RoleController {

    private final RoleService roleService;

    /**
     * Constructs a RoleController with the given RoleService.
     *
     * @param roleService the role service
     */
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * Creates a new role.
     *
     * @param roleDto the role data
     * @return the created role
     */
    @PostMapping
    @Operation(summary = "Create role", description = "Create a new role")
    public ResponseResult<RoleDto> createRole(@Valid @RequestBody RoleDto roleDto) {
        RoleDto created = roleService.createRole(roleDto);
        return ResponseResult.created(created);
    }

    /**
     * Updates an existing role.
     *
     * @param id      the role ID
     * @param roleDto the updated role data
     * @return the updated role
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update role", description = "Update an existing role")
    public ResponseResult<RoleDto> updateRole(@PathVariable String id, @Valid @RequestBody RoleDto roleDto) {
        roleDto.setId(id);
        RoleDto updated = roleService.updateRole(roleDto);
        return ResponseResult.success(updated);
    }

    /**
     * Deletes a role by ID.
     *
     * @param id the role ID
     * @return success response
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete role", description = "Delete a role by ID")
    public ResponseResult<Void> deleteRole(@PathVariable String id) {
        roleService.deleteRole(id);
        return ResponseResult.success();
    }

    /**
     * Finds a role by ID.
     *
     * @param id the role ID
     * @return the role data
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get role by ID", description = "Get role details by ID")
    public ResponseResult<RoleDto> getRoleById(@PathVariable String id) {
        RoleDto roleDto = roleService.findRoleById(id);
        return ResponseResult.success(roleDto);
    }

    /**
     * Returns all roles.
     *
     * @return list of all roles
     */
    @GetMapping
    @Operation(summary = "List all roles", description = "Get all roles")
    public ResponseResult<List<RoleDto>> getAllRoles() {
        List<RoleDto> roles = roleService.findAllRoles();
        return ResponseResult.success(roles);
    }

    /**
     * Returns all permissions.
     *
     * @return list of all permissions
     */
    @GetMapping("/permissions")
    @Operation(summary = "List all permissions", description = "Get all permissions")
    public ResponseResult<List<Permission>> getAllPermissions() {
        List<Permission> permissions = roleService.findAllPermissions();
        return ResponseResult.success(permissions);
    }
}
