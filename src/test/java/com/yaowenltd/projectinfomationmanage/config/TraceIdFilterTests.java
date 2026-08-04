/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TraceIdFilter} 单元测试 —— 验证 traceId 生成 / MDC 写入 / 路径过滤 / W3C 沿用.
 *
 * @since 2026-08-04
 */
class TraceIdFilterTests {

    private InMemorySpanExporter exporter;
    private OpenTelemetrySdk sdk;
    private TraceIdFilter filter;

    @BeforeEach
    void setUp() {
        exporter = InMemorySpanExporter.create();
        SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                .build();
        sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();
        filter = new TraceIdFilter(sdk);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        sdk.getSdkTracerProvider().shutdown();
    }

    @Test
    void filter_designPath_generatesTraceIdAndWritesMdc() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
        request.setRequestURI("/design/api/auth/login");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        // traceId 应在 MDC 里出现一次（filter 退出前），并是 32 位 hex
        // 由于 filter 在 finally 里 MDC.clear()，我们通过 exporter 拿到 traceId 验证
        List<SpanData> spans = exporter.getFinishedSpanItems();
        assertEquals(1, spans.size());
        String traceId = spans.get(0).getTraceId();
        assertEquals(32, traceId.length(), "traceId should be 32 hex chars");
        assertTrue(traceId.matches("[0-9a-f]{32}"), "traceId should match hex pattern");
    }

    @Test
    void filter_unknownPath_isSkipped() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.setRequestURI("/actuator/health");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // actuator 路径不在 filter 范围内 —— 不应产生 span
        assertEquals(0, exporter.getFinishedSpanItems().size());
    }

    @Test
    void filter_v1Path_isCovered() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/chat/completions");
        request.setRequestURI("/design/v1/chat/completions");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // /design/v1/** 也应覆盖
        assertEquals(1, exporter.getFinishedSpanItems().size());
    }

    @Test
    void filter_incomingTraceparent_isReused() throws Exception {
        // 模拟上游已传 traceparent header
        String incomingTraceId = "0af7651916cd43dd8448eb211c80319c";
        String incomingSpanId = "b7ad6b7169203331";
        String traceparent = String.format("00-%s-%s-01", incomingTraceId, incomingSpanId);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
        request.setRequestURI("/design/api/auth/login");
        request.addHeader("traceparent", traceparent);
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // 沿用上游 traceId
        SpanData spanData = exporter.getFinishedSpanItems().get(0);
        assertEquals(incomingTraceId, spanData.getTraceId());
    }

    @Test
    void filter_mdcClearedAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
        request.setRequestURI("/design/api/auth/login");
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                // 链里取 MDC 应该能取到 traceId
                assertNotNull(MDC.get("trace_id"));
            }
        };

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        // filter 退出后 MDC 必须清掉（避免 servlet 线程复用泄漏）
        assertEquals(null, MDC.get("trace_id"));
        assertEquals(null, MDC.get("span_id"));
    }

    @Test
    void filter_chainedContext_isAccessibleToDownstream() throws Exception {
        // 验证 filter 创建的 span 是当前 Context 的 active span —— 下游代码 Span.current() 能拿到
        String[] capturedTraceId = new String[1];

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
        request.setRequestURI("/design/api/auth/login");
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                Span current = Span.current();
                if (current.getSpanContext().isValid()) {
                    capturedTraceId[0] = current.getSpanContext().getTraceId();
                }
            }
        };

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertNotNull(capturedTraceId[0]);
        assertEquals(32, capturedTraceId[0].length());
    }

    @Test
    void filter_onException_marksSpanAsError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/login");
        request.setRequestURI("/design/api/auth/login");
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                throw new RuntimeException("boom");
            }
        };

        try {
            filter.doFilter(request, new MockHttpServletResponse(), chain);
        } catch (RuntimeException ignored) {
            // 异常应当透传
        }

        SpanData spanData = exporter.getFinishedSpanItems().get(0);
        assertEquals(io.opentelemetry.api.trace.StatusCode.ERROR, spanData.getStatus().getStatusCode());
    }

    @Test
    void filter_twoRequests_getDifferentTraceIds() throws Exception {
        // 两次独立请求 —— traceId 必须不同（不串）
        String tid1 = runOneRequest();
        String tid2 = runOneRequest();
        assertNotEquals(tid1, tid2);
    }

    private String runOneRequest() throws Exception {
        exporter.reset();
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/auth/login");
        req.setRequestURI("/design/api/auth/login");
        filter.doFilter(req, new MockHttpServletResponse(), new MockFilterChain());
        return exporter.getFinishedSpanItems().get(0).getTraceId();
    }

    // Unused reference to OpenTelemetry class — guards against SDK shape changes
    @SuppressWarnings("unused")
    private static void referenceOpenTelemetry() {
        SpanKind kind = SpanKind.SERVER;
        try (Scope ignored = Context.root().with(Span.getInvalid()).makeCurrent()) {
            // no-op
        }
        OpenTelemetry noop = OpenTelemetry.noop();
        assertNotNull(noop);
    }
}