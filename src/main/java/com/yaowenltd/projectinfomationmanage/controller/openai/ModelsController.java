/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller.openai;

import com.yaowenltd.projectinfomationmanage.service.openai.OpenAiRelayService;
import com.yaowenltd.projectinfomationmanage.service.openai.dto.ModelListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OpenAI 兼容 Models 列表入口（{@code GET /design/v1/models}）.
 *
 * @since 2026-08-04
 */
@RestController
@RequestMapping("/v1")
@Tag(name = "OpenAI Compatible Models", description = "OpenAI 兼容 Models 列表")
public class ModelsController {

    private final OpenAiRelayService relay;

    public ModelsController(OpenAiRelayService relay) {
        this.relay = relay;
    }

    /**
     * 列出当前可用的 model —— 来自所有启用 provider 的汇总.
     *
     * @return model 列表
     */
    @GetMapping("/models")
    @Operation(summary = "List models",
            description = "列出当前可用的 model（来自所有启用 provider）")
    public ResponseEntity<ModelListResponse> listModels() {
        return relay.listModels();
    }
}