/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 出站 HTTP 客户端配置.
 * <p>
 * 提供一个供内部 HTTP 调用（例如飞书 webhook 推送）共用的 {@link RestTemplate} Bean.
 * </p>
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 构建共享的 RestTemplate，使用固定超时（5 秒）.
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
}