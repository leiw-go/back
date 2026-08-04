/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.llm;

import com.yaowenltd.projectinfomationmanage.mapper.llm.LlmRequestLogMapper;
import com.yaowenltd.projectinfomationmanage.model.entity.LlmRequestLog;
import io.opentelemetry.api.trace.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 把每次 LLM 调用的用量 / 状态 / 延迟 / traceId 写入 {@code t_llm_request_log}.
 *
 * @since 2026-08-04
 */
@Component
public class UsageRecorder {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsageRecorder.class);

    private final LlmRequestLogMapper mapper;

    public UsageRecorder(LlmRequestLogMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 记录一次调用结果（成功或失败）.
     *
     * @param userId       调用方用户 ID
     * @param provider     provider 名
     * @param model        请求 model 字段（原始值，未拆 provider 前缀）
     * @param endpoint     {@code chat_completions} / {@code embeddings}
     * @param promptTokens prompt token 数（0 表示未知）
     * @param completionTokens completion token 数（0 表示未知）
     * @param totalTokens  总 token 数
     * @param statusCode   HTTP 状态码
     * @param latencyMs    端到端耗时（毫秒）
     * @param requestId    客户端 X-Request-Id（可空）
     * @param errorMessage 错误摘要（成功时为 null）
     */
    public void record(String userId,
                       String provider,
                       String model,
                       String endpoint,
                       int promptTokens,
                       int completionTokens,
                       int totalTokens,
                       int statusCode,
                       long latencyMs,
                       String requestId,
                       String errorMessage) {
        LlmRequestLog log = new LlmRequestLog();
        log.setId(UUID.randomUUID().toString());
        log.setUserId(userId);
        log.setProvider(provider);
        log.setModel(model);
        log.setEndpoint(endpoint);
        log.setPromptTokens(promptTokens);
        log.setCompletionTokens(completionTokens);
        log.setTotalTokens(totalTokens);
        log.setStatusCode(statusCode);
        log.setLatencyMs((int) Math.min(Integer.MAX_VALUE, latencyMs));
        log.setRequestId(requestId);
        log.setErrorMessage(errorMessage);

        Span current = Span.current();
        if (current.getSpanContext().isValid()) {
            log.setTraceId(current.getSpanContext().getTraceId());
        }

        try {
            mapper.insertRequestLog(log);
        } catch (Exception e) {
            // 落库失败不能影响业务响应 —— 仅日志告警
            LOGGER.warn("Failed to insert llm request log: {}", e.getMessage());
        }
    }
}