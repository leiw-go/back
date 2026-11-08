/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.product;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

/**
 * CRM 产品出参.
 *
 * @since 2026-11-08
 */
@Data
public class CrmProductDto {

    /**
     * 产品 ID
     */
    private String id;

    /**
     * 产品编码
     */
    private String productCode;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 分类
     */
    private String category;

    /**
     * 规格描述
     */
    private String specification;

    /**
     * 单位
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
     * 成本单价
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
     * 多币种参考价（详情接口返回）
     */
    private List<CrmProductPriceDto> prices;

}
