/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.impl;

import com.yaowenltd.projectinfomationmanage.common.JwtUtil;
import com.yaowenltd.projectinfomationmanage.common.UnauthorizedException;
import com.yaowenltd.projectinfomationmanage.mapper.UserMapper;
import com.yaowenltd.projectinfomationmanage.model.dto.LoginRequest;
import com.yaowenltd.projectinfomationmanage.model.dto.LoginResponse;
import com.yaowenltd.projectinfomationmanage.model.entity.User;
import com.yaowenltd.projectinfomationmanage.service.AuthService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Implementation of AuthService for user authentication.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;

    private final JwtUtil jwtUtil;

    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs an AuthServiceImpl with required dependencies.
     *
     * @param userMapper the user mapper
     * @param jwtUtil    the JWT utility
     */
    public AuthServiceImpl(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Authenticates user and returns JWT token.
     *
     * @param loginRequest the login credentials
     * @return the login response with token
     * @throws UnauthorizedException if credentials are invalid
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
}
