/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.config;

import io.opentelemetry.api.OpenTelemetry;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;

/**
 * 出站 HTTP 客户端配置.
 * <p>
 * 提供两类 {@link RestTemplate} Bean：
 * </p>
 * <ul>
 *   <li>{@code restTemplate} —— 短超时（5 秒），供飞书 webhook 等内部短链路使用</li>
 *   <li>{@code llmRestTemplate} —— 长超时（默认 60 秒），并接入 OpenTelemetry 上下文注入
 *       （下游 LLM 上游会被加上 {@code traceparent} header，沿用本服务的 traceId），
 *       供 LLM 中转调用上游 provider 使用</li>
 * </ul>
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 共享的 RestTemplate，固定 5 秒超时（飞书 webhook 等内部短链路用）.
     *
     * @param builder Spring Boot 自动配置的构建器
     * @return 配置了合理超时的 RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    /**
     * LLM 中专用的 RestTemplate：长超时 + 注入 traceparent header.
     *
     * @param builder           Spring Boot 自动配置的构建器
     * @param openTelemetry     OpenTelemetry SDK（用于在出站请求里塞 traceparent）
     * @param llmProperties     LLM 配置（读 {@code llm.upstream.timeout-ms}）
     * @return 配置了长超时与 trace 注入拦截器的 RestTemplate
     */
    @Bean
    public RestTemplate llmRestTemplate(RestTemplateBuilder builder,
                                        OpenTelemetry openTelemetry,
                                        LlmProperties llmProperties) {
        ClientHttpRequestInterceptor traceparentInterceptor =
                new TraceparentInjectingInterceptor(openTelemetry);
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofMillis(llmProperties.getUpstream().getTimeoutMs()))
                .additionalInterceptors(Collections.singletonList(traceparentInterceptor))
                .build();
    }
}