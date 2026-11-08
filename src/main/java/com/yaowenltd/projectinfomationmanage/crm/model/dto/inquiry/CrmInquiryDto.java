/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.inquiry;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationSummaryDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * CRM 询盘出参.
 *
 * @since 2026-11-08
 */
@Data
public class CrmInquiryDto {

    /**
     * 询盘 ID
     */
    private String id;

    /**
     * 客户 ID
     */
    private String customerId;

    /**
     * 联系人 ID
     */
    private String contactId;

    /**
     * 询盘编号
     */
    private String inquiryNo;

    /**
     * 来源渠道
     */
    private String source;

    /**
     * 主题
     */
    private String subject;

    /**
     * 客户目标单价（仅参考）
     */
    private BigDecimal targetPrice;

    /**
     * 币种
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
     * 状态 NEW/PROCESSING/QUOTED/WON/LOST
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

    /**
     * 首次回复时间
     */
    private LocalDateTime firstReplyAt;

    /**
     * 关联报价单（详情接口返回）
     */
    private List<CrmQuotationSummaryDto> quotations = new ArrayList<>();

    /**
     * 是否超时未报价（创建后超过 24 小时且仍无报价单）
     */
    private boolean pendingQuote;

}
