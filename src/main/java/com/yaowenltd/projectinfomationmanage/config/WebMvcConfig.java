/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.config;

import com.yaowenltd.projectinfomationmanage.crm.config.CrmAccessInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 用于注册拦截器的 Web MVC 配置.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    private final CrmAccessInterceptor crmAccessInterceptor;

    /**
     * 使用指定的拦截器构造 WebMvcConfig.
     *
     * @param authInterceptor      认证拦截器
     * @param crmAccessInterceptor CRM 权限点拦截器
     */
    public WebMvcConfig(AuthInterceptor authInterceptor, CrmAccessInterceptor crmAccessInterceptor) {
        this.authInterceptor = authInterceptor;
        this.crmAccessInterceptor = crmAccessInterceptor;
    }

    /**
     * 为所有 API 路径注册认证拦截器.
     * <p>注意:后端全局 servlet context-path 为 {@code /design},该前缀会在进入
     * DispatcherServlet 之前被剥离,因此拦截器匹配的是<b>去掉 context-path 之后</b>
     * 的路径,即 {@code /api/**}. 外部访问地址虽然写作 {@code /design/api/xxx},
     * 但此处若写 {@code /design/api/**} 则永远匹配不上,拦截器会静默失效.
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**");
        // CRM 路径额外校验 crm:access 权限点（必须排在 AuthInterceptor 之后，
        // 这样 request 里已经有 username 属性）
        registry.addInterceptor(crmAccessInterceptor)
                .addPathPatterns("/api/crm/**");
    }
}
