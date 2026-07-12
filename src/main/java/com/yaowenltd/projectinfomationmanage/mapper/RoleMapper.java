/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.mapper;

import com.yaowenltd.projectinfomationmanage.model.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色数据库操作的 Mapper 接口.
 */
@Mapper
public interface RoleMapper {

    /**
     * 新增一个角色.
     *
     * @param role 待插入的角色
     * @return 受影响的行数
     */
    int insertRole(Role role);

    /**
     * 更新已有角色.
     *
     * @param role 带有更新信息的角色
     * @return 受影响的行数
     */
    int updateRole(Role role);

    /**
     * 根据 ID 删除角色.
     *
     * @param id 角色 ID
     * @return 受影响的行数
     */
    int deleteRoleById(@Param("id") String id);

    /**
     * 根据 ID 查询角色.
     *
     * @param id 角色 ID
     * @return 角色实体，未找到返回 null
     */
    Role findRoleById(@Param("id") String id);

    /**
     * 查询所有角色.
     *
     * @return 角色列表
     */
    List<Role> findAllRoles();

    /**
     * 根据用户 ID 查询角色列表.
     *
     * @param userId 用户 ID
     * @return 分配给该用户的角色列表
     */
    List<Role> findRolesByUserId(@Param("userId") String userId);
}