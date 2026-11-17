/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.email;

import lombok.Data;

/**
 * CRM 邮件引擎改写请求.
 * <p>
 * 一次调用 = 目标语言 + 语气（+ 场景 / 特殊要求 / 发件人上下文）+ 可选的草稿。
 * <p>
 * 有草稿 = 微调草稿，没有草稿 = 按场景与特殊要求直接生成。
 *
 * @since 2026-11-17
 */
@Data
public class CrmEmailEngineRewriteRequest {

    /** 草稿内容（可选，最多 8000 字）：有内容 = 微调，无内容 = 直接生成. */
    private String content;

    /** 草稿里的原始主题（可选，作为上下文）. */
    private String subject;

    /** 目标语言码，如 en / de / ja；为空默认 en. */
    private String language;

    /** 语气码：new_customer / existing_customer；为空默认 new_customer. */
    private String tone;

    /** 场景码，如 quotation / payment；为空默认 inquiry_reply. */
    private String scene;

    /** 使用者额外提出的特殊场景与要求（可选，优先级高于场景约定）. */
    private String customInstruction;

    /** 客户公司名或称呼（进 prompt，决定称呼写法）. */
    private String customerName;

    /** 我方发件人姓名. */
    private String senderName;

    /** 我方公司名. */
    private String senderCompany;

    /** 我方职位. */
    private String senderTitle;

    /** 模型名，格式 {@code provider:model}；为空取默认模型. */
    private String model;

    /** 采样温度；为空默认 0.3（改写要稳定，不要发散）. */
    private Double temperature;

    /** 最大生成 token 数；为空默认 4096（推理模型的思考 token 也算在内）. */
    private Integer maxTokens;
}
