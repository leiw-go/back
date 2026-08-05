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
     * <p><strong>注意 pattern 不要带 {@code /design} 前缀。</strong>
     * 后端全局 servlet context-path 虽然是 {@code /design}，但 context-path 由 servlet 容器
     * 剥离后才交给 DispatcherServlet，拦截器 pattern 匹配的是<strong>剥离之后</strong>的
     * lookup path：外部访问 {@code /design/v1/chat/completions}，这里要写 {@code /v1/**}。
     * </p>
     * <p>
     * 写成 {@code /design/api/**} 会导致拦截器一次都不触发 —— 鉴权形同虚设，且 controller 里
     * {@code request.getAttribute("username")} 恒为 null，落库时报
     * {@code Column 'user_id' cannot be null}。
     * 回归测试见 {@code WebMvcConfigInterceptorPathTests}。
     * </p>
     * <p>
     * 覆盖 {@code /api/**}（业务接口）与 {@code /v1/**}（OpenAI 兼容路径）—— 端用户
     * 通过同一套 JWT 鉴权。TraceId filter 是 Servlet Filter，通过 {@code @Component}
     * 自动注册到主过滤链，不在本注册表里。
     * </p>
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**", "/v1/**");
    }
}
