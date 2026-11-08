/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 商机出参.
 *
 * @since 2026-11-08
 */
@Data
public class CrmOpportunityDto {

    /**
     * 商机 ID
     */
    private String id;

    /**
     * 客户 ID
     */
    private String customerId;

    /**
     * 商机名称
     */
    private String name;

    /**
     * 阶段
     */
    private String stage;

    /**
     * 预计金额（原币）
     */
    private BigDecimal amount;

    /**
     * 原币币种
     */
    private String currencyCode;

    /**
     * 本位币（USD）快照
     */
    private BigDecimal amountBase;

    /**
     * 赢率 0-100
     */
    private Integer winRate;

    /**
     * 预计成交日
     */
    private LocalDate expectCloseDate;

    /**
     * 竞争对手
     */
    private String competitor;

    /**
     * 丢单原因
     */
    private String lostReason;

    /**
     * 负责人 ID
     */
    private String ownerId;

    /**
     * 备注
     */
    private String remark;

}
