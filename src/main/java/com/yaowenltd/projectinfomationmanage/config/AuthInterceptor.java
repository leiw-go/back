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
 * Interceptor for authenticating and authorizing API requests using JWT tokens.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthInterceptor.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String BEARER_PREFIX = "Bearer ";

    private static final String AUTH_ATTR_USERNAME = "username";

    private final JwtUtil jwtUtil;

    /**
     * Constructs an AuthInterceptor with the given JwtUtil.
     *
     * @param jwtUtil the JWT utility for token validation
     */
    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Pre-handles the request by validating the JWT token.
     *
     * @param request  the HTTP request
     * @param response the HTTP response
     * @param handler  the handler object
     * @return true if the request should proceed, false otherwise
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
