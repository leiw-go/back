/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.mapper;

import com.yaowenltd.projectinfomationmanage.model.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Mapper interface for RolePermission database operations.
 */
@Mapper
public interface RolePermissionMapper {

    /**
     * Inserts a new role-permission relationship.
     *
     * @param rolePermission the role-permission relationship to insert
     * @return the number of affected rows
     */
    int insertRolePermission(RolePermission rolePermission);

    /**
     * Deletes role-permission relationships by role ID.
     *
     * @param roleId the role ID
     * @return the number of affected rows
     */
    int deleteRolePermissionByRoleId(@Param("roleId") String roleId);

    /**
     * Finds role-permission relationships by role ID.
     *
     * @param roleId the role ID
     * @return list of role-permission relationships
     */
    List<RolePermission> findRolePermissionsByRoleId(@Param("roleId") String roleId);
}
