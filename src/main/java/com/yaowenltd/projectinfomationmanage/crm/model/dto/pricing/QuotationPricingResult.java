/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/**
 * 报价算价结果，含单头金额与利润汇总。
 *
 * @since 2026-11-08
 */
@Data
public class QuotationPricingResult {

    /**
     * 逐行算价结果
     */
    private java.util.List<PricedItem> items;

    /**
     * 报价总额（原币）
     */
    private BigDecimal totalAmount;

    /**
     * 成本汇总（原币）
     */
    private BigDecimal totalCost;

    /**
     * 毛利额
     */
    private BigDecimal profitAmount;

    /**
     * 毛利率（%）
     */
    private BigDecimal profitRate;

}
