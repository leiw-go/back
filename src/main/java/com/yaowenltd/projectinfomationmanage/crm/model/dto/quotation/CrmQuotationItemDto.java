/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation;

import java.math.BigDecimal;
import lombok.Data;

/**
 * CRM 报价明细出参.
 *
 * @since 2026-11-08
 */
@Data
public class CrmQuotationItemDto {

    /**
     * 明细 ID
     */
    private String id;

    /**
     * 产品 ID
     */
    private String productId;

    /**
     * 名称快照
     */
    private String productName;

    /**
     * 规格快照
     */
    private String specification;

    /**
     * 数量
     */
    private BigDecimal quantity;

    /**
     * 单位快照
     */
    private String unit;

    /**
     * 成本单价快照
     */
    private BigDecimal costPrice;

    /**
     * 行覆盖加价率 %（空则用单头）
     */
    private BigDecimal markupRate;

    /**
     * 行覆盖折扣率 %（空则用单头）
     */
    private BigDecimal discountRate;

    /**
     * 单价
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
