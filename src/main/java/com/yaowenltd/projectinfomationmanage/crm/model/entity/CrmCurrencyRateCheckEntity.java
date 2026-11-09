/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 汇率交叉比对明细实体，对应表 t_currency_rate_check.
 *
 * @since 2026-11-08
 */
@Data
public class CrmCurrencyRateCheckEntity {

    /**
     * 主键
     */
    private String id;

    /**
     * 关联同步批次 ID
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
     * 主源该币种的日期
     */
    private LocalDate  primaryDate;

    /**
     * 比对源值
     */
    private BigDecimal secondaryRate;

    /**
     * 比对源快照日期
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
     * 本次适用的阈值
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
