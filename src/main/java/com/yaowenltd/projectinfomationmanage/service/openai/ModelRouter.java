/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.openai;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 解析客户端请求里的 {@code model} 字段，把 {@code "provider:model"} 拆成两部分.
 * <p>
 * 协议：客户端发 {@code "openai:gpt-4o-mini"} / {@code "deepseek:deepseek-chat"} 等，
 * 由网关路由到对应 provider。未带 {@code "provider:"} 前缀时视作解析失败 —— 让上游 SDK
 * 写错也能被网关快速拒绝，避免悄悄打到默认 provider。
 * </p>
 *
 * @since 2026-08-04
 */
public final class ModelRouter {

    private ModelRouter() {
    }

    /**
     * 路由结果.
     *
     * @since 2026-08-04
     */
    @Data
    @AllArgsConstructor
    public static class Resolved {

        /** provider 名（DB 中 {@code t_llm_provider.name} 的取值） */
        private String provider;

        /** 上游 model 名（拆掉前缀后） */
        private String upstreamModel;

        /** 客户端原始 model 字符串（{@code provider:model}）—— 用于落库 */
        private String originalModel;
    }

    /**
     * 解析 model 字符串.
     *
     * @param model 客户端传的 model 字段
     * @return 解析结果（无法解析时返回 null，由调用方转为 400 error）
     */
    public static Resolved resolve(String model) {
        if (model == null) {
            return null;
        }
        int idx = model.indexOf(':');
        if (idx <= 0 || idx >= model.length() - 1) {
            return null;
        }
        String provider = model.substring(0, idx);
        String upstreamModel = model.substring(idx + 1);
        if (provider.isBlank() || upstreamModel.isBlank()) {
            return null;
        }
        return new Resolved(provider, upstreamModel, model);
    }
}