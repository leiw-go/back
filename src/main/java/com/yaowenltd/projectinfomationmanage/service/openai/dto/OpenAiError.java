/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * OpenAI 错误响应体 —— 用于把上游错误透传给客户端，或把网关层错误以 OpenAI 格式返回.
 *
 * @since 2026-08-04
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAiError {

    private Error error;

    /**
     * 构造一个 OpenAiError.
     *
     * @param message 错误消息（直接对外暴露，不含内部栈）
     * @param type    错误类型（如 {@code upstream_error} / {@code invalid_request_error}）
     * @param code    错误 code（可选，如上游返回的 code；null 表示无）
     * @return OpenAiError 实例
     */
    public static OpenAiError of(String message, String type, String code) {
        OpenAiError wrapper = new OpenAiError();
        Error err = new Error();
        err.setMessage(message);
        err.setType(type);
        err.setCode(code);
        wrapper.setError(err);
        return wrapper;
    }

    /**
     * 错误内层.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Error {
        private String message;
        private String type;
        private String code;
        @JsonProperty("param")
        private String param;
    }
}