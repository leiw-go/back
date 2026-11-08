/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.entity;

import java.math.BigDecimal;
import lombok.Data;

/**
 * CRM 产品实体，对应表 t_crm_product.
 *
 * @since 2026-11-08
 */
@Data
public class CrmProductEntity {

    /**
     * 主键
     */
    private String id;

    /**
     * 产品编码（业务唯一）
     */
    private String productCode;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 分类，字典 product_category
     */
    private String category;

    /**
     * 规格描述
     */
    private String specification;

    /**
     * 单位（PCS/KG/CBM/SET）
     */
    private String unit;

    /**
     * 单件重量（kg）
     */
    private BigDecimal weightKg;

    /**
     * 单件体积（CBM）
     */
    private BigDecimal volumeCbm;

    /**
     * 成本单价（币种见 costCurrency）
     */
    private BigDecimal costPrice;

    /**
     * 成本价币种
     */
    private String costCurrency;

    /**
     * 默认销售价（参考）
     */
    private BigDecimal defaultPrice;

    /**
     * 默认币种（参考）
     */
    private String defaultCurrency;

    /**
     * 起订量
     */
    private Integer moq;

    /**
     * HS 海关编码
     */
    private String hsCode;

    /**
     * 产地
     */
    private String origin;

    /**
     * 状态：1 在售 / 0 停售
     */
    private Integer productStatus;

    /**
     * 备注
     */
    private String remark;

    /**
     * 逻辑删除：0 未删除；删除时写入毫秒时间戳
     */
    private Long deleted;

}
