/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.mapper;

import com.yaowenltd.projectinfomationmanage.model.entity.RolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色权限关系数据库操作的 Mapper 接口.
 */
@Mapper
public interface RolePermissionMapper {

    /**
     * 新增一条角色权限关系.
     *
     * @param rolePermission 待插入的角色权限关系
     * @return 受影响的行数
     */
    int insertRolePermission(RolePermission rolePermission);

    /**
     * 根据角色 ID 删除角色权限关系.
     *
     * @param roleId 角色 ID
     * @return 受影响的行数
     */
    int deleteRolePermissionByRoleId(@Param("roleId") String roleId);

    /**
     * 根据角色 ID 查询角色权限关系.
     *
     * @param roleId 角色 ID
     * @return 角色权限关系列表
     */
    List<RolePermission> findRolePermissionsByRoleId(@Param("roleId") String roleId);
}