/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 看板聚合结果（计数 + 金额）.
 *
 * @since 2026-11-08
 */
@Data
public class CrmDashboardAggregateDto {

    /**
     * 计数.
     */
    private long count;

    /**
     * 金额合计（本位币 USD）.
     */
    private BigDecimal amountBase = BigDecimal.ZERO;

}
