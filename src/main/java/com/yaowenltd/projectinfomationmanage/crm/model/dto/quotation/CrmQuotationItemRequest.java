/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation;

import java.math.BigDecimal;
import lombok.Data;

/**
 * CRM 报价明细入参.
 * <p>
 * 注意：**不接受** {@code unitPrice} / {@code amount}（服务端算），
 * 除非该行 {@code manualPrice = true}（此时用 {@code unitPrice} 覆盖）。
 *
 * @since 2026-11-08
 */
@Data
public class CrmQuotationItemRequest {

    /**
     * 产品 ID
     */
    private String productId;

    /**
     * 数量（必须 >= 产品 MOQ）
     */
    private BigDecimal quantity;

    /**
     * 行覆盖加价率 %（空则用单头）
     */
    private BigDecimal markupRate;

    /**
     * 行覆盖折扣率 %（空则用单头）
     */
    private BigDecimal discountRate;

    /**
     * 手工指定的单价；仅当 {@code manualPrice = true} 时生效
     */
    private BigDecimal unitPrice;

    /**
     * 是否手工改价（true 时不随加价率 / 折扣重算）
     */
    private boolean manualPrice;

}
