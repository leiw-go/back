/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller.llm;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.model.dto.LlmProviderDto;
import com.yaowenltd.projectinfomationmanage.service.llm.ProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * LLM provider 管理端 CRUD 接口（仅 ADMIN）.
 *
 * @since 2026-08-04
 */
@RestController
@RequestMapping("/api/llm/providers")
@Tag(name = "LLM Provider Management", description = "上游 LLM provider 配置 CRUD")
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    /**
     * 列出所有 provider（apiKey 字段在响应里不返回）.
     *
     * @return provider 列表
     */
    @GetMapping
    @Operation(summary = "列出所有 provider", description = "返回所有 provider 配置（apiKey 字段不回传）")
    public ResponseResult<List<LlmProviderDto>> listProviders() {
        return ResponseResult.success(providerService.findAll());
    }

    /**
     * 创建 provider.
     *
     * @param dto 入参（必填 name / baseUrl / apiKey）
     * @return 创建后的 DTO
     */
    @PostMapping
    @Operation(summary = "创建 provider", description = "新建一个上游 provider 配置")
    public ResponseResult<LlmProviderDto> createProvider(@RequestBody LlmProviderDto dto) {
        return ResponseResult.created(providerService.create(dto));
    }

    /**
     * 更新 provider.
     *
     * @param dto 入参（必填 id；apiKey 为空则保留原值）
     * @return 更新后的 DTO
     */
    @PutMapping
    @Operation(summary = "更新 provider", description = "更新 provider 配置；apiKey 为空则不修改密文列")
    public ResponseResult<LlmProviderDto> updateProvider(@RequestBody LlmProviderDto dto) {
        return ResponseResult.success(providerService.update(dto));
    }

    /**
     * 删除 provider.
     *
     * @param id provider 主键
     * @return 空成功体
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除 provider", description = "按 ID 删除 provider 配置")
    public ResponseResult<Void> deleteProvider(@PathVariable String id) {
        providerService.delete(id);
        return ResponseResult.success();
    }
}