/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.product;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 产品多币种参考价出参.
 *
 * @since 2026-11-08
 */
@Data
public class CrmProductPriceDto {

    /**
     * 参考价 ID
     */
    private String id;

    /**
     * 产品 ID
     */
    private String productId;

    /**
     * 币种
     */
    private String currencyCode;

    /**
     * 参考销售单价
     */
    private BigDecimal price;

    /**
     * 阶梯起点
     */
    private Integer minQuantity;

    /**
     * 生效日
     */
    private LocalDate validFrom;

    /**
     * 失效日
     */
    private LocalDate validTo;

}
