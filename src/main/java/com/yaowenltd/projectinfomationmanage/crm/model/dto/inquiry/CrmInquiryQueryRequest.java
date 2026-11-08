/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.inquiry;

import lombok.Data;

/**
 * CRM 询盘列表筛选条件.
 *
 * @since 2026-11-08
 */
@Data
public class CrmInquiryQueryRequest {

    /**
     * 页码，从 1 开始
     */
    private Integer page;

    /**
     * 每页条数
     */
    private Integer size;

    /**
     * 状态 NEW/PROCESSING/QUOTED/WON/LOST
     */
    private String inquiryStatus;

    /**
     * 客户 ID
     */
    private String customerId;

    /**
     * 负责人 ID
     */
    private String ownerId;

    /**
     * 来源渠道
     */
    private String source;

    /**
     * 只看超时未报价（创建后超过 24 小时且仍无报价单）
     */
    private Boolean pendingQuote;

}
