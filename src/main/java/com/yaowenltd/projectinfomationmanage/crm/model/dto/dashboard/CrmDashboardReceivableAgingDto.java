/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 应收账龄统计结果.
 * <p>
 * 只统计**未收齐**的应收（{@code t_receivable.received_amount < receivable_amount}），
 * 金额统一给本位币 USD 口径；分桶依据是到期日与今天的天数差（逾期天数），
 * 详见 {@link com.yaowenltd.projectinfomationmanage.crm.model.enums.ReceivableAgingBucket}。
 * </p>
 *
 * @since 2026-11-08
 */
@Data
public class CrmDashboardReceivableAgingDto {

    /**
     * 未回款笔数合计.
     */
    private long totalCount;

    /**
     * 未回款金额合计（本位币 USD）.
     */
    private BigDecimal totalAmountBase = BigDecimal.ZERO;

    /**
     * 各账龄桶（固定 4 个桶，没有数据时笔数与金额为 0）.
     */
    private List<CrmReceivableAgingBucketDto> buckets = new ArrayList<>();

}
