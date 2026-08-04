/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.openai.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link OpenAiError} 单元测试 —— 验证 OpenAI 错误响应格式符合官方契约.
 *
 * @since 2026-08-04
 */
class OpenAiErrorTests {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void of_buildsValidStructure() {
        OpenAiError err = OpenAiError.of("model not found", "invalid_request_error", "model_not_found");

        assertNotNull(err.getError());
        assertEquals("model not found", err.getError().getMessage());
        assertEquals("invalid_request_error", err.getError().getType());
        assertEquals("model_not_found", err.getError().getCode());
    }

    @Test
    void of_acceptsNullCode() {
        OpenAiError err = OpenAiError.of("bad request", "invalid_request_error", null);
        assertNull(err.getError().getCode());
    }

    @Test
    void serialization_producesOpenAiErrorShape() throws Exception {
        OpenAiError err = OpenAiError.of("upstream timeout", "upstream_timeout", null);

        String json = mapper.writeValueAsString(err);

        // 顶层是 {"error": {...}} 结构
        assertEquals(true, json.contains("\"error\":{"));
        assertEquals(true, json.contains("\"message\":\"upstream timeout\""));
        assertEquals(true, json.contains("\"type\":\"upstream_timeout\""));
        // code 是 null —— Jackson 默认会序列化为 "code":null
        assertEquals(true, json.contains("\"code\":null"));
    }

    @Test
    void deserialization_roundTripsThroughJackson() throws Exception {
        String json = "{\"error\":{\"message\":\"rate limit exceeded\",\"type\":\"rate_limit_error\",\"code\":\"429\"}}";

        OpenAiError err = mapper.readValue(json, OpenAiError.class);

        assertEquals("rate limit exceeded", err.getError().getMessage());
        assertEquals("rate_limit_error", err.getError().getType());
        assertEquals("429", err.getError().getCode());
    }

    @Test
    void deserialization_toleratesUnknownFields() throws Exception {
        // 上游可能加新字段，本类应能容忍
        String json = "{\"error\":{\"message\":\"oops\",\"type\":\"x\",\"code\":\"y\","
                + "\"param\":\"temperature\",\"some_new_field\":\"ignored\"}}";

        OpenAiError err = mapper.readValue(json, OpenAiError.class);

        assertEquals("oops", err.getError().getMessage());
        assertEquals("x", err.getError().getType());
        assertEquals("y", err.getError().getCode());
        assertEquals("temperature", err.getError().getParam());
    }
}