/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.service.email.CrmEmailEngineOutputParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link CrmEmailEngineOutputParser} 的单元测试.
 *
 * @since 2026-11-18
 */
@DisplayName("邮件引擎输出解析")
class CrmEmailEngineOutputParserTests {

    @Test
    @DisplayName("拆出主题与正文")
    void splitsSubjectAndBody() {
        var parsed = CrmEmailEngineOutputParser.parse(
                "Subject: Quotation for LED strip\n\nDear Mr. Smith,\nCould you please help me?\n\nBest regards");

        assertEquals("Quotation for LED strip", parsed.subject());
        assertEquals("Dear Mr. Smith,\nCould you please help me?\n\nBest regards", parsed.body());
    }

    @Test
    @DisplayName("没有主题行时整段当正文，不丢内容")
    void keepsWholeTextWhenNoSubject() {
        var parsed = CrmEmailEngineOutputParser.parse("Dear Mr. Smith,\nCould you please help me?");

        assertEquals("", parsed.subject());
        assertEquals("Dear Mr. Smith,\nCould you please help me?", parsed.body());
    }

    @Test
    @DisplayName("主题行大小写不敏感")
    void subjectPrefixIsCaseInsensitive() {
        var parsed = CrmEmailEngineOutputParser.parse("SUBJECT: Hello\n\nBody text");

        assertEquals("Hello", parsed.subject());
        assertEquals("Body text", parsed.body());
    }

    @Test
    @DisplayName("空输出返回空主题空正文")
    void handlesEmptyOutput() {
        var parsed = CrmEmailEngineOutputParser.parse("   ");

        assertEquals("", parsed.subject());
        assertEquals("", parsed.body());
    }
}
