/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 商机按阶段的聚合结果（漏斗统计用）.
 *
 * @since 2026-11-08
 */
@Data
public class CrmOpportunityStageCountDto {

    /**
     * 阶段码.
     */
    private String stage;

    /**
     * 该阶段的商机数.
     */
    private long stageCount;

    /**
     * 该阶段的商机金额合计（本位币 USD）.
     */
    private BigDecimal amountBase = BigDecimal.ZERO;

}
