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
 * 提供一个供飞书机器人 webhook 等内部 HTTP 调用共用的 {@link RestTemplate} Bean.
 * 连接 / 读取超时使用固定默认值；若将来需要从 Nacos 配置中读取，可把本 Bean 改回
 * {@code @RefreshScope} 并注入相关配置属性.
 * </p>
 */
@Configuration
public class RestTemplateConfig {

    /**
     * RestTemplate 的连接 / 读取超时（毫秒）.
     * 飞书 webhook 同步响应通常很快，5 秒足够覆盖；如有更慢的调用方, 请按调用方各自
     * 调整（例如在调用方法上自行 wrap ClientHttpRequestFactory）.
     */
    private static final long DEFAULT_TIMEOUT_MS = 5000L;

    /**
     * 构建共享的 RestTemplate，并将默认超时应用到连接 / 读取阶段.
     *
     * @param builder Spring Boot 自动配置的构建器
     * @return 配置了合理超时的 RestTemplate
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofMillis(DEFAULT_TIMEOUT_MS))
                .setReadTimeout(Duration.ofMillis(DEFAULT_TIMEOUT_MS))
                .build();
    }
}
