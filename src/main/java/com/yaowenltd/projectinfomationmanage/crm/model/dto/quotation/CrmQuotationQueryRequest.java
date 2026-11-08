/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation;

import lombok.Data;

/**
 * CRM 报价单列表筛选条件.
 *
 * @since 2026-11-08
 */
@Data
public class CrmQuotationQueryRequest {

    /**
     * 页码，从 1 开始
     */
    private Integer page;

    /**
     * 每页条数
     */
    private Integer size;

    /**
     * 客户 ID
     */
    private String customerId;

    /**
     * 状态 DRAFT/SENT/CONFIRMED/REJECTED/EXPIRED
     */
    private String quotationStatus;

    /**
     * 币种
     */
    private String currencyCode;

    /**
     * 负责人 ID
     */
    private String ownerId;

    /**
     * 来源询盘 ID
     */
    private String inquiryId;

    /**
     * 只看已过有效期的（DRAFT / SENT 且 valid_until 已过）
     */
    private Boolean expired;

}
