/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 汇率交叉比对明细化出参.
 *
 * @since 2026-11-08
 */
@Data
public class CrmCurrencyRateCheckDto {

    /**
     * 明细 ID
     */
    private String id;

    /**
     * 批次 ID
     */
    private String syncId;

    /**
     * 基准币
     */
    private String baseCurrency;

    /**
     * 目标币
     */
    private String quoteCurrency;

    /**
     * 主源值
     */
    private BigDecimal primaryRate;

    /**
     * 主源日期
     */
    private LocalDate  primaryDate;

    /**
     * 比对源值
     */
    private BigDecimal secondaryRate;

    /**
     * 比对源日期
     */
    private LocalDate  secondaryDate;

    /**
     * 偏差（%）
     */
    private BigDecimal deviationRate;

    /**
     * 日期差
     */
    private Integer dateGapDays;

    /**
     * 适用阈值
     */
    private BigDecimal threshold;

    /**
     * 比对结果
     */
    private String checkResult;

    /**
     * 拒绝原因码
     */
    private String rejectReason;

    /**
     * 比对时间
     */
    private LocalDateTime checkedAt;

}
