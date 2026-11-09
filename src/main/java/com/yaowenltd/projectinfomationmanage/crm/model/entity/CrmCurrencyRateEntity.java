/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 汇率实体，对应表 t_currency_rate.
 *
 * @since 2026-11-08
 */
@Data
public class CrmCurrencyRateEntity {

    /**
     * 主键
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
     * 来源 FRANKFURTER / OPEN_ER_API / MANUAL
     */
    private String source;

    /**
     * 比对源标识
     */
    private String crossSource;

    /**
     * 比对结果 MATCH / WARN / SINGLE / REJECT / MANUAL
     */
    private String crossStatus;

    /**
     * 与比对源的偏差（%）
     */
    private BigDecimal deviationRate;

    /**
     * 两源数据日期差（天）
     */
    private Integer dateGapDays;

    /**
     * 交叉比对时间
     */
    private LocalDateTime checkedAt;

}
