/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 客户来源渠道分布.
 *
 * @since 2026-11-08
 */
@Data
public class CrmDashboardCustomerSourceDto {

    /**
     * 来源渠道码（字典 customer_source）.
     */
    private String source;

    /**
     * 该来源的客户数.
     */
    private long customerCount;

    /**
     * 该来源客户的商机数.
     */
    private long opportunityCount;

    /**
     * 该来源客户的商机金额合计（本位币 USD）.
     */
    private BigDecimal opportunityAmountBase = BigDecimal.ZERO;

}
