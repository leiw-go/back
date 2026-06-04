/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service;

import com.yaowenltd.projectinfomationmanage.model.dto.LoginRequest;
import com.yaowenltd.projectinfomationmanage.model.dto.LoginResponse;

/**
 * Service interface for authentication operations.
 */
public interface AuthService {

    /**
     * Authenticates a user with username and password.
     *
     * @param loginRequest the login credentials
     * @return the login response containing token and user info
     */
    LoginResponse login(LoginRequest loginRequest);
}
