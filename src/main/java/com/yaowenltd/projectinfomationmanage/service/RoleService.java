/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service;

import com.yaowenltd.projectinfomationmanage.model.dto.RoleDto;
import com.yaowenltd.projectinfomationmanage.model.entity.Permission;

import java.util.List;

/**
 * Service interface for role and permission management operations.
 */
public interface RoleService {

    /**
     * Creates a new role.
     *
     * @param roleDto the role data
     * @return the created role
     */
    RoleDto createRole(RoleDto roleDto);

    /**
     * Updates an existing role.
     *
     * @param roleDto the role data with updates
     * @return the updated role
     */
    RoleDto updateRole(RoleDto roleDto);

    /**
     * Deletes a role by ID.
     *
     * @param id the role ID
     */
    void deleteRole(String id);

    /**
     * Finds a role by ID.
     *
     * @param id the role ID
     * @return the role data
     */
    RoleDto findRoleById(String id);

    /**
     * Returns all roles.
     *
     * @return list of all roles
     */
    List<RoleDto> findAllRoles();

    /**
     * Returns all permissions.
     *
     * @return list of all permissions
     */
    List<Permission> findAllPermissions();
}
