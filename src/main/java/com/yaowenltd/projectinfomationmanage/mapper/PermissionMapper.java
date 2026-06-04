/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.mapper;

import com.yaowenltd.projectinfomationmanage.model.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Mapper interface for Permission database operations.
 */
@Mapper
public interface PermissionMapper {

    /**
     * Inserts a new permission.
     *
     * @param permission the permission to insert
     * @return the number of affected rows
     */
    int insertPermission(Permission permission);

    /**
     * Deletes a permission by ID.
     *
     * @param id the permission ID
     * @return the number of affected rows
     */
    int deletePermissionById(@Param("id") String id);

    /**
     * Finds a permission by ID.
     *
     * @param id the permission ID
     * @return the permission, or null if not found
     */
    Permission findPermissionById(@Param("id") String id);

    /**
     * Returns all permissions.
     *
     * @return list of all permissions
     */
    List<Permission> findAllPermissions();

    /**
     * Finds permissions by role ID.
     *
     * @param roleId the role ID
     * @return list of permissions assigned to the role
     */
    List<Permission> findPermissionsByRoleId(@Param("roleId") String roleId);
}
