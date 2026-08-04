/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * OpenAI Chat Completions 响应体（按官方文档字段命名）.
 *
 * @since 2026-08-04
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionResponse {

    /** 响应 id. */
    private String id;

    /** 固定 {@code chat.completion}. */
    private String object;

    /** 创建时间（epoch 秒）. */
    private Long created;

    /** 实际使用的 model. */
    private String model;

    /** 选择列表（默认 1 个）—— 多 choice 模式下逐个填. */
    private List<Choice> choices;

    /** 用量信息. */
    private Usage usage;

    /** 系统指纹. */
    @JsonProperty("system_fingerprint")
    private String systemFingerprint;

    /**
     * 单个 choice.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        private Integer index;
        private Message message;
        private String finishReason;
    }

    /**
     * choice 里的消息.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private String role;
        private String content;
    }

    /**
     * 用量.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        @JsonProperty("completion_tokens")
        private Integer completionTokens;
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }
}