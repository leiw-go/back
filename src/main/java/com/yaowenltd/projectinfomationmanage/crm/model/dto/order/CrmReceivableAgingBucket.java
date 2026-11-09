/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.order;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 单个账龄区间.
 *
 * @since 2026-11-08
 */
@Data
public class CrmReceivableAgingBucket {

    /**
     * 区间标识 0-30 / 31-60 / 61-90 / 90+
     */
    private String bucket;

    /**
     * 区间中文名
     */
    private String label;

    /**
     * 未回款金额
     */
    private BigDecimal amount;

    /**
     * 笔数
     */
    private long count;

}
