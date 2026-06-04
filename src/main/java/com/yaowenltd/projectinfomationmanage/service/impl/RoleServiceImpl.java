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
 * Implementation of RoleService for role and permission management.
 */
@Service
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;

    private final PermissionMapper permissionMapper;

    private final RolePermissionMapper rolePermissionMapper;

    private final UserRoleMapper userRoleMapper;

    /**
     * Constructs a RoleServiceImpl with required dependencies.
     *
     * @param roleMapper           the role mapper
     * @param permissionMapper     the permission mapper
     * @param rolePermissionMapper the role-permission mapper
     * @param userRoleMapper       the user-role mapper
     */
    public RoleServiceImpl(RoleMapper roleMapper, PermissionMapper permissionMapper,
                           RolePermissionMapper rolePermissionMapper, UserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.permissionMapper = permissionMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userRoleMapper = userRoleMapper;
    }

    /**
     * Creates a new role with permissions.
     *
     * @param roleDto the role data
     * @return the created role
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
     * Updates an existing role.
     *
     * @param roleDto the role data with updates
     * @return the updated role
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
     * Deletes a role by ID.
     *
     * @param id the role ID
     */
    @Override
    @Transactional
    public void deleteRole(String id) {
        userRoleMapper.deleteUserRoleByRoleId(id);
        rolePermissionMapper.deleteRolePermissionByRoleId(id);
        roleMapper.deleteRoleById(id);
    }

    /**
     * Finds a role by ID.
     *
     * @param id the role ID
     * @return the role data
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
     * Returns all roles.
     *
     * @return list of all roles
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
     * Returns all permissions.
     *
     * @return list of all permissions
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
