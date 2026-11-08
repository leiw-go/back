/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 汇率查询与金额折算.<p>按需求文档 §5.5：指定日期取当天汇率，取不到则取该日期之前最近一条；交叉汇率走 USD 三角折算。
 *
 * @since 2026-11-08
 */
public interface CurrencyRateQueryService {

    /**
     * 查询某币种对在指定日期的汇率.
     *
     * @param baseCurrency  基准币
     * @param quoteCurrency 目标币
     * @param date          日期
     * @return 汇率；无任何可用汇率时返回 null
     */
    BigDecimal findRateOn(String baseCurrency, String quoteCurrency, LocalDate date);

    /**
     * 按汇率折算金额.
     *
     * @param amount     原币金额
     * @param from       原币
     * @param to         目标币
     * @param date       汇率日期
     * @return 折算结果；无可用汇率时返回 null（调用方需标注 rateMissing）
     */
    BigDecimal convert(BigDecimal amount, String from, String to, LocalDate date);
}
