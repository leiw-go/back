/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 金额折算出参.
 *
 * @since 2026-11-08
 */
@Data
public class CrmCurrencyConvertDto {

    /**
     * 原币金额
     */
    private BigDecimal amount;

    /**
     * 源币
     */
    private String fromCurrency;

    /**
     * 目标币
     */
    private String toCurrency;

    /**
     * 折算日期
     */
    private LocalDate  date;

    /**
     * 使用的汇率
     */
    private BigDecimal rate;

    /**
     * 折算结果
     */
    private BigDecimal convertedAmount;

    /**
     * 是否没有可用汇率
     */
    private boolean rateMissing;

}
