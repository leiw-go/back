/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.config;

import com.yaowenltd.projectinfomationmanage.common.JwtUtil;
import com.yaowenltd.projectinfomationmanage.common.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

/**
 * 基于 JWT 令牌对 API 请求进行认证与授权的拦截器.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthInterceptor.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String BEARER_PREFIX = "Bearer ";

    private static final String AUTH_ATTR_USERNAME = "username";

    private final JwtUtil jwtUtil;

    /**
     * 使用指定的 JwtUtil 构造 AuthInterceptor.
     *
     * @param jwtUtil 用于令牌校验的 JWT 工具
     */
    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * 通过校验 JWT 令牌对请求进行前置处理.
     *
     * @param request  HTTP 请求
     * @param response HTTP 响应
     * @param handler  处理器对象
     * @return 请求应继续执行时返回 true，否则返回 false
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        Method method = handlerMethod.getMethod();

        if (method.isAnnotationPresent(SkipAuth.class)) {
            return true;
        }

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            LOGGER.warn("Missing or invalid authorization header for request: {}", request.getRequestURI());
            throw new UnauthorizedException("no permission to call");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!jwtUtil.validateToken(token)) {
            LOGGER.warn("Invalid or expired token for request: {}", request.getRequestURI());
            throw new UnauthorizedException("no permission to call");
        }

        String username = jwtUtil.getUsernameFromToken(token);
        request.setAttribute(AUTH_ATTR_USERNAME, username);
        return true;
    }
}
