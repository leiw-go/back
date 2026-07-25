/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 用于注册拦截器的 Web MVC 配置.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    /**
     * 使用指定的 AuthInterceptor 构造 WebMvcConfig.
     *
     * @param authInterceptor 认证拦截器
     */
    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    /**
     * 为所有 API 路径注册认证拦截器.
     * <p>注意:后端全局 servlet context-path 为 {@code /design},
     * 拦截路径必须与外部访问路径保持一致,即 {@code /design/api/**}.
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/design/api/**");
    }
}
