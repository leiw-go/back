/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailEngineRewriteRequest;
import com.yaowenltd.projectinfomationmanage.crm.service.email.CrmEmailEnginePromptBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CrmEmailEnginePromptBuilder} 的单元测试.
 * <p>
 * prompt 是这个功能的核心资产 —— 语气话术、事实保护约束都在这里，
 * 改坏了要立刻红。
 *
 * @since 2026-11-17
 */
@DisplayName("邮件引擎 prompt 构造")
class CrmEmailEnginePromptBuilderTests {

    private final CrmEmailEnginePromptBuilder builder = new CrmEmailEnginePromptBuilder();

    private CrmEmailEngineRewriteRequest request() {
        CrmEmailEngineRewriteRequest request = new CrmEmailEngineRewriteRequest();
        request.setContent("Dear, we are interested in your product. Price? When can you deliver?");
        request.setSubject("Inquiry about LED strip");
        request.setLanguage("en");
        request.setTone("new_customer");
        request.setScene("inquiry_reply");
        request.setCustomerName("Acme Trading Co.");
        request.setSenderName("Wang Lei");
        request.setSenderCompany("GreatBus");
        request.setSenderTitle("Sales Manager");
        return request;
    }

    @Test
    @DisplayName("系统提示包含目标语言与语气话术")
    void systemPromptContainsLanguageAndTone() {
        String prompt = builder.buildSystemPrompt(request());

        assertTrue(prompt.contains("英语（English）"), "应写明目标语言");
        assertTrue(prompt.contains("干练"), "应包含简洁干练的要求");
        assertTrue(prompt.contains("非常客气、礼貌、友好"), "新客户语气应写明客气友好");
        assertTrue(prompt.contains("询盘回复"), "应包含场景约定");
    }

    @Test
    @DisplayName("老客户语气要求随和亲近")
    void existingCustomerToneIsCasual() {
        CrmEmailEngineRewriteRequest request = request();
        request.setTone("existing_customer");

        String prompt = builder.buildSystemPrompt(request);

        assertTrue(prompt.contains("随和、亲近"), "老客户语气应写明随和亲近");
        assertTrue(prompt.contains("Please can you"), "老客户语气应给出轻松句式示例");
    }

    @Test
    @DisplayName("系统提示必须带事实保护约束")
    void systemPromptProtectsFacts() {
        String prompt = builder.buildSystemPrompt(request());

        assertTrue(prompt.contains("不得新增"), "必须禁止新增事实");
        assertTrue(prompt.contains("Subject:"), "必须规定主题行格式");
        assertTrue(prompt.contains("Markdown"), "必须禁止 Markdown 代码块");
    }

    @Test
    @DisplayName("没有草稿时切换成从零撰写 —— 禁止编造具体数据")
    void withoutDraftWritesFromScratch() {
        CrmEmailEngineRewriteRequest request = request();
        request.setContent(null);

        String prompt = builder.buildSystemPrompt(request);

        assertTrue(prompt.contains("从零撰写"), "应切换到从零撰写模式");
        assertTrue(prompt.contains("不要编造"), "必须禁止编造型号价格日期");
        assertTrue(prompt.contains("[型号]"), "应给出占位写法示例");
    }

    @Test
    @DisplayName("使用者给的特殊场景与要求会被写进 prompt，且优先于场景约定")
    void customInstructionIsInjected() {
        CrmEmailEngineRewriteRequest request = request();
        request.setCustomInstruction("客户展会后要跟进，强调交期");

        String prompt = builder.buildSystemPrompt(request);

        assertTrue(prompt.contains("客户展会后要跟进，强调交期"), "应带上特殊要求");
        assertTrue(prompt.contains("优先级高于"), "应说明特殊要求优先级更高");
    }

    @Test
    @DisplayName("语言 / 语气 / 场景取不到时回落到默认值")
    void fallsBackToDefaults() {
        CrmEmailEngineRewriteRequest request = request();
        request.setLanguage("xx");
        request.setTone("unknown");
        request.setScene("unknown");

        String prompt = builder.buildSystemPrompt(request);

        assertTrue(prompt.contains("英语（English）"), "未知语言回落到英语");
        assertTrue(prompt.contains("礼貌"), "未知语气回落到新客户（礼貌的一档）");
        assertTrue(prompt.contains("客户询盘后的首次回复"), "未知场景回落到询盘回复");
    }

    @Test
    @DisplayName("用户提示带上原始内容与上下文")
    void userPromptCarriesContext() {
        String prompt = builder.buildUserPrompt(request());

        assertTrue(prompt.contains("Dear, we are interested"), "应带上原始正文");
        assertTrue(prompt.contains("Inquiry about LED strip"), "应带上原始主题");
        assertTrue(prompt.contains("Acme Trading Co."), "应带上客户名");
        assertTrue(prompt.contains("GreatBus"), "应带上我方公司");
    }

    @Test
    @DisplayName("没有草稿时用户提示说明直接写新邮件")
    void userPromptWithoutDraft() {
        CrmEmailEngineRewriteRequest request = request();
        request.setContent(null);

        String prompt = builder.buildUserPrompt(request);

        assertTrue(prompt.contains("没有草稿"), "应说明没有草稿");
        assertFalse(prompt.contains("Dear, we are interested"), "不应带上原始正文");
    }

    @Test
    @DisplayName("未提供发件人时用默认的 leon / 销售业务员")
    void userPromptUsesDefaultSender() {
        CrmEmailEngineRewriteRequest request = request();
        request.setSenderName(null);
        request.setSenderCompany(null);
        request.setSenderTitle(null);

        String prompt = builder.buildUserPrompt(request);

        assertFalse(prompt.contains("Wang Lei"), "不应保留未提供的发件人");
        assertTrue(prompt.contains("leon"), "发件人应回落到 leon");
        assertTrue(prompt.contains("销售业务员"), "职位应回落到销售业务员");
    }

    @Test
    @DisplayName("提示词要求署名原样写名字，不许补 [Last Name] 之类的占位")
    void systemPromptKeepsSenderNameAsIs() {
        String prompt = builder.buildSystemPrompt(request());

        assertTrue(prompt.contains("原样写"), "应要求发件人姓名原样写");
        assertTrue(prompt.contains("[Last Name]"), "应点名禁止拆姓名补占位");
    }
}
