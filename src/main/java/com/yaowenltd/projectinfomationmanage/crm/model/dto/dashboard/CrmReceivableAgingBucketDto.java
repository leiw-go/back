/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 应收账龄的单个分桶.
 *
 * @since 2026-11-08
 */
@Data
public class CrmReceivableAgingBucketDto {

    /**
     * 桶标识：{@code 0-30} / {@code 31-60} / {@code 61-90} / {@code 90+}.
     */
    private String bucket;

    /**
     * 桶文案：{@code 0-30 天} / {@code 31-60 天} / {@code 61-90 天} / {@code 90 天以上}.
     */
    private String label;

    /**
     * 该桶的未回款笔数.
     */
    private long count;

    /**
     * 该桶的未回款金额合计（本位币 USD）.
     */
    private BigDecimal amountBase = BigDecimal.ZERO;

}
