/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * OpenAI Chat Completions 流式 chunk（SSE data 块的反序列化对象）.
 *
 * @since 2026-08-04
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionChunk {

    private String id;
    private String object;
    private Long created;
    private String model;

    private List<Choice> choices;

    /** 流式最后一个 chunk 才会有 usage. */
    private ChatCompletionResponse.Usage usage;

    @JsonProperty("system_fingerprint")
    private String systemFingerprint;

    /**
     * 流式 choice（与 {@link ChatCompletionResponse.Choice} 不同：delta 替代 message）.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {
        private Integer index;
        private Delta delta;

        /**
         * 结束原因 —— 上游字段名是 {@code finish_reason}，必须显式映射
         * （Jackson 默认按驼峰匹配，映射不上会一直是 null）.
         */
        @JsonProperty("finish_reason")
        private String finishReason;
    }

    /**
     * 流式增量.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Delta {
        private String role;
        private String content;

        /**
         * 推理型模型（DeepSeek-R1 / MiniMax-M3 等）的思考过程增量.
         */
        @JsonProperty("reasoning_content")
        private String reasoningContent;
    }
}