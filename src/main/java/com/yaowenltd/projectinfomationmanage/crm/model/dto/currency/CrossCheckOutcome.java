/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.currency;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 两源汇率交叉比对结果。
 *
 * @since 2026-11-08
 */
@Data
public class CrossCheckOutcome {

    /**
     * 偏差（%）
     */
    private BigDecimal deviation;

    /**
     * 比对结果
     */
    private com.yaowenltd.projectinfomationmanage.crm.model.enums.CurrencyCheckResult result;

    /**
     * 本次实际适用的阈值（%）
     */
    private BigDecimal appliedThreshold;

    /**
     * 两源数据日期差（天）
     */
    private int dateGapDays;

    /**
     * 判定原因说明
     */
    private String reason;

}
