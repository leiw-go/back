/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.order;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/**
 * CRM 应收账龄分析.
 *
 * @since 2026-11-08
 */
@Data
public class CrmReceivableAgingDto {

    /**
     * 账龄分桶
     */
    private List<CrmReceivableAgingBucket> buckets;

    /**
     * 未回款总额
     */
    private BigDecimal totalOutstandingAmount;

    /**
     * 未回款笔数
     */
    private long totalOutstandingCount;

}
