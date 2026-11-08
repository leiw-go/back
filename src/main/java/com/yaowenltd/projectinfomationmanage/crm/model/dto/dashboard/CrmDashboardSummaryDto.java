/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 经营看板顶部指标.
 *
 * @since 2026-11-08
 */
@Data
public class CrmDashboardSummaryDto {

    /**
     * 本月成交额（订单域在 M4 落地后才有数据，此前恒为 0）.
     */
    private BigDecimal monthDealAmount = BigDecimal.ZERO;

    /**
     * 逾期应收金额（订单域在 M4 落地后才有数据，此前恒为 0）.
     */
    private BigDecimal overdueReceivableAmount = BigDecimal.ZERO;

    /**
     * 活跃客户数（未删除且客户状态不是 LOST）.
     */
    private long activeCustomerCount;

    /**
     * 进行中商机数（阶段不是 WON / LOST）.
     */
    private long openOpportunityCount;

    /**
     * 进行中商机金额合计（本位币 USD）.
     */
    private BigDecimal openOpportunityAmountBase = BigDecimal.ZERO;

}
