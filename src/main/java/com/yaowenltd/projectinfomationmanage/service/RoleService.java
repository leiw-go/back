/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service;

import com.yaowenltd.projectinfomationmanage.model.dto.RoleDto;
import com.yaowenltd.projectinfomationmanage.model.entity.Permission;

import java.util.List;

/**
 * 服务接口，定义角色与权限管理业务能力.
 */
public interface RoleService {

    /**
     * 创建新角色.
     *
     * @param roleDto 角色数据
     * @return 已创建的角色
     */
    RoleDto createRole(RoleDto roleDto);

    /**
     * 更新已存在的角色.
     *
     * @param roleDto 包含更新字段的角色数据
     * @return 更新后的角色
     */
    RoleDto updateRole(RoleDto roleDto);

    /**
     * 根据 ID 删除角色.
     *
     * @param id 角色 ID
     */
    void deleteRole(String id);

    /**
     * 根据 ID 查找角色.
     *
     * @param id 角色 ID
     * @return 角色数据
     */
    RoleDto findRoleById(String id);

    /**
     * 返回所有角色.
     *
     * @return 所有角色列表
     */
    List<RoleDto> findAllRoles();

    /**
     * 返回所有权限.
     *
     * @return 所有权限列表
     */
    List<Permission> findAllPermissions();
}