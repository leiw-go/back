/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * OpenAI Embeddings 请求体.
 *
 * @since 2026-08-04
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbeddingRequest {

    /** 模型名（{@code provider:model} 形态） */
    private String model;

    /** 输入文本（单条或批量） */
    private List<String> input;

    /** 用户标识 —— 本服务会覆盖为 JWT subject */
    private String user;

    /** 编码格式（{@code float} / {@code base64}），默认 float */
    private String encodingFormat;
}