/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.config;

import com.yaowenltd.projectinfomationmanage.common.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.util.ServletRequestPathUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * {@link WebMvcConfig} 拦截路径的匹配测试.
 * <p>
 * 锁定一个非常容易踩错的语义：<strong>拦截器的 path pattern 匹配的是
 * "去掉 context-path 之后" 的 lookup path，不是浏览器地址栏里的完整路径。</strong>
 * </p>
 * <p>
 * 本服务 {@code server.servlet.context-path=/design}，外部访问
 * {@code /design/v1/chat/completions}，但 DispatcherServlet 拿到的 lookup path 是
 * {@code /v1/chat/completions}。若把 pattern 写成 {@code /design/v1/**}，
 * 拦截器<strong>一次都不会触发</strong> —— 表现为鉴权形同虚设，且 controller 里
 * {@code request.getAttribute("username")} 恒为 null，最终 UsageRecorder 落库时
 * 报 {@code Column 'user_id' cannot be null}。
 * </p>
 * <p>
 * 测试直接驱动真实的 {@link WebMvcConfig#addInterceptors}，因此改坏配置一定会红。
 * </p>
 *
 * @since 2026-08-05
 */
@DisplayName("WebMvcConfig 拦截路径")
class WebMvcConfigInterceptorPathTests {

    /** 本服务的 servlet context-path，与 application.yml 保持一致. */
    private static final String CONTEXT_PATH = "/design";

    /**
     * 取出 {@link WebMvcConfig} 实际注册的拦截器（带 path pattern）.
     * <p>
     * {@code InterceptorRegistry#getInterceptors()} 是 protected，用匿名子类把它暴露出来，
     * 这样测试读到的就是生产配置本身，而不是在测试里复制一份 pattern。
     * </p>
     */
    private static List<Object> registeredInterceptors() {
        var registry = new InterceptorRegistry() {
            List<Object> expose() {
                return getInterceptors();
            }
        };
        new WebMvcConfig(new AuthInterceptor(mock(JwtUtil.class))).addInterceptors(registry);
        return registry.expose();
    }

    /**
     * 构造一个带 context-path 的请求，模拟 Tomcat 的真实行为.
     *
     * @param externalUri 外部完整路径，如 {@code /design/v1/chat/completions}
     */
    private static MockHttpServletRequest requestFor(String externalUri) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", externalUri);
        request.setContextPath(CONTEXT_PATH);
        request.setRequestURI(externalUri);
        // DispatcherServlet 在分发前会做这一步：解析 RequestPath 并剥离 context-path。
        // 不调用的话 MappedInterceptor#matches 拿不到 lookup path 会直接抛异常。
        ServletRequestPathUtils.parseAndCache(request);
        return request;
    }

    private static boolean isIntercepted(String externalUri) {
        MockHttpServletRequest request = requestFor(externalUri);
        for (Object interceptor : registeredInterceptors()) {
            if (interceptor instanceof MappedInterceptor mapped && mapped.matches(request)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 需要鉴权的路径必须被拦截 —— 这是回归的核心断言.
     */
    @ParameterizedTest(name = "{0} 应被拦截")
    @ValueSource(strings = {
        "/design/v1/chat/completions",
        "/design/v1/embeddings",
        "/design/v1/models",
        "/design/api/auth/me",
        "/design/api/llm/providers",
    })
    @DisplayName("/design/api/** 与 /design/v1/** 都要走 AuthInterceptor")
    void protectedPaths_areIntercepted(String uri) {
        assertThat(isIntercepted(uri))
                .as("拦截器没覆盖 %s —— 鉴权会被绕过，且 request attribute \"username\" 为 null", uri)
                .isTrue();
    }

    /**
     * 非 API 路径不应被拦截，避免 actuator / swagger 被误伤.
     */
    @ParameterizedTest(name = "{0} 不应被拦截")
    @ValueSource(strings = {
        "/design/actuator/health",
        "/design/swagger-ui.html",
        "/design/v3/api-docs",
    })
    @DisplayName("actuator / swagger 不走 AuthInterceptor")
    void publicPaths_areNotIntercepted(String uri) {
        assertThat(isIntercepted(uri))
                .as("%s 被误拦截了", uri)
                .isFalse();
    }

    /**
     * 显式断言 pattern 里不带 context-path 前缀，把"为什么"钉在测试里.
     */
    @Test
    @DisplayName("path pattern 不能带 /design 前缀（context-path 已被 servlet 容器剥离）")
    void patterns_mustNotIncludeContextPath() {
        for (Object interceptor : registeredInterceptors()) {
            assertThat(interceptor)
                    .as("拦截器必须带 path pattern，不能全局生效")
                    .isInstanceOf(MappedInterceptor.class);
            MappedInterceptor mapped = (MappedInterceptor) interceptor;
            for (String pattern : mapped.getPathPatterns()) {
                assertThat(pattern)
                        .as("pattern 匹配的是剥离 context-path 后的 lookup path，不该带 /design")
                        .doesNotStartWith(CONTEXT_PATH + "/");
            }
        }
    }
}
