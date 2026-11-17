/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller.openai;

import com.yaowenltd.projectinfomationmanage.service.openai.OpenAiRelayService;
import com.yaowenltd.projectinfomationmanage.service.openai.dto.ChatCompletionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * OpenAI 兼容 Chat Completions 入口（{@code POST /design/v1/chat/completions}）.
 * <p>
 * 根据请求体 {@code stream} 字段自动选择响应类型：
 * </p>
 * <ul>
 *   <li>{@code stream=false}（默认）→ 普通 JSON 响应</li>
 *   <li>{@code stream=true} → {@code text/event-stream} 流式响应</li>
 * </ul>
 *
 * @since 2026-08-04
 */
@RestController
@RequestMapping("/v1")
@Tag(name = "OpenAI Compatible Chat", description = "OpenAI 兼容 Chat Completions 中转")
public class ChatCompletionsController {

    private final OpenAiRelayService relay;

    public ChatCompletionsController(OpenAiRelayService relay) {
        this.relay = relay;
    }

    /**
     * Chat Completions —— 非流式返回 JSON，流式返回 {@link SseEmitter}.
     *
     * @param request  Chat Completions 请求体
     * @param http     HTTP 请求（用于取 X-Request-Id）
     * @return 非流式 → {@link ResponseEntity}；流式 → {@link SseEmitter}
     */
    @PostMapping("/chat/completions")
    @Operation(summary = "Chat Completions",
            description = "OpenAI 兼容 Chat Completions，按 stream 字段返回 JSON 或 SSE 流")
    public Object chatCompletions(@RequestBody ChatCompletionRequest request,
                                  HttpServletRequest http) {
        String requestId = http.getHeader("X-Request-Id");
        String userId = (String) http.getAttribute("username");
        return relay.chatCompletion(request, userId, requestId);
    }
}