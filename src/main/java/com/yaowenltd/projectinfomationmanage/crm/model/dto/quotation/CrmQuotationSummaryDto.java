/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 报价单摘要（询盘详情里列出关联报价单时使用）.
 *
 * @since 2026-11-08
 */
@Data
public class CrmQuotationSummaryDto {

    /**
     * 报价单 ID
     */
    private String id;

    /**
     * 报价单号
     */
    private String quotationNo;

    /**
     * 版本号
     */
    private Integer versionNo;

    /**
     * 币种
     */
    private String currencyCode;

    /**
     * 报价总额（原币）
     */
    private BigDecimal totalAmount;

    /**
     * 状态 DRAFT/SENT/CONFIRMED/REJECTED/EXPIRED
     */
    private String quotationStatus;

    /**
     * 有效期至
     */
    private LocalDate validUntil;

}
