/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * 出站 HTTP 客户端配置.
 * <p>
 * 提供一个由 akshare 集成（以及后续任何内部 HTTP 调用）共用的 {@link RestTemplate} Bean.
 * 连接 / 读取超时从 {@link AkshareServiceProperties} 读取，便于在 Nacos 中在线调整而无需重启.
 * </p>
 *
 * <p>
 * <strong>为何在 Bean 方法上使用 {@link RefreshScope}</strong>:
 * 底层的 {@link org.springframework.http.client.ClientHttpRequestFactory}
 * / Apache HttpClient 在构造期就将超时写死了. 仅刷新 {@link AkshareServiceProperties}
 * 并不够 —— 为了使新的超时生效，必须重新实例化 RestTemplate 本身. 这是我们能承受的
 * 最轻量的重建方式（每次刷新仅重建一个 RestTemplate）.
 * </p>
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 构建共享的 RestTemplate.
     *
     * @param builder    Spring Boot 自动配置的构建器
     * @param properties akshare 集成的实时配置属性；该 Bean 是 {@code @RefreshScope}
     *                   代理，因此对 {@code akshare.service.timeout-ms} 的任何 Nacos
     *                   端变更都会在 RestTemplate 自身被重建时（同样由刷新触发）被拾取.
     * @return 配置了合理超时的 RestTemplate
     */
    @Bean
    @RefreshScope
    public RestTemplate restTemplate(RestTemplateBuilder builder,
                                     AkshareServiceProperties properties) {
        long timeoutMs = properties.getTimeoutMs();
        return builder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }
}
