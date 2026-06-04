/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.mapper;

import com.yaowenltd.projectinfomationmanage.model.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Mapper interface for UserRole database operations.
 */
@Mapper
public interface UserRoleMapper {

    /**
     * Inserts a new user-role relationship.
     *
     * @param userRole the user-role relationship to insert
     * @return the number of affected rows
     */
    int insertUserRole(UserRole userRole);

    /**
     * Deletes user-role relationships by user ID.
     *
     * @param userId the user ID
     * @return the number of affected rows
     */
    int deleteUserRoleByUserId(@Param("userId") String userId);

    /**
     * Deletes user-role relationships by role ID.
     *
     * @param roleId the role ID
     * @return the number of affected rows
     */
    int deleteUserRoleByRoleId(@Param("roleId") String roleId);

    /**
     * Finds user-role relationships by user ID.
     *
     * @param userId the user ID
     * @return list of user-role relationships
     */
    List<UserRole> findUserRolesByUserId(@Param("userId") String userId);
}
