/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.openai;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.ModelType;
import org.springframework.stereotype.Component;

/**
 * BPE token 估算器（基于 jtokkit）.
 * <p>
 * 不同 model 用不同的 BPE 表：
 * </p>
 * <ul>
 *   <li>cl100k_base —— gpt-3.5 / gpt-4 / text-embedding-ada-002</li>
 *   <li>o200k_base —— gpt-4o / gpt-4o-mini / text-embedding-3-small/large</li>
 *   <li>p50k_base —— code-davinci 等 legacy</li>
 * </ul>
 *
 * @since 2026-08-04
 */
@Component
public class TokenEstimator {

    /**
     * 估算一段文本的 token 数.
     *
     * @param text  输入文本（可空）
     * @param model model 名（用来选 BPE 表；未知 model 回退到 cl100k_base）
     * @return token 数；输入为空时返回 0
     */
    public int estimate(String text, String model) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        Encoding encoding = lookupEncoding(model);
        return encoding.countTokens(text);
    }

    /**
     * 估算 Chat Completions 请求的 prompt token 数 —— 把所有 message 的 content 拼起来估.
     *
     * @param messagesJson 序列化后的 messages JSON（已包含 role/content 等字段）
     * @param model        model 名
     * @return 估算的 prompt token 数
     */
    public int estimatePrompt(String messagesJson, String model) {
        return estimate(messagesJson, model);
    }

    /**
     * 查 model 对应的 BPE 编码 —— 优先用 jtokkit 已知的 ModelType，回退到 cl100k_base.
     *
     * @param model model 名（上游 model，拆过 provider 前缀的）
     * @return Encoding 实例
     */
    private Encoding lookupEncoding(String model) {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        if (model != null) {
            String lower = model.toLowerCase();
            ModelType type = matchKnownModel(lower);
            if (type != null) {
                try {
                    return registry.getEncodingForModel(type);
                } catch (Exception ignored) {
                    // jtokkit 0.6.1 不支持某些 ModelType —— 回退到 cl100k_base
                }
            }
        }
        // 默认 cl100k_base（gpt-3.5 / gpt-4 / embeddings 通用）
        return registry.getEncoding(EncodingType.CL100K_BASE);
    }

    /**
     * 按 model 名启发式匹配 jtokkit 的 {@link ModelType}.
     *
     * @param lower 小写 model 名
     * @return ModelType（无匹配返回 null）
     */
    private static ModelType matchKnownModel(String lower) {
        if (lower.contains("gpt-4")) {
            return ModelType.GPT_4;
        }
        if (lower.contains("gpt-3.5") || lower.contains("gpt-35")) {
            return ModelType.GPT_3_5_TURBO;
        }
        return null;
    }
}