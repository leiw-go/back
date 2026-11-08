/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 产品定价相关的只读信息，供报价算价引擎做成本价、币种、MOQ 与在售校验。
 *
 * @since 2026-11-08
 */
@Data
public class ProductPricingInfo {

    /**
     * 产品 ID
     */
    private String productId;

    /**
     * 成本单价；为 null 表示未维护成本价，该产品不能参与报价
     */
    private BigDecimal costPrice;

    /**
     * 成本价币种（ISO 4217）；必须与报价单币种相同
     */
    private String costCurrency;

    /**
     * 起订量；为 null 表示不做 MOQ 校验
     */
    private Integer moq;

    /**
     * 是否在售
     */
    private boolean onSale;

}
