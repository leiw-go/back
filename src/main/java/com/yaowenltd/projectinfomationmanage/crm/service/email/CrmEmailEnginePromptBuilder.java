/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.email;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailEngineRewriteRequest;
import org.springframework.stereotype.Component;

/**
 * 邮件引擎的 prompt 构造器.
 * <p>
 * 规则与话术全部收敛在这里 —— 改话术不用发版前端，前端只负责传
 * 「语言 / 语气 / 场景 + 可选的特殊要求 + 可选的草稿」。
 * <p>
 * 两种工作模式（由有没有草稿自动决定，前端不用选）：
 * <ul>
 *   <li><strong>微调</strong>：给了草稿（可能是上一轮的改进结果）→ 在草稿基础上改写，
 *       保留草稿里的事实与信息，只优化表达。</li>
 *   <li><strong>生成</strong>：没给草稿 → 从零写一封完整邮件（含主题）。</li>
 * </ul>
 * <p>
 * 三条最关键的设计约束：
 * <ol>
 *   <li><strong>事实不得编造</strong>：型号、数量、价格、日期、条款等一律不得新增或改写，
 *       原文没有的信息不写 —— 外贸邮件一旦把报价写错，后果是实打实的赔单。</li>
 *   <li><strong>只输出主题 + 正文</strong>：固定格式（首行 {@code Subject:}），
 *       不输出解释、不输出 Markdown 代码块，否则粘进邮件里很难看。</li>
 *   <li><strong>简洁干练</strong>：一句话一个要点，删掉客套铺垫。</li>
 * </ol>
 *
 * @since 2026-11-17
 */
@Component
public class CrmEmailEnginePromptBuilder {

    /** 我方联系人默认值. */
    public static final String DEFAULT_SENDER_NAME = "leon";

    /** 我方职位默认值. */
    public static final String DEFAULT_SENDER_TITLE = "销售业务员";

    /**
     * 构造系统提示.
     *
     * @param request 改写请求
     * @return 系统提示
     */
    public String buildSystemPrompt(CrmEmailEngineRewriteRequest request) {
        CrmEmailEngineLanguage language = CrmEmailEngineLanguage.of(request.getLanguage());
        CrmEmailEngineTone tone = CrmEmailEngineTone.of(request.getTone());
        CrmEmailEngineScene scene = CrmEmailEngineScene.of(request.getScene());
        boolean rewrite = hasDraft(request);
        String custom = customRequirement(request);

        return String.join("\n",
                "你是一名资深的外贸业务经理，负责写专业、简洁、干练的"
                        + language.getPromptName() + "商务邮件。",
                "",
                "硬性要求：",
                "1. 输出格式固定为两段：第一行写 “Subject: <邮件主题>”（主题用"
                        + language.getPromptName() + "，不要加任何前缀或解释）；"
                        + "然后空一行，接着写邮件正文（正文开头不要再重复主题）。"
                        + "除主题行与正文外，不输出任何解释、注释、前言、总结或 Markdown 代码块。",
                "2. 简洁、干练、专业：一句话一个要点，删掉客套铺垫与空洞赞美，全文控制在 150 词以内。",
                rewrite
                        ? "3. 这是**微调**任务：在下面给出的草稿基础上改写。严格保留草稿中的事实性信息："
                            + "型号、规格、数量、价格、金额、日期、港口、贸易术语、付款方式、联系人、公司名、"
                            + "附件说明。不得新增、臆测、换算或修改任何数据；草稿没有的信息一律不要写。"
                            + "草稿里的主题行也要一并优化。"
                        : "3. 这是**从零撰写**任务：没有草稿可参考。绝对不要编造任何具体的型号、价格、金额、"
                            + "日期与条款；需要这些信息的地方用明确占位（如 [型号]、[价格]、[交期]）"
                            + "或直接向客户索取。",
                "4. 全文必须使用" + language.getPromptName() + "，包括主题、称呼、正文、结尾敬语与署名标签。",
                "5. " + tone.getHint(),
                "6. 结构：主题 → 称呼（Dear {客户称呼}，使用该语言惯用写法）→ 正文（2~4 段，"
                        + "按意图顺序）→ 明确的行动请求 → 结尾敬语（该语言惯用敬语）→ 署名（我方发件人信息）。",
                "7. 场景约定：" + scene.getLabel() + " —— " + scene.getHint(),
                custom.isEmpty()
                        ? "8. 没有额外的特殊要求，按上面的约定写。"
                        : "8. 使用者额外提出的特殊场景与要求（优先级高于第 7 条的场景约定）：" + custom,
                "9. 上下文里的称呼、落款如果没有给出，就用通用占位（如 Dear Sir or Madam / Best regards），"
                        + "不要编造具体人名。署名里的发件人姓名按我方发件人原样写"
                        + "（给的是 leon 就写 leon），不要拆成名和姓、不要补 [Last Name] 这类占位。");
    }

    /**
     * 构造用户提示（草稿 + 上下文）.
     *
     * @param request 改写请求
     * @return 用户提示
     */
    public String buildUserPrompt(CrmEmailEngineRewriteRequest request) {
        StringBuilder sb = new StringBuilder();
        if (hasDraft(request)) {
            sb.append("待微调的草稿（可能包含上一轮的改进结果）：\n")
                    .append(request.getContent().trim()).append('\n');
        } else {
            sb.append("没有草稿，请直接写一封新的邮件。\n");
        }
        if (notBlank(request.getSubject())) {
            sb.append("\n草稿里的原始主题：").append(request.getSubject().trim());
        }
        sb.append("\n客户：").append(orDefault(request.getCustomerName(), "（未提供，用通用称呼）"));
        sb.append("\n我方发件人：").append(orDefault(request.getSenderName(), DEFAULT_SENDER_NAME));
        sb.append("，").append(orDefault(request.getSenderCompany(), "（未提供）"));
        sb.append("，").append(orDefault(request.getSenderTitle(), DEFAULT_SENDER_TITLE));
        return sb.toString();
    }

    /**
     * 是否是微调模式（有没有草稿）.
     *
     * @param request 改写请求
     * @return 有草稿返回 true
     */
    public boolean hasDraft(CrmEmailEngineRewriteRequest request) {
        return notBlank(request.getContent());
    }

    /**
     * 取使用者额外提出的特殊场景与要求.
     *
     * @param request 改写请求
     * @return 特殊要求；没有则返回空串
     */
    private String customRequirement(CrmEmailEngineRewriteRequest request) {
        return notBlank(request.getCustomInstruction()) ? request.getCustomInstruction().trim() : "";
    }

    /**
     * 判断字符串是否非空白.
     *
     * @param value 字符串
     * @return 非空白返回 true
     */
    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 取字符串，空则用默认值.
     *
     * @param value        字符串
     * @param defaultValue 默认值
     * @return 非空字符串或默认值
     */
    private String orDefault(String value, String defaultValue) {
        return notBlank(value) ? value.trim() : defaultValue;
    }
}
