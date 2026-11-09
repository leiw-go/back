/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.entity;

import java.math.BigDecimal;
import lombok.Data;

/**
 * CRM 订单明细实体，对应表 t_order_item.
 *
 * @since 2026-11-08
 */
@Data
public class CrmOrderItemEntity {

    /**
     * 主键
     */
    private String id;
    /**
     * 订单 → t_order.id
     */
    private String orderId;
    /**
     * 产品 → t_crm_product.id
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
     * 单价（由报价单快照而来）
     */
    private BigDecimal unitPrice;
    /**
     * 行金额（由报价单快照而来）
     */
    private BigDecimal amount;
    /**
     * 逻辑删除：0 未删除；删除时写入毫秒时间戳
     */
    private Long deleted;
}
