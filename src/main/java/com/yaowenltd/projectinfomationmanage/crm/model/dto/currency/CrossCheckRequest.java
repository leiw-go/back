/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.currency;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 两源汇率交叉比对请求。
 *
 * @since 2026-11-08
 */
@Data
public class CrossCheckRequest {

    /**
     * 主源报价
     */
    private CurrencyQuote primary;

    /**
     * 比对源报价
     */
    private CurrencyQuote secondary;

    /**
     * 本次适用的偏差阈值（%）
     */
    private BigDecimal threshold;

    /**
     * 允许的最大数据日期差（天），超过则跳过比对
     */
    private int maxDateGapDays;

    /**
     * 是否核心币种；核心币种在日期差过大时阈值放宽一档
     */
    private boolean coreCurrency;

}
