/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.common;

import com.yaowenltd.projectinfomationmanage.common.UnauthorizedException;
import com.yaowenltd.projectinfomationmanage.mapper.UserMapper;
import com.yaowenltd.projectinfomationmanage.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * 当前登录用户解析器.
 * <p>
 * {@code AuthInterceptor} 只把**用户名**写进 request 属性，
 * 而 CRM 域的 {@code owner_id} / {@code receiver_id} 存的是 {@code t_user.id}，
 * 这里统一做一次用户名 → 用户 ID 的解析。
 *
 * @since 2026-11-08
 */
@Component
public class CurrentUserResolver {

    /** AuthInterceptor 写入的用户名属性名. */
    public static final String AUTH_ATTR_USERNAME = "username";

    private final UserMapper userMapper;

    /**
     * 使用指定的 Mapper 构造 CurrentUserResolver.
     *
     * @param userMapper 用户 Mapper
     */
    public CurrentUserResolver(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 取当前登录用户的 ID.
     *
     * @param request HTTP 请求
     * @return 用户 ID
     */
    public String currentUserId(HttpServletRequest request) {
        return requireUser(request).getId();
    }

    /**
     * 取当前登录用户的用户名.
     *
     * @param request HTTP 请求
     * @return 用户名
     */
    public String currentUsername(HttpServletRequest request) {
        return requireUser(request).getUsername();
    }

    /**
     * 取当前登录用户；未登录或用户不存在时抛 401.
     */
    private User requireUser(HttpServletRequest request) {
        Object username = request == null ? null : request.getAttribute(AUTH_ATTR_USERNAME);
        if (username == null) {
            throw new UnauthorizedException("no permission to call");
        }
        User user = userMapper.selectByUsername(username.toString());
        if (user == null) {
            throw new UnauthorizedException("no permission to call");
        }
        return user;
    }

}
