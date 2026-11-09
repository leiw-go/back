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
     * 本月成交额（订单 {@code t_order.total_amount_base} 合计，本位币 USD）.
     * <p>
     * 成交口径 = 订单已确认即视为成交（{@code CONFIRMED / IN_PRODUCTION / SHIPPED /
     * DELIVERED / COMPLETED}），排除已取消（{@code CANCELLED}）；按订单创建时间归月。
     * </p>
     */
    private BigDecimal monthDealAmount = BigDecimal.ZERO;

    /**
     * 逾期应收金额（本位币 USD）：约定收款日早于今天、且尚未收齐的应收金额合计.
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
