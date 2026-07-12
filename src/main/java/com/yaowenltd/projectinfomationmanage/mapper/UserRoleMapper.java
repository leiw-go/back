/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.mapper;

import com.yaowenltd.projectinfomationmanage.model.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户角色关系数据库操作的 Mapper 接口.
 */
@Mapper
public interface UserRoleMapper {

    /**
     * 新增一条用户角色关系.
     *
     * @param userRole 待插入的用户角色关系
     * @return 受影响的行数
     */
    int insertUserRole(UserRole userRole);

    /**
     * 根据用户 ID 删除用户角色关系.
     *
     * @param userId 用户 ID
     * @return 受影响的行数
     */
    int deleteUserRoleByUserId(@Param("userId") String userId);

    /**
     * 根据角色 ID 删除用户角色关系.
     *
     * @param roleId 角色 ID
     * @return 受影响的行数
     */
    int deleteUserRoleByRoleId(@Param("roleId") String roleId);

    /**
     * 根据用户 ID 查询用户角色关系.
     *
     * @param userId 用户 ID
     * @return 用户角色关系列表
     */
    List<UserRole> findUserRolesByUserId(@Param("userId") String userId);
}