/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.openai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link ModelRouter} 单元测试 —— 验证 {@code provider:model} 解析契约.
 *
 * @since 2026-08-04
 */
class ModelRouterTests {

    @Test
    void resolve_parsesStandardFormat() {
        ModelRouter.Resolved r = ModelRouter.resolve("openai:gpt-4o-mini");
        assertNotNull(r);
        assertEquals("openai", r.getProvider());
        assertEquals("gpt-4o-mini", r.getUpstreamModel());
        assertEquals("openai:gpt-4o-mini", r.getOriginalModel());
    }

    @Test
    void resolve_parsesDeepSeekFormat() {
        ModelRouter.Resolved r = ModelRouter.resolve("deepseek:deepseek-chat");
        assertNotNull(r);
        assertEquals("deepseek", r.getProvider());
        assertEquals("deepseek-chat", r.getUpstreamModel());
    }

    @Test
    void resolve_returnsNullWhenNoColon() {
        assertNull(ModelRouter.resolve("gpt-4o-mini"));
    }

    @Test
    void resolve_returnsNullWhenEmptyProvider() {
        assertNull(ModelRouter.resolve(":gpt-4o-mini"));
    }

    @Test
    void resolve_returnsNullWhenEmptyModel() {
        assertNull(ModelRouter.resolve("openai:"));
    }

    @Test
    void resolve_returnsNullForNullInput() {
        assertNull(ModelRouter.resolve(null));
    }

    @Test
    void resolve_returnsNullForBlankInput() {
        assertNull(ModelRouter.resolve("   "));
    }

    @Test
    void resolve_preservesOriginalForLogging() {
        ModelRouter.Resolved r = ModelRouter.resolve("azure-openai:my-deployment");
        assertNotNull(r);
        assertEquals("azure-openai:my-deployment", r.getOriginalModel());
    }
}