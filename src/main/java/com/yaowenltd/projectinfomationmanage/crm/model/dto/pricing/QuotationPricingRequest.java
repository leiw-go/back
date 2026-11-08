/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/**
 * 报价算价请求。
 *
 * @since 2026-11-08
 */
@Data
public class QuotationPricingRequest {

    /**
     * 报价单币种（ISO 4217）
     */
    private String currencyCode;

    /**
     * 报价单级默认加价率（%）
     */
    private BigDecimal markupRate;

    /**
     * 报价单级默认折扣率（%）
     */
    private BigDecimal discountRate;

    /**
     * 报价明细列表
     */
    private java.util.List<PricingItemRequest> items;

}
