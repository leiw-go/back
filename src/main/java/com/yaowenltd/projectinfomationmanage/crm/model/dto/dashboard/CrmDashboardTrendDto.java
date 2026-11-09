/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 经营看板的单个时间桶（按月或按季）.
 *
 * @since 2026-11-08
 */
@Data
public class CrmDashboardTrendDto {

    /**
     * 时间桶标识：按月为 {@code yyyy-MM}，按季为 {@code yyyy-Qn}.
     */
    private String period;

    /**
     * 该期间新增客户数.
     */
    private long newCustomerCount;

    /**
     * 该期间新增商机数.
     */
    private long newOpportunityCount;

    /**
     * 该期间新增商机金额合计（本位币 USD）.
     */
    private BigDecimal newOpportunityAmountBase = BigDecimal.ZERO;

    /**
     * 该期间的成交额（订单 {@code t_order.total_amount_base} 合计，本位币 USD）.
     * <p>
     * 成交口径 = 订单已确认即视为成交（{@code CONFIRMED / IN_PRODUCTION / SHIPPED /
     * DELIVERED / COMPLETED}），排除已取消（{@code CANCELLED}）；按订单创建时间归期。
     * </p>
     */
    private BigDecimal dealAmountBase = BigDecimal.ZERO;

    /**
     * 该期间新增商机的赢单率（%）：当期新增且已决（WON + LOST）的商机里赢单占比；
     * 分母为 0 时取 0.
     */
    private BigDecimal winRate = BigDecimal.ZERO;

}
