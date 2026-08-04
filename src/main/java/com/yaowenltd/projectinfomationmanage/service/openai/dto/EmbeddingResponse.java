/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * OpenAI Embeddings 响应体.
 *
 * @since 2026-08-04
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbeddingResponse {

    private String object;

    private List<Embedding> data;

    private String model;

    private Usage usage;

    /**
     * 单条 embedding.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Embedding {
        private String object;
        private Integer index;

        /** 浮点向量；上游若返回 base64，本服务首版不支持. */
        private List<Double> embedding;
    }

    /**
     * 用量.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }
}