/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 上游 LLM provider 配置实体，对应表 {@code t_llm_provider}.
 *
 * @since 2026-08-04
 */
@Data
public class LlmProvider {

    /** 主键 UUID. */
    private String id;

    /** provider 唯一名（与 model 拼接用，如 openai / deepseek / ollama）. */
    private String name;

    /** 上游 baseUrl. */
    private String baseUrl;

    /** AES-GCM 加密后的 API key（含 IV + tag + ciphertext，字节数组形态）—— 不直接展示给前端. */
    private byte[] apiKeyCiphertext;

    /** 1: 启用, 0: 停用. */
    private Integer enabled;

    /** 路由优先级，数值越小越优先. */
    private Integer priority;

    /** 单 provider 超时覆盖（毫秒），NULL 表示走全局默认值. */
    private Integer timeoutMsOverride;

    /** 创建时间. */
    private LocalDateTime createdAt;

    /** 更新时间. */
    private LocalDateTime updatedAt;
}