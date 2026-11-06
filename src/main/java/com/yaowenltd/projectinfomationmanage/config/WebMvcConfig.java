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
    }
}
