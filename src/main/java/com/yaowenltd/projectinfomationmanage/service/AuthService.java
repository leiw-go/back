/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service;

import com.yaowenltd.projectinfomationmanage.model.dto.CurrentUserResponse;
import com.yaowenltd.projectinfomationmanage.model.dto.LoginRequest;
import com.yaowenltd.projectinfomationmanage.model.dto.LoginResponse;
import com.yaowenltd.projectinfomationmanage.model.dto.RegisterRequest;
import com.yaowenltd.projectinfomationmanage.model.dto.RegisterResponse;

/**
 * Service interface for authentication operations.
 */
public interface AuthService {

    /**
     * Registers a new user.
     *
     * @param registerRequest the registration data
     * @return the registration response
     */
    RegisterResponse register(RegisterRequest registerRequest);

    /**
     * Authenticates a user with username and password.
     *
     * @param loginRequest the login credentials
     * @return the login response containing token and user info
     */
    LoginResponse login(LoginRequest loginRequest);

    /**
     * Retrieves the current authenticated user's information.
     *
     * @param username the username from the JWT token
     * @return the current user response containing user details and role
     */
    CurrentUserResponse getCurrentUser(String username);
}
