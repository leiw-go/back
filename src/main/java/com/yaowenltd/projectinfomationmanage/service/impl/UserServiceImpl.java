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
 * Implementation of UserService for user management.
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    private final UserRoleMapper userRoleMapper;

    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs a UserServiceImpl with required dependencies.
     *
     * @param userMapper     the user mapper
     * @param userRoleMapper the user-role mapper
     */
    public UserServiceImpl(UserMapper userMapper, UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Creates a new user with an encrypted password.
     *
     * @param userDto the user data
     * @return the created user
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
     * Updates an existing user.
     *
     * @param userDto the user data with updates
     * @return the updated user
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
     * Deletes a user by ID.
     *
     * @param id the user ID
     */
    @Override
    @Transactional
    public void deleteUser(String id) {
        userRoleMapper.deleteUserRoleByUserId(id);
        userMapper.deleteUserById(id);
    }

    /**
     * Finds a user by ID.
     *
     * @param id the user ID
     * @return the user data
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
     * Returns all users.
     *
     * @return list of all users
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
