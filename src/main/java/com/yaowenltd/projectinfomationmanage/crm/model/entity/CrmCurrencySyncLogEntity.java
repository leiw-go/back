/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 汇率同步批次日志实体，对应表 t_currency_sync_log.
 *
 * @since 2026-11-08
 */
@Data
public class CrmCurrencySyncLogEntity {

    /**
     * 主键 = syncId
     */
    private String id;

    /**
     * 触发方式 SCHEDULE / MANUAL
     */
    private String triggerType;

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
     * 实际参与比对的币种数
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
     * 实际入库数
     */
    private Integer savedCount;
    private Integer skippedCount;

    /**
     * 本次最大偏差（%）
     */
    private BigDecimal maxDeviation;

    /**
     * 最大偏差对应币种
     */
    private String maxDeviationCurrency;

    /**
     * 批次状态 SUCCESS / PARTIAL / FAILED
     */
    private String syncStatus;

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

}
