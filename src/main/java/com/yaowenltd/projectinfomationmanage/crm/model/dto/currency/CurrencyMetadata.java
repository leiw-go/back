/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.currency;

import lombok.Data;

/**
 * 币种元数据（来自主源的币种字典）.
 *
 * @since 2026-11-08
 */
@Data
public class CurrencyMetadata {

    /** ISO 4217 三字母码. */
    private String currencyCode;

    /** 币种名称. */
    private String currencyName;

    /** 符号. */
    private String symbol;

    /** ISO 4217 数字码. */
    private String numericCode;

}
