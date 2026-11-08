/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.entity;

import java.math.BigDecimal;
import lombok.Data;

/**
 * CRM 报价明细实体，对应表 t_quotation_item.
 *
 * @since 2026-11-08
 */
@Data
public class CrmQuotationItemEntity {

    /**
     * 主键
     */
    private String id;

    /**
     * 报价单 → t_quotation.id
     */
    private String quotationId;

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
     * 单价（服务端算）
     */
    private BigDecimal unitPrice;

    /**
     * 行金额（服务端算）
     */
    private BigDecimal amount;

    /**
     * 1 = 手工改过单价，不随加价率 / 折扣重算
     */
    private Integer manualPrice;

    /**
     * 逻辑删除：0 未删除；删除时写入毫秒时间戳
     */
    private Long deleted;

}
