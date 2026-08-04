/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * OpenAI {@code GET /v1/models} 响应体.
 *
 * @since 2026-08-04
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModelListResponse {

    /** 固定 {@code list}. */
    private String object = "list";

    private List<Model> data;

    /**
     * 单个 model.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Model {
        /** 模型 id —— 本服务形如 {@code openai:gpt-4o-mini}. */
        private String id;
        /** 固定 {@code model}. */
        private String object = "model";
        /** 拥有者 —— 本服务用 provider 名. */
        private String ownedBy;
        /** 创建时间（epoch 秒）—— 本服务填 0（无意义字段） */
        private Long created;
    }
}