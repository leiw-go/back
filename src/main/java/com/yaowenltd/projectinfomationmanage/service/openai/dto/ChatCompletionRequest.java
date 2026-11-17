/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.openai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * OpenAI Chat Completions 请求体（按官方文档字段命名）.
 * <p>
 * 字段全集见 <a href="https://platform.openai.com/docs/api-reference/chat/create">OpenAI Chat API</a>。
 * 本类只承接本服务关心的字段；其余字段（如 {@code tools} / {@code functions}）首版不解析，
 * 通过 {@code extra} 透传给上游。
 * </p>
 *
 * @since 2026-08-04
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChatCompletionRequest {

    /** 模型名 —— 本服务约定形如 {@code openai:gpt-4o-mini} / {@code deepseek:deepseek-chat}. */
    private String model;

    /** 对话历史. */
    private List<Message> messages;

    /** 温度. */
    private Double temperature;

    /** 是否流式. */
    private Boolean stream;

    /** 最大生成 token 数. */
    @JsonProperty("max_tokens")
    private Integer maxTokens;

    /** top_p. */
    @JsonProperty("top_p")
    private Double topP;

    /** 频率惩罚. */
    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    /** 存在惩罚. */
    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    /** 停止词. */
    private List<String> stop;

    /** 用户标识（用于上游滥用检测）—— 本服务会覆盖为 JWT subject. */
    private String user;

    /** 流式选项（{@code stream_options.include_usage}）—— 本服务默认强制设为 true. */
    @JsonProperty("stream_options")
    private StreamOptions streamOptions;

    /** 其它未识别的字段透传给上游. */
    private Map<String, Object> extra;

    /**
     * 单条对话消息.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Message {
        private String role;
        private String content;
        private String name;
    }

    /**
     * 流式选项.
     */
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class StreamOptions {
        @JsonProperty("include_usage")
        private Boolean includeUsage;
    }

    /**
     * 返回是否流式（默认 false）.
     *
     * @return 是否流式
     */
    public boolean isStream() {
        return Boolean.TRUE.equals(stream);
    }

    /**
     * 返回是否需要在流式响应里包含 usage（默认 true，本服务强制开启）.
     *
     * @return 是否包含 usage
     */
    public boolean isIncludeUsage() {
        if (streamOptions == null || streamOptions.includeUsage == null) {
            return true;
        }
        return streamOptions.includeUsage;
    }
}