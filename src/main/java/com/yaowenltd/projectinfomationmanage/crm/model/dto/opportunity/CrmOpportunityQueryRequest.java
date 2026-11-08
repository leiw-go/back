/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity;

import lombok.Data;

/**
 * CRM 商机列表筛选条件.
 *
 * @since 2026-11-08
 */
@Data
public class CrmOpportunityQueryRequest {

    /**
     * 页码，从 1 开始
     */
    private Integer page;

    /**
     * 每页条数
     */
    private Integer size;

    /**
     * 阶段
     */
    private String stage;

    /**
     * 负责人 ID
     */
    private String ownerId;

    /**
     * 客户 ID
     */
    private String customerId;

    /**
     * 金额下限（含）
     */
    private java.math.BigDecimal minAmount;

    /**
     * 金额上限（含）
     */
    private java.math.BigDecimal maxAmount;

    /**
     * 商机名称关键字
     */
    private String name;

}
