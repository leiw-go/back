/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * CRM 邮件生成历史实体，对应表 t_crm_email_generation.
 * <p>
 * 邮件引擎每次成功生成都落一份 —— 既能回看这封邮件是怎么生成的，
 * 也能把上一版的结果拿回来继续改。
 *
 * @since 2026-11-18
 */
@Data
public class CrmEmailGenerationEntity {

    /**
     * 主键
     */
    private String id;

    /**
     * 一次生成的请求 ID（与 t_llm_request_log 对齐）
     */
    private String requestId;

    /**
     * 主题
     */
    private String subject;

    /**
     * 正文
     */
    private String content;

    /**
     * 目标语言码
     */
    private String language;

    /**
     * 语气：new_customer / existing_customer
     */
    private String tone;

    /**
     * 业务场景
     */
    private String scene;

    /**
     * 模型（provider:model）
     */
    private String model;

    /**
     * 输入的草稿；为空表示直接生成
     */
    private String sourceContent;

    /**
     * 使用者额外提出的特殊场景与要求
     */
    private String customInstruction;

    /**
     * 客户称呼
     */
    private String customerName;

    /**
     * 我方发件人
     */
    private String senderName;

    /**
     * 我方公司
     */
    private String senderCompany;

    /**
     * 我方职位
     */
    private String senderTitle;

    /**
     * 生成耗时（毫秒）
     */
    private Long elapsedMs;

    /**
     * 输入 token
     */
    private Integer promptTokens;

    /**
     * 输出 token（含推理模型的思考 token）
     */
    private Integer completionTokens;

    /**
     * 总 token
     */
    private Integer totalTokens;

    /**
     * 生成人
     */
    private String ownerId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除：0 未删除；删除时写入毫秒时间戳
     */
    private Long deleted;
}
