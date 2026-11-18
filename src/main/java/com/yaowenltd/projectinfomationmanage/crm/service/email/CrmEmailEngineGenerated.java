/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.email;

/**
 * 一次生成完成后的产物 —— 落库用.
 * <p>
 * 帧翻译器在流结束时才知道完整的正文与用量，用它把结果交给上层：
 * 一份回给前端（done 帧），一份存进生成历史。
 *
 * @param requestId        请求 ID
 * @param subject          主题
 * @param body             正文
 * @param model            模型（{@code provider:model}）
 * @param language         目标语言码
 * @param tone             语气码
 * @param scene            场景码
 * @param elapsedMs        耗时（毫秒）
 * @param promptTokens     输入 token
 * @param completionTokens 输出 token
 * @param totalTokens      总 token
 * @since 2026-11-18
 */
public record CrmEmailEngineGenerated(
        String requestId,
        String subject,
        String body,
        String model,
        String language,
        String tone,
        String scene,
        long elapsedMs,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens) {
}
