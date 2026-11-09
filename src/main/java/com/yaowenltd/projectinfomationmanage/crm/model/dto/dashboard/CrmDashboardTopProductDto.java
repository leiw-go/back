/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard;

import java.math.BigDecimal;
import lombok.Data;

/**
 * 产品成交排行的单行.
 * <p>
 * 成交金额统一给本位币 USD 口径：订单明细 {@code t_order_item.amount} 是原币，
 * 按所属订单的 {@code total_amount_base / total_amount} 汇率推算。
 * 原币金额不汇总返回 —— 多个币种直接相加没有业务意义。
 * </p>
 *
 * @since 2026-11-08
 */
@Data
public class CrmDashboardTopProductDto {

    /**
     * 产品 ID（{@code t_crm_product.id}）；订单明细未关联产品目录时为空.
     */
    private String productId;

    /**
     * 产品名称：优先取产品目录名称，目录缺失时回退到订单明细的名称快照.
     */
    private String productName;

    /**
     * 产品编码（{@code t_crm_product.product_code}）；未关联产品目录时为空.
     */
    private String productCode;

    /**
     * 成交金额合计（本位币 USD）.
     */
    private BigDecimal dealAmountBase = BigDecimal.ZERO;

    /**
     * 成交数量合计.
     */
    private BigDecimal dealQuantity = BigDecimal.ZERO;

    /**
     * 成交订单数（按订单去重）.
     */
    private long orderCount;

}
