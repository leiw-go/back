/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.inquiry;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 询盘入参（新建 / 更新）.
 *
 * @since 2026-11-08
 */
@Data
public class CrmInquiryRequest {

    /**
     * 客户 ID
     */
    private String customerId;

    /**
     * 联系人 ID；为空时表示未指定联系人
     */
    private String contactId;

    /**
     * 来源渠道
     */
    private String source;

    /**
     * 主题
     */
    private String subject;

    /**
     * 客户目标单价（仅参考，不参与定价）
     */
    private BigDecimal targetPrice;

    /**
     * 币种；为空时默认 USD
     */
    private String currencyCode;

    /**
     * 数量
     */
    private BigDecimal quantity;

    /**
     * 单位
     */
    private String unit;

    /**
     * 期望交期
     */
    private LocalDate expectDeliveryDate;

    /**
     * 状态；为空时默认 NEW
     */
    private String inquiryStatus;

    /**
     * 原始需求内容（可外文）
     */
    private String content;

    /**
     * 负责人 ID
     */
    private String ownerId;

}
