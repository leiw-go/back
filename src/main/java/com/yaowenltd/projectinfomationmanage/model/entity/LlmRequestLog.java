/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * LLM 调用记录实体，对应表 {@code t_llm_request_log}.
 *
 * @since 2026-08-04
 */
@Data
public class LlmRequestLog {

    /** 主键 UUID. */
    private String id;

    /** JWT subject（t_user.id）—— 调用方用户. */
    private String userId;

    /** 上游 provider 名. */
    private String provider;

    /** 请求里的 model 字段（原值，未拆 provider 前缀）—— 例如 {@code openai:gpt-4o-mini}. */
    private String model;

    /** {@code chat_completions} / {@code embeddings}. */
    private String endpoint;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    /** HTTP 状态码（上游返回码；流式断连写 499）. */
    private Integer statusCode;

    /** 端到端耗时（毫秒）. */
    private Integer latencyMs;

    /** 客户端请求 ID（X-Request-Id header）—— 可空. */
    private String requestId;

    /** OpenTelemetry traceId，32 位 hex. */
    private String traceId;

    /** 上游错误摘要；成功时 NULL. */
    private String errorMessage;

    /** 创建时间. */
    private LocalDateTime createdAt;
}