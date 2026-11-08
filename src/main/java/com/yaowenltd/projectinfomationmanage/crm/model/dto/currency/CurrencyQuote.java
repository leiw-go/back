/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * 某币种在某一天的汇率报价。
 *
 * @since 2026-11-08
 */
@Data
public class CurrencyQuote {

    /**
     * 币种代码（ISO 4217）
     */
    private String currencyCode;

    /**
     * 汇率（1 单位基准币可兑换的目标币数量）
     */
    private BigDecimal rate;

    /**
     * 汇率日期
     */
    private java.time.LocalDate rateDate;

}
