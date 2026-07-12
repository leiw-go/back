/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.impl;

import com.yaowenltd.projectinfomationmanage.mapper.UserMapper;
import com.yaowenltd.projectinfomationmanage.mapper.UserRoleMapper;
import com.yaowenltd.projectinfomationmanage.model.dto.UserDto;
import com.yaowenltd.projectinfomationmanage.model.entity.User;
import com.yaowenltd.projectinfomationmanage.model.entity.UserRole;
import com.yaowenltd.projectinfomationmanage.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 用户管理的 Spring 实现.
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final UserRoleMapper userRoleMapper;

    private final PasswordEncoder passwordEncoder;

    /**
     * 构造 UserServiceImpl，注入所需依赖.
     *
     * @param userMapper     用户 Mapper
     * @param userRoleMapper 用户-角色 Mapper
     */
    public UserServiceImpl(UserMapper userMapper, UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * 创建新用户，并对密码进行加密.
     *
     * @param userDto 用户数据
     * @return 已创建的用户
     */
    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        User user = new User();
        String id = UUID.randomUUID().toString();
        user.setId(id);
        user.setUsername(userDto.getUsername());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setRealName(userDto.getRealName());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhone());
        user.setStatus(userDto.getStatus() != null ? userDto.getStatus() : 1);
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);

        userMapper.insertUser(user);

        if (userDto.getRoleIds() != null) {
            for (String roleId : userDto.getRoleIds()) {
                UserRole userRole = new UserRole();
                userRole.setId(UUID.randomUUID().toString());
                userRole.setUserId(id);
                userRole.setRoleId(roleId);
                userRole.setCreateTime(now);
                userRoleMapper.insertUserRole(userRole);
            }
        }

        userDto.setId(id);
        userDto.setPassword(null);
        userDto.setCreateTime(now);
        userDto.setUpdateTime(now);
        return userDto;
    }

    /**
     * 更新已存在的用户.
     *
     * @param userDto 包含更新字段的用户数据
     * @return 更新后的用户
     */
    @Override
    @Transactional
    public UserDto updateUser(UserDto userDto) {
        User existingUser = userMapper.findUserById(userDto.getId());
        if (existingUser == null) {
            throw new IllegalArgumentException("user not found");
        }

        User user = new User();
        user.setId(userDto.getId());
        user.setUsername(userDto.getUsername());
        if (userDto.getPassword() != null && !userDto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }
        user.setRealName(userDto.getRealName());
        user.setEmail(userDto.getEmail());
        user.setPhone(userDto.getPhone());
        user.setStatus(userDto.getStatus());

        userMapper.updateUser(user);

        if (userDto.getRoleIds() != null) {
            userRoleMapper.deleteUserRoleByUserId(userDto.getId());
            LocalDateTime now = LocalDateTime.now();
            for (String roleId : userDto.getRoleIds()) {
                UserRole userRole = new UserRole();
                userRole.setId(UUID.randomUUID().toString());
                userRole.setUserId(userDto.getId());
                userRole.setRoleId(roleId);
                userRole.setCreateTime(now);
                userRoleMapper.insertUserRole(userRole);
            }
        }

        userDto.setPassword(null);
        return userDto;
    }

    /**
     * 根据 ID 删除用户.
     *
     * @param id 用户 ID
     */
    @Override
    @Transactional
    public void deleteUser(String id) {
        userRoleMapper.deleteUserRoleByUserId(id);
        userMapper.deleteUserById(id);
    }

    /**
     * 根据 ID 查找用户.
     *
     * @param id 用户 ID
     * @return 用户数据
     */
    @Override
    public UserDto findUserById(String id) {
        User user = userMapper.findUserById(id);
        if (user == null) {
            throw new IllegalArgumentException("user not found");
        }
        return convertToDto(user);
    }

    /**
     * 返回所有用户.
     *
     * @return 所有用户列表
     */
    @Override
    public List<UserDto> findAllUsers() {
        List<User> users = userMapper.findAllUsers();
        List<UserDto> userDtos = new ArrayList<>();
        for (User user : users) {
            userDtos.add(convertToDto(user));
        }
        return userDtos;
    }

    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRealName(user.getRealName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setStatus(user.getStatus());
        dto.setCreateTime(user.getCreateTime());
        dto.setUpdateTime(user.getUpdateTime());

        List<UserRole> userRoles = userRoleMapper.findUserRolesByUserId(user.getId());
        List<String> roleIds = new ArrayList<>();
        for (UserRole userRole : userRoles) {
            roleIds.add(userRole.getRoleId());
        }
        dto.setRoleIds(roleIds);

        return dto;
    }
}