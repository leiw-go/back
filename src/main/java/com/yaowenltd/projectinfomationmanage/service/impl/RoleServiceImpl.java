/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.impl;

import com.yaowenltd.projectinfomationmanage.mapper.PermissionMapper;
import com.yaowenltd.projectinfomationmanage.mapper.RoleMapper;
import com.yaowenltd.projectinfomationmanage.mapper.RolePermissionMapper;
import com.yaowenltd.projectinfomationmanage.mapper.UserRoleMapper;
import com.yaowenltd.projectinfomationmanage.model.dto.RoleDto;
import com.yaowenltd.projectinfomationmanage.model.entity.Permission;
import com.yaowenltd.projectinfomationmanage.model.entity.Role;
import com.yaowenltd.projectinfomationmanage.model.entity.RolePermission;
import com.yaowenltd.projectinfomationmanage.service.RoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 角色与权限管理的 Spring 实现.
 */
@Service
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;

    private final PermissionMapper permissionMapper;

    private final RolePermissionMapper rolePermissionMapper;

    private final UserRoleMapper userRoleMapper;

    /**
     * 构造 RoleServiceImpl，注入所需依赖.
     *
     * @param roleMapper           角色 Mapper
     * @param permissionMapper     权限 Mapper
     * @param rolePermissionMapper 角色-权限 Mapper
     * @param userRoleMapper       用户-角色 Mapper
     */
    public RoleServiceImpl(RoleMapper roleMapper, PermissionMapper permissionMapper,
                           RolePermissionMapper rolePermissionMapper, UserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userRoleMapper = userRoleMapper;
    }

    /**
     * 创建新角色并绑定权限.
     *
     * @param roleDto 角色数据
     * @return 已创建的角色
     */
    @Override
    @Transactional
    public RoleDto createRole(RoleDto roleDto) {
        Role role = new Role();
        String id = UUID.randomUUID().toString();
        role.setId(id);
        role.setRoleName(roleDto.getRoleName());
        role.setRoleCode(roleDto.getRoleCode());
        role.setDescription(roleDto.getDescription());
        role.setStatus(roleDto.getStatus() != null ? roleDto.getStatus() : 1);
        LocalDateTime now = LocalDateTime.now();
        role.setCreateTime(now);
        role.setUpdateTime(now);

        roleMapper.insertRole(role);

        if (roleDto.getPermissionIds() != null) {
            for (String permissionId : roleDto.getPermissionIds()) {
                RolePermission rp = new RolePermission();
                rp.setId(UUID.randomUUID().toString());
                rp.setRoleId(id);
                rp.setPermissionId(permissionId);
                rp.setCreateTime(now);
                rolePermissionMapper.insertRolePermission(rp);
            }
        }

        roleDto.setId(id);
        roleDto.setCreateTime(now);
        roleDto.setUpdateTime(now);
        return roleDto;
    }

    /**
     * 更新已存在的角色.
     *
     * @param roleDto 包含更新字段的角色数据
     * @return 更新后的角色
     */
    @Override
    @Transactional
    public RoleDto updateRole(RoleDto roleDto) {
        Role existingRole = roleMapper.findRoleById(roleDto.getId());
        if (existingRole == null) {
            throw new IllegalArgumentException("role not found");
        }

        Role role = new Role();
        role.setId(roleDto.getId());
        role.setRoleName(roleDto.getRoleName());
        role.setRoleCode(roleDto.getRoleCode());
        role.setDescription(roleDto.getDescription());
        role.setStatus(roleDto.getStatus());

        roleMapper.updateRole(role);

        if (roleDto.getPermissionIds() != null) {
            rolePermissionMapper.deleteRolePermissionByRoleId(roleDto.getId());
            LocalDateTime now = LocalDateTime.now();
            for (String permissionId : roleDto.getPermissionIds()) {
                RolePermission rp = new RolePermission();
                rp.setId(UUID.randomUUID().toString());
                rp.setRoleId(roleDto.getId());
                rp.setPermissionId(permissionId);
                rp.setCreateTime(now);
                rolePermissionMapper.insertRolePermission(rp);
            }
        }

        return roleDto;
    }

    /**
     * 根据 ID 删除角色.
     *
     * @param id 角色 ID
     */
    @Override
    @Transactional
    public void deleteRole(String id) {
        userRoleMapper.deleteUserRoleByRoleId(id);
        rolePermissionMapper.deleteRolePermissionByRoleId(id);
        roleMapper.deleteRoleById(id);
    }

    /**
     * 根据 ID 查找角色.
     *
     * @param id 角色 ID
     * @return 角色数据
     */
    @Override
    public RoleDto findRoleById(String id) {
        Role role = roleMapper.findRoleById(id);
        if (role == null) {
            throw new IllegalArgumentException("role not found");
        }
        return convertToDto(role);
    }

    /**
     * 返回所有角色.
     *
     * @return 所有角色列表
     */
    @Override
    public List<RoleDto> findAllRoles() {
        List<Role> roles = roleMapper.findAllRoles();
        List<RoleDto> roleDtos = new ArrayList<>();
        for (Role role : roles) {
            roleDtos.add(convertToDto(role));
        }
        return roleDtos;
    }

    /**
     * 返回所有权限.
     *
     * @return 所有权限列表
     */
    @Override
    public List<Permission> findAllPermissions() {
        return permissionMapper.findAllPermissions();
    }

    private RoleDto convertToDto(Role role) {
        RoleDto dto = new RoleDto();
        dto.setId(role.getId());
        dto.setRoleName(role.getRoleName());
        dto.setRoleCode(role.getRoleCode());
        dto.setDescription(role.getDescription());
        dto.setStatus(role.getStatus());
        dto.setCreateTime(role.getCreateTime());
        dto.setUpdateTime(role.getUpdateTime());

        List<RolePermission> rps = rolePermissionMapper.findRolePermissionsByRoleId(role.getId());
        List<String> permissionIds = new ArrayList<>();
        for (RolePermission rp : rps) {
            permissionIds.add(rp.getPermissionId());
        }
        dto.setPermissionIds(permissionIds);

        return dto;
    }
}