/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.email;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * CRM 邮件生成历史（一条 = 一次生成）.
 *
 * @since 2026-11-18
 */
@Data
@Schema(description = "邮件生成历史")
public class CrmEmailGenerationDto {

    /** 记录 ID. */
    @Schema(description = "记录 ID")
    private String id;

    /** 一次生成的请求 ID. */
    @Schema(description = "生成请求 ID")
    private String requestId;

    /** 主题. */
    @Schema(description = "邮件主题")
    private String subject;

    /** 正文. */
    @Schema(description = "邮件正文")
    private String content;

    /** 目标语言码. */
    @Schema(description = "目标语言码")
    private String language;

    /** 语气. */
    @Schema(description = "语气：new_customer / existing_customer")
    private String tone;

    /** 业务场景. */
    @Schema(description = "业务场景")
    private String scene;

    /** 模型. */
    @Schema(description = "模型（provider:model）")
    private String model;

    /** 输入的草稿；为空表示直接生成. */
    @Schema(description = "输入的草稿")
    private String sourceContent;

    /** 特殊场景与要求. */
    @Schema(description = "特殊场景与要求")
    private String customInstruction;

    /** 客户称呼. */
    @Schema(description = "客户称呼")
    private String customerName;

    /** 我方发件人. */
    @Schema(description = "我方发件人")
    private String senderName;

    /** 我方公司. */
    @Schema(description = "我方公司")
    private String senderCompany;

    /** 我方职位. */
    @Schema(description = "我方职位")
    private String senderTitle;

    /** 生成耗时（毫秒）. */
    @Schema(description = "生成耗时（毫秒）")
    private Long elapsedMs;

    /** 输入 token. */
    @Schema(description = "输入 token")
    private Integer promptTokens;

    /** 输出 token. */
    @Schema(description = "输出 token")
    private Integer completionTokens;

    /** 总 token. */
    @Schema(description = "总 token")
    private Integer totalTokens;

    /** 生成人. */
    @Schema(description = "生成人")
    private String ownerId;

    /** 生成时间. */
    @Schema(description = "生成时间")
    private LocalDateTime createTime;
}
