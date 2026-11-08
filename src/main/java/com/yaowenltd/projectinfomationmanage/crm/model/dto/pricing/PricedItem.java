/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 报价算价后的单行结果。
 *
 * @since 2026-11-08
 */
@Data
public class PricedItem {

    /**
     * 产品 ID
     */
    private String productId;

    /**
     * 成本单价快照
     */
    private BigDecimal costPrice;

    /**
     * 实际生效的加价率（%）
     */
    private BigDecimal markupRate;

    /**
     * 实际生效的折扣率（%）
     */
    private BigDecimal discountRate;

    /**
     * 行单价
     */
    private BigDecimal unitPrice;

    /**
     * 行金额
     */
    private BigDecimal amount;

    /**
     * 是否手工改价
     */
    private boolean manualPrice;

}
