/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 汇率出参.
 *
 * @since 2026-11-08
 */
@Data
public class CrmCurrencyRateDto {

    /**
     * 汇率 ID
     */
    private String id;

    /**
     * 基准币
     */
    private String baseCurrency;

    /**
     * 目标币
     */
    private String quoteCurrency;

    /**
     * 汇率
     */
    private BigDecimal rate;

    /**
     * 汇率日期
     */
    private LocalDate  rateDate;

    /**
     * 来源
     */
    private String source;

    /**
     * 比对源标识
     */
    private String crossSource;

    /**
     * 比对结果
     */
    private String crossStatus;

    /**
     * 偏差（%）
     */
    private BigDecimal deviationRate;

    /**
     * 日期差
     */
    private Integer dateGapDays;

}
