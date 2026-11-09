/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.entity;

import lombok.Data;

/**
 * CRM 币种实体，对应表 t_currency.
 *
 * @since 2026-11-08
 */
@Data
public class CrmCurrencyEntity {

    /**
     * 主键
     */
    private String id;

    /**
     * ISO 4217 三字母码
     */
    private String currencyCode;

    /**
     * 币种名称
     */
    private String currencyName;

    /**
     * 符号
     */
    private String symbol;

    /**
     * ISO 4217 数字码
     */
    private String numericCode;

    /**
     * 常用币标记
     */
    private Integer isCommon;

    /**
     * 分级 CORE / NORMAL
     */
    private String currencyLevel;

    /**
     * 状态 NORMAL / SUSPECT / DISABLED
     */
    private String status;

    /**
     * 连续超标天数
     */
    private Integer suspectDays;

    /**
     * 连续达标天数
     */
    private Integer recoverDays;

}
