/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.currency;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 汇率同步批次摘要.
 *
 * @since 2026-11-08
 */
@Data
public class CrmCurrencySyncStatusDto {

    /**
     * 批次 ID
     */
    private String syncId;

    /**
     * 触发方式
     */
    private String triggerType;

    /**
     * 批次状态
     */
    private String syncStatus;

    /**
     * 主源状态
     */
    private String primaryStatus;

    /**
     * 主源条数
     */
    private Integer primaryRows;

    /**
     * 主源数据日期
     */
    private LocalDate  primaryDate;

    /**
     * 比对源状态
     */
    private String secondaryStatus;

    /**
     * 比对源条数
     */
    private Integer secondaryRows;

    /**
     * 比对源快照日期
     */
    private LocalDate  secondaryDate;

    /**
     * 参与比对的币种数
     */
    private Integer comparedCount;

    /**
     * 一致数
     */
    private Integer matchedCount;

    /**
     * 警告数
     */
    private Integer warnedCount;

    /**
     * 拒绝数
     */
    private Integer rejectedCount;

    /**
     * 单源数
     */
    private Integer singleCount;

    /**
     * 入库数
     */
    private Integer savedCount;
    private Integer skippedCount;

    /**
     * 最大偏差（%）
     */
    private BigDecimal maxDeviation;

    /**
     * 最大偏差对应币种
     */
    private String maxDeviationCurrency;

    /**
     * 失败原因
     */
    private String errorMsg;

    /**
     * 耗时
     */
    private Integer durationMs;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 结束时间
     */
    private LocalDateTime finishedAt;

    /**
     * 是否陈旧（距上次同步超过一天）
     */
    private boolean stale;

}
