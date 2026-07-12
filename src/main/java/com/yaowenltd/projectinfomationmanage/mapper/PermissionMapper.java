/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.mapper;

import com.yaowenltd.projectinfomationmanage.model.entity.Permission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 权限数据库操作的 Mapper 接口.
 */
@Mapper
public interface PermissionMapper {

    /**
     * 新增一个权限.
     *
     * @param permission 待插入的权限
     * @return 受影响的行数
     */
    int insertPermission(Permission permission);

    /**
     * 根据 ID 删除权限.
     *
     * @param id 权限 ID
     * @return 受影响的行数
     */
    int deletePermissionById(@Param("id") String id);

    /**
     * 根据 ID 查询权限.
     *
     * @param id 权限 ID
     * @return 权限实体，未找到返回 null
     */
    Permission findPermissionById(@Param("id") String id);

    /**
     * 查询所有权限.
     *
     * @return 权限列表
     */
    List<Permission> findAllPermissions();

    /**
     * 根据角色 ID 查询权限列表.
     *
     * @param roleId 角色 ID
     * @return 分配给该角色的权限列表
     */
    List<Permission> findPermissionsByRoleId(@Param("roleId") String roleId);
}