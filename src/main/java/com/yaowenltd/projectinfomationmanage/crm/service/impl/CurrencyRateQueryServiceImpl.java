/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.yaowenltd.projectinfomationmanage.crm.mapper.CurrencyRateMapper;
import com.yaowenltd.projectinfomationmanage.crm.service.CurrencyRateQueryService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 汇率查询与金额折算实现.
 * <p>按需求文档 §5.5：指定日期取当天汇率，取不到则取该日期之前最近一条；
 * 交叉汇率走 USD 三角折算；完全无汇率时返回 null（由调用方标注 rateMissing）。
 * </p>
 *
 * @since 2026-11-08
 */
@Service
public class CurrencyRateQueryServiceImpl implements CurrencyRateQueryService {

    /** 本位币（折算的中间锚点）. */
    private static final String BASE_CURRENCY = "USD";

    /** 折算结果的小数位. */
    private static final int AMOUNT_SCALE = 2;

    /** 折算时的中间精度. */
    private static final int CALC_SCALE = 16;

    private final CurrencyRateMapper currencyRateMapper;

    /**
     * 使用指定的 Mapper 构造 CurrencyRateQueryServiceImpl.
     *
     * @param currencyRateMapper 汇率 Mapper
     */
    public CurrencyRateQueryServiceImpl(CurrencyRateMapper currencyRateMapper) {
        this.currencyRateMapper = currencyRateMapper;
    }

    @Override
    public BigDecimal findRateOn(String baseCurrency, String quoteCurrency, LocalDate date) {
        if (baseCurrency == null || quoteCurrency == null || date == null) {
            return null;
        }
        return currencyRateMapper.selectLatestRateOnOrBefore(baseCurrency, quoteCurrency, date);
    }

    @Override
    public BigDecimal convert(BigDecimal amount, String from, String to, LocalDate date) {
        if (amount == null || from == null || to == null || date == null) {
            return null;
        }
        if (from.equals(to)) {
            return amount.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        }
        if (BASE_CURRENCY.equals(from)) {
            BigDecimal rate = findRateOn(BASE_CURRENCY, to, date);
            return rate == null ? null
                    : amount.multiply(rate).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        }
        if (BASE_CURRENCY.equals(to)) {
            BigDecimal rate = findRateOn(BASE_CURRENCY, from, date);
            return rate == null || rate.signum() == 0 ? null
                    : amount.divide(rate, AMOUNT_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal fromRate = findRateOn(BASE_CURRENCY, from, date);
        BigDecimal toRate = findRateOn(BASE_CURRENCY, to, date);
        if (fromRate == null || toRate == null || fromRate.signum() == 0) {
            return null;
        }
        return amount.divide(fromRate, CALC_SCALE, RoundingMode.HALF_UP)
                .multiply(toRate)
                .setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
    }
}
