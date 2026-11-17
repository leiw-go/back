/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry SDK 装配.
 * <p>
 * 手动装配 {@link OpenTelemetrySdk}（不依赖 {@code opentelemetry-spring-boot-starter}）：
 * </p>
 * <ul>
 *   <li>资源属性固定为 service.name=ProjectInfomationManage + service.version 取自
 *       {@code Package.getImplementationVersion()}（jar 里有 manifest 时才填）</li>
 *   <li>{@code llm.trace.exporter=logging} 时挂 {@link LoggingSpanExporter}，
 *       否则不挂任何 exporter（in-memory，仅供 logback MDC 取 trace_id 用）</li>
 *   <li>Bean 销毁时 {@code SdkTracerProvider#close()} 释放线程池</li>
 * </ul>
 *
 * @since 2026-08-04
 */
@Configuration
public class OpenTelemetryConfig {

    private final LlmProperties llmProperties;

    private OpenTelemetrySdk openTelemetrySdk;

    /**
     * 使用给定的 LLM 配置构造 OpenTelemetryConfig.
     *
     * @param llmProperties LLM 模块配置（用于决定 span exporter）
     */
    public OpenTelemetryConfig(LlmProperties llmProperties) {
        this.llmProperties = llmProperties;
    }

    /**
     * 构造并暴露 OpenTelemetry SDK Bean.
     *
     * @return 装配好的 OpenTelemetry 实例
     */
    @Bean
    public OpenTelemetry openTelemetry() {
        Resource resource = Resource.getDefault().merge(Resource.create(
                Attributes.builder()
                        .put(AttributeKey.stringKey("service.name"), "ProjectInfomationManage")
                        .put(AttributeKey.stringKey("service.version"),
                                resolveServiceVersion())
                        .build()));

        SdkTracerProviderBuilder providerBuilder =
                SdkTracerProvider.builder().setResource(resource);

        SpanProcessor processor = buildSpanProcessor();
        if (processor != null) {
            providerBuilder.addSpanProcessor(processor);
        }

        SdkTracerProvider tracerProvider = providerBuilder.build();

        this.openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                // W3C TraceContext 传播器：出站 HTTP 自动注入 traceparent header，
                // 入站 HTTP 自动提取 traceparent（TraceIdFilter 不需要手动解析）。
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
        return this.openTelemetrySdk;
    }

    /**
     * 解析服务版本号（jar manifest 里的 Implementation-Version；读不到时回退到 "unknown"）.
     *
     * @return 服务版本字符串
     */
    private static String resolveServiceVersion() {
        String version = OpenTelemetryConfig.class.getPackage().getImplementationVersion();
        return version == null || version.isBlank() ? "unknown" : version;
    }

    /**
     * 根据 {@code llm.trace.exporter} 决定挂哪种 span processor.
     * <ul>
     *   <li>{@code logging} → {@link SimpleSpanProcessor} + {@link LoggingSpanExporter}（dev 友好）</li>
     *   <li>其它 → null（in-memory）</li>
     * </ul>
     *
     * @return span processor；null 表示不挂 exporter
     */
    private SpanProcessor buildSpanProcessor() {
        String exporter = llmProperties.getTrace().getExporter();
        if ("logging".equalsIgnoreCase(exporter)) {
            SpanExporter logging = LoggingSpanExporter.create();
            return SimpleSpanProcessor.create(logging);
        }
        // 生产推荐 BatchSpanProcessor + OTLP；本 spec 首版不发外部，留扩展点
        // 如需 OTLP：return BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder()...).build();
        return null;
    }

    /**
     * 应用关闭时关闭 tracer provider，释放内部线程池.
     */
    @PreDestroy
    public void shutdown() {
        if (openTelemetrySdk != null) {
            openTelemetrySdk.getSdkTracerProvider().shutdown();
        }
    }
}