/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * LLM 出站请求注入 {@code traceparent} header 的 RestTemplate 拦截器.
 * <p>
 * 走 W3C Trace Context 规范：用 SDK 自带的 {@link io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator}
 * 把当前 {@link Context} 序列化为 {@code traceparent} header，让上游 provider 的日志 / 监控能与本服务串联。
 * </p>
 *
 * @since 2026-08-04
 */
public class TraceparentInjectingInterceptor implements ClientHttpRequestInterceptor {

    private final TextMapPropagator propagator;

    /**
     * 使用给定的 OpenTelemetry SDK 构造拦截器.
     *
     * @param openTelemetry OpenTelemetry SDK 实例
     */
    public TraceparentInjectingInterceptor(OpenTelemetry openTelemetry) {
        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
    }

    /**
     * 拦截 HTTP 请求，把当前 Context 注入 {@code traceparent} header.
     *
     * @param request   当前请求
     * @param body      请求体
     * @param execution 执行链
     * @return 响应
     * @throws IOException IO 异常
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request,
                                        byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        if (propagator != null) {
            propagator.inject(Context.current(), request, REQUEST_HEADER_SETTER);
        }
        return execution.execute(request, body);
    }

    /** 把 OpenTelemetry 的 setter 适配到 Spring 的 HttpRequest. */
    private static final TextMapSetter<HttpRequest> REQUEST_HEADER_SETTER =
            (carrier, key, value) -> carrier.getHeaders().set(key, value);
}