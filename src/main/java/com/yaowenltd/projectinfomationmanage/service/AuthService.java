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
 * 服务接口，定义认证业务能力.
 */
public interface AuthService {

    /**
     * 注册新用户.
     *
     * @param registerRequest 注册数据
     * @return 注册响应
     */
    RegisterResponse register(RegisterRequest registerRequest);

    /**
     * 使用用户名和密码认证用户.
     *
     * @param loginRequest 登录凭证
     * @return 包含令牌与用户信息的登录响应
     */
    LoginResponse login(LoginRequest loginRequest);

    /**
     * 获取当前已认证用户的信息.
     *
     * @param username 来自 JWT 令牌的用户名
     * @return 包含用户详情与角色的当前用户响应
     */
    CurrentUserResponse getCurrentUser(String username);
}