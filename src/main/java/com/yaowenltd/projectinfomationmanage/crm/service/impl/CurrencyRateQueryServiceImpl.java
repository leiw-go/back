/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CurrencyRateMapper;
import com.yaowenltd.projectinfomationmanage.crm.service.CurrencyRateQueryService;
import org.springframework.stereotype.Service;

/**
 * 汇率查询与金额折算实现.
 *
 * @since 2026-11-08
 */
@Service
public class CurrencyRateQueryServiceImpl implements CurrencyRateQueryService {

    /**
     * 汇率 Mapper.
     */
    private final CurrencyRateMapper currencyRateMapper;
    /**
     * 构造 CurrencyRateQueryServiceImpl.
     *
     * @param currencyRateMapper 汇率 Mapper
     */
    public CurrencyRateQueryServiceImpl(CurrencyRateMapper currencyRateMapper) {
        this.currencyRateMapper = currencyRateMapper;
    }

    /**
     * 查询某币种对在指定日期的汇率.
     */
    @Override
    public BigDecimal findRateOn(String baseCurrency, String quoteCurrency, LocalDate date) {
        throw new UnsupportedOperationException("TDD: 待实现");
    }

    /**
     * 按汇率折算金额.
     */
    @Override
    public BigDecimal convert(BigDecimal amount, String from, String to, LocalDate date) {
        throw new UnsupportedOperationException("TDD: 待实现");
    }
}
