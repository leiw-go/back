/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller.openai;

import com.yaowenltd.projectinfomationmanage.service.openai.OpenAiRelayService;
import com.yaowenltd.projectinfomationmanage.service.openai.dto.EmbeddingRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OpenAI 兼容 Embeddings 入口（{@code POST /design/v1/embeddings}）.
 *
 * @since 2026-08-04
 */
@RestController
@RequestMapping("/v1")
@Tag(name = "OpenAI Compatible Embeddings", description = "OpenAI 兼容 Embeddings 中转")
public class EmbeddingsController {

    private final OpenAiRelayService relay;

    public EmbeddingsController(OpenAiRelayService relay) {
        this.relay = relay;
    }

    /**
     * Embeddings —— 非流式直转上游 provider.
     *
     * @param request Embeddings 请求体
     * @param http    HTTP 请求
     * @return 嵌入向量响应
     */
    @PostMapping("/embeddings")
    @Operation(summary = "Embeddings",
            description = "OpenAI 兼容 Embeddings 接口，转发到上游 provider")
    public ResponseEntity<?> embeddings(@RequestBody EmbeddingRequest request,
                                        HttpServletRequest http) {
        String requestId = http.getHeader("X-Request-Id");
        String userId = (String) http.getAttribute("username");
        return relay.embedding(request, userId, requestId);
    }
}