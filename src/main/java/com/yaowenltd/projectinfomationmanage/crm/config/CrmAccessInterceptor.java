/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.config;

import com.yaowenltd.projectinfomationmanage.common.ForbiddenException;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmAccessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * CRM 路径的权限点拦截器：校验 crm:access，持有才放行.
 *
 * @since 2026-11-08
 */
@Component
public class CrmAccessInterceptor implements HandlerInterceptor {

    /** AuthInterceptor 写入的用户名属性名. */
    private static final String AUTH_ATTR_USERNAME = "username";

    /** 无 CRM 使用权限时的提示文案. */
    public static final String FORBIDDEN_MESSAGE = "无 CRM 使用权限";

    private final CrmAccessService crmAccessService;

    /**
     * 使用指定的服务构造 CrmAccessInterceptor.
     *
     * @param crmAccessService CRM 访问权限判定服务
     */
    public CrmAccessInterceptor(CrmAccessService crmAccessService) {
        this.crmAccessService = crmAccessService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String username = (String) request.getAttribute(AUTH_ATTR_USERNAME);
        if (username == null || !crmAccessService.hasCrmAccess(username)) {
            throw new ForbiddenException(FORBIDDEN_MESSAGE);
        }
        return true;
    }
}
