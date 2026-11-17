/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * traceId 全链路追踪 Servlet filter.
 * <p>
 * 作用范围：{@code /design/api/**} 与 {@code /design/v1/**}（actuator / 静态资源 / Nacos 心跳不带 traceId）。
 * </p>
 * <p>
 * 行为：
 * </p>
 * <ul>
 *   <li>进入时启动 root span（{@code http.server.request}），把当前 {@code Span} 设置为当前 Context</li>
 *   <li>把 trace_id / span_id 写入 SLF4J MDC（logback pattern 用 {@code %X{}} 取）</li>
 *   <li>响应回到 filter 时 {@code span.end()} 并清掉 MDC（避免线程复用泄漏到下一个请求）</li>
 *   <li>异常路径上 {@code span.setStatus(ERROR)} + 记录异常事件</li>
 *   <li>当前请求没有 traceId 时由 OTel SDK 自动生成（SDK 默认行为）；上游传 {@code traceparent} header 时由 SDK 自动提取并沿用</li>
 * </ul>
 *
 * @since 2026-08-04
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceIdFilter extends OncePerRequestFilter {

    /** 过滤器作用路径：JWT 保护的 API 路径 + OpenAI 兼容路径. */
    private static final String[] URL_PATTERNS = {"/design/api/", "/design/v1/"};

    /** tracer 名（用于 OpenTelemetry SDK 区分来源）—— 与 service.name 一致即可. */
    private static final String TRACER_NAME = "ProjectInfomationManage";

    /** span 名格式：{@code http.server.request <METHOD> <URI>}. */
    private static final String SPAN_NAME_FORMAT = "http.server.request %s %s";

    private final Tracer tracer;

    private final TextMapPropagator propagator;

    /**
     * 使用给定的 OpenTelemetry SDK 构造 TraceIdFilter.
     *
     * @param openTelemetry OpenTelemetry SDK 实例（由 {@link OpenTelemetryConfig} 装配）
     */
    public TraceIdFilter(OpenTelemetry openTelemetry) {
        this.tracer = openTelemetry.getTracer(TRACER_NAME);
        this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
    }

    /**
     * 把 HttpServletRequest 适配为 OTel TextMapGetter —— 用于从入站请求提取 W3C traceparent.
     */
    private static final TextMapGetter<HttpServletRequest> REQUEST_HEADER_GETTER = new TextMapGetter<>() {
        @Override
        public Iterable<String> keys(HttpServletRequest carrier) {
            return Collections.list(carrier.getHeaderNames());
        }

        @Override
        public String get(HttpServletRequest carrier, String key) {
            return carrier == null ? null : carrier.getHeader(key);
        }
    };

    /**
     * 仅对 {@code /design/api/**} 与 {@code /design/v1/**} 启用本 filter.
     *
     * @param request 当前请求
     * @return 是否跳过本 filter（true = 跳过）
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri == null) {
            return true;
        }
        for (String pattern : URL_PATTERNS) {
            if (uri.startsWith(pattern)) {
                return false;
            }
        }
        return true;
    }

    /**
     * filter 主逻辑.
     *
     * @param request     当前请求
     * @param response    当前响应
     * @param filterChain 过滤器链
     * @throws ServletException 转发异常
     * @throws IOException      IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String spanName = String.format(SPAN_NAME_FORMAT, request.getMethod(), getRequestPath(request));

        // 从入站请求里提取 W3C traceparent —— 有的话沿用上游 traceId，没有的话 SDK 自动生成
        Context extracted = Context.current();
        if (propagator != null) {
            extracted = propagator.extract(extracted, request, REQUEST_HEADER_GETTER);
        }

        Span span = tracer.spanBuilder(spanName)
                .setSpanKind(SpanKind.SERVER)
                .setParent(extracted)
                .startSpan();

        // 让下游代码（Controller / Service / 出站 RestTemplate）能 Span.current() 拿到这个 span
        try (Scope ignored = span.makeCurrent()) {
            // 把 trace_id / span_id 镜像到 MDC，logback 的 OpenTelemetryAppender 会读它
            putMdcIfPresent(span);
            filterChain.doFilter(request, response);
            // 2xx/3xx/4xx 都不算 error；只有异常时才标 ERROR
            if (span.getSpanContext().isValid() && response.getStatus() >= 500) {
                span.setStatus(StatusCode.ERROR, "HTTP " + response.getStatus());
            }
        } catch (Exception exception) {
            span.recordException(exception);
            span.setStatus(StatusCode.ERROR, exception.getClass().getSimpleName());
            throw exception;
        } finally {
            span.end();
            MDC.remove("trace_id");
            MDC.remove("span_id");
        }
    }

    /**
     * 从当前 span 拿 traceId / spanId 写入 MDC.
     *
     * @param span 当前 span
     */
    private static void putMdcIfPresent(Span span) {
        if (span.getSpanContext().isValid()) {
            MDC.put("trace_id", span.getSpanContext().getTraceId());
            MDC.put("span_id", span.getSpanContext().getSpanId());
        }
    }

    /**
     * 取 servlet path 优先，fallback 到 requestURI（带 context-path 时 servlet path 已剥过）.
     *
     * @param request 当前请求
     * @return 用于 span 名的路径
     */
    private static String getRequestPath(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        return servletPath != null && !servletPath.isEmpty() ? servletPath : request.getRequestURI();
    }

    // OpenTelemetry Context 工具方法（导出供其他模块复用）：把当前 Context 包到一个 Runnable 上。
    @SuppressWarnings("unused")
    static Runnable wrapWithCurrentContext(Runnable delegate) {
        Context snapshot = Context.current();
        return () -> {
            try (Scope ignored = snapshot.makeCurrent()) {
                delegate.run();
            }
        };
    }
}