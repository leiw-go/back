/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 报价算价的单个明细输入。
 *
 * @since 2026-11-08
 */
@Data
public class PricingItemRequest {

    /**
     * 产品信息（必填）
     */
    private ProductPricingInfo product;

    /**
     * 数量
     */
    private BigDecimal quantity;

    /**
     * 行覆盖加价率；为 null 时用报价单级默认值
     */
    private BigDecimal markupRate;

    /**
     * 行覆盖折扣率；为 null 时用报价单级默认值
     */
    private BigDecimal discountRate;

    /**
     * 手工指定的单价；非 null 时视为手工改价，不再随加价率/折扣重算
     */
    private BigDecimal manualUnitPrice;

}
