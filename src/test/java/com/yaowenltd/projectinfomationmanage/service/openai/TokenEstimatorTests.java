/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.openai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link TokenEstimator} 单元测试 —— 验证中文 / emoji / 英文 token 估算数量级合理.
 *
 * @since 2026-08-04
 */
class TokenEstimatorTests {

    private final TokenEstimator estimator = new TokenEstimator();

    @Test
    void estimate_returnsZeroForNull() {
        assertEquals(0, estimator.estimate(null, "gpt-4"));
    }

    @Test
    void estimate_returnsZeroForEmpty() {
        assertEquals(0, estimator.estimate("", "gpt-4"));
    }

    @Test
    void estimate_englishShortText_isWithinRange() {
        // cl100k_base 上 "Hello world" 一般是 2 tokens
        int n = estimator.estimate("Hello world", "gpt-4");
        assertTrue(n >= 1 && n <= 3, "english short should be ~2 tokens, got " + n);
    }

    @Test
    void estimate_chineseText_handlesMultiByte() {
        // 中文一般 1 个汉字 ~ 1 token（cl100k_base）
        String text = "你好世界，这是一个测试。";
        int n = estimator.estimate(text, "gpt-4");
        // 包含 12 个汉字 + 一些标点，cl100k_base 通常落在 10-20 之间
        assertTrue(n >= 5 && n <= 25, "chinese text token count should be reasonable, got " + n);
    }

    @Test
    void estimate_emoji_handlesSurrogatePairs() {
        String text = "🎉🚀✨🌟💫";
        int n = estimator.estimate(text, "gpt-4");
        // emoji 在 BPE 里通常占 1-2 tokens
        assertTrue(n >= 2 && n <= 20, "emoji token count should be reasonable, got " + n);
    }

    @Test
    void estimate_gpt4AndGpt35_shouldUseSameEncoding_cl100k() {
        // 当前实现都 fallback 到 cl100k_base —— 验证数量级一致
        String text = "The quick brown fox jumps over the lazy dog.";
        int gpt4 = estimator.estimate(text, "gpt-4");
        int gpt35 = estimator.estimate(text, "gpt-3.5-turbo");
        assertEquals(gpt4, gpt35, "gpt-4 and gpt-3.5 should produce same count under cl100k_base");
    }

    @Test
    void estimate_longerText_scalesLinearly() {
        // 长度翻倍时 token 数应该也接近翻倍（不一定精确，但要大）
        String short1 = "Hello world. ".repeat(5);
        String short2 = "Hello world. ".repeat(50);
        int n1 = estimator.estimate(short1, "gpt-4");
        int n2 = estimator.estimate(short2, "gpt-4");
        assertTrue(n2 > n1 * 5, "longer text should have substantially more tokens");
    }
}