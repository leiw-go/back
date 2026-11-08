/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 产品多币种参考价实体，对应表 t_crm_product_price.
 *
 * @since 2026-11-08
 */
@Data
public class CrmProductPriceEntity {

    /**
     * 主键
     */
    private String id;

    /**
     * 产品 → t_crm_product.id
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

    /**
     * 逻辑删除：0 未删除（参考价走物理删除，该列恒为 0）
     */
    private Long deleted;

}
