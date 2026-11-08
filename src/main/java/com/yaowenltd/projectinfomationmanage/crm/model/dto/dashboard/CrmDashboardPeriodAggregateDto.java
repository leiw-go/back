/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 看板按时间桶的聚合结果.
 *
 * @since 2026-11-08
 */
@Data
public class CrmDashboardPeriodAggregateDto {

    /**
     * 时间桶标识.
     */
    private String period;

    /**
     * 该时间桶的新增数量.
     */
    private long count;

    /**
     * 该时间桶新增且当前阶段为 WON 的数量.
     */
    private long won;

    /**
     * 该时间桶新增且当前阶段为 WON 或 LOST 的数量.
     */
    private long decided;

    /**
     * 该时间桶新增的金额合计（本位币 USD）.
     */
    private BigDecimal amountBase = BigDecimal.ZERO;

}
