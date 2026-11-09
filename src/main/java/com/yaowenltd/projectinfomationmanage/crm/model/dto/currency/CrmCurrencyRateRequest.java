/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.currency;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 汇率手工维护入参.
 *
 * @since 2026-11-08
 */
@Data
public class CrmCurrencyRateRequest {

    /**
     * 目标币
     */
    private String quoteCurrency;

    /**
     * 汇率
     */
    private BigDecimal rate;

    /**
     * 汇率日期；为空时取当天
     */
    private LocalDate  rateDate;

}
