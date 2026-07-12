/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.impl;

import com.yaowenltd.projectinfomationmanage.common.JwtUtil;
import com.yaowenltd.projectinfomationmanage.common.UnauthorizedException;
import com.yaowenltd.projectinfomationmanage.mapper.RoleMapper;
import com.yaowenltd.projectinfomationmanage.mapper.UserMapper;
import com.yaowenltd.projectinfomationmanage.mapper.UserRoleMapper;
import com.yaowenltd.projectinfomationmanage.model.dto.CurrentUserResponse;
import com.yaowenltd.projectinfomationmanage.model.dto.LoginRequest;
import com.yaowenltd.projectinfomationmanage.model.dto.LoginResponse;
import com.yaowenltd.projectinfomationmanage.model.dto.RegisterRequest;
import com.yaowenltd.projectinfomationmanage.model.dto.RegisterResponse;
import com.yaowenltd.projectinfomationmanage.model.entity.Role;
import com.yaowenltd.projectinfomationmanage.model.entity.User;
import com.yaowenltd.projectinfomationmanage.model.entity.UserRole;
import com.yaowenltd.projectinfomationmanage.service.AuthService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 用户认证的 Spring 实现.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_USER_ROLE_ID = "550e8400-e29b-41d4-a716-446655440102";

    private final UserMapper userMapper;

    private final RoleMapper roleMapper;

    private final UserRoleMapper userRoleMapper;

    private final JwtUtil jwtUtil;

    private final PasswordEncoder passwordEncoder;

    /**
     * 构造 AuthServiceImpl，注入所需依赖.
     *
     * @param userMapper     用户 Mapper
     * @param roleMapper     角色 Mapper
     * @param userRoleMapper 用户-角色 Mapper
     * @param jwtUtil        JWT 工具类
     */
    public AuthServiceImpl(UserMapper userMapper, RoleMapper roleMapper,
                           UserRoleMapper userRoleMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * 注册新用户，并赋予默认 USER 角色.
     *
     * @param registerRequest 注册数据
     * @return 注册响应
     */
    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest registerRequest) {
        User existingUser = userMapper.findUserByUsername(registerRequest.getUsername());
        if (existingUser != null) {
            throw new IllegalArgumentException("username already exists");
        }

        User user = new User();
        String userId = UUID.randomUUID().toString();
        user.setId(userId);
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRealName(registerRequest.getRealName());
        user.setEmail(registerRequest.getEmail());
        user.setPhone(registerRequest.getPhone());
        user.setStatus(1);
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);

        userMapper.insertUser(user);

        Role userRole = roleMapper.findRoleById(DEFAULT_USER_ROLE_ID);
        if (userRole != null) {
            UserRole ur = new UserRole();
            ur.setId(UUID.randomUUID().toString());
            ur.setUserId(userId);
            ur.setRoleId(DEFAULT_USER_ROLE_ID);
            ur.setCreateTime(now);
            userRoleMapper.insertUserRole(ur);
        }

        RegisterResponse response = new RegisterResponse();
        response.setId(userId);
        response.setUsername(registerRequest.getUsername());
        response.setRealName(registerRequest.getRealName());
        response.setMessage("user registered successfully");
        return response;
    }

    /**
     * 认证用户并返回 JWT 令牌.
     *
     * @param loginRequest 登录凭证
     * @return 包含令牌的登录响应
     * @throws UnauthorizedException 当凭证无效时
     */
    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        User user = userMapper.findUserByUsername(loginRequest.getUsername());
        if (user == null) {
            throw new UnauthorizedException("username or password is incorrect");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new UnauthorizedException("username or password is incorrect");
        }

        String token = jwtUtil.generateToken(user.getUsername());
        return new LoginResponse(token, user.getUsername(), user.getRealName());
    }

    /**
     * 获取当前已认证用户的信息，包括其角色编码.
     *
     * @param username 来自 JWT 令牌的用户名
     * @return 当前用户响应
     * @throws UnauthorizedException 当未找到用户时
     */
    @Override
    public CurrentUserResponse getCurrentUser(String username) {
        User user = userMapper.findUserByUsername(username);
        if (user == null) {
            throw new UnauthorizedException("user not found");
        }

        List<Role> roles = roleMapper.findRolesByUserId(user.getId());
        String roleCode = null;
        if (roles != null && !roles.isEmpty()) {
            roleCode = roles.get(0).getRoleCode();
        }

        CurrentUserResponse response = new CurrentUserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRoleCode(roleCode);
        response.setRealName(user.getRealName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setStatus(user.getStatus());
        return response;
    }
}