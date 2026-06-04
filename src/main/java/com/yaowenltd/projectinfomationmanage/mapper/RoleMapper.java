/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.mapper;

import com.yaowenltd.projectinfomationmanage.model.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Mapper interface for Role database operations.
 */
@Mapper
public interface RoleMapper {

    /**
     * Inserts a new role.
     *
     * @param role the role to insert
     * @return the number of affected rows
     */
    int insertRole(Role role);

    /**
     * Updates an existing role.
     *
     * @param role the role with updated information
     * @return the number of affected rows
     */
    int updateRole(Role role);

    /**
     * Deletes a role by ID.
     *
     * @param id the role ID
     * @return the number of affected rows
     */
    int deleteRoleById(@Param("id") String id);

    /**
     * Finds a role by ID.
     *
     * @param id the role ID
     * @return the role, or null if not found
     */
    Role findRoleById(@Param("id") String id);

    /**
     * Returns all roles.
     *
     * @return list of all roles
     */
    List<Role> findAllRoles();

    /**
     * Finds roles by user ID.
     *
     * @param userId the user ID
     * @return list of roles assigned to the user
     */
    List<Role> findRolesByUserId(@Param("userId") String userId);
}
