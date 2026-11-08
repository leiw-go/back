/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.mapper.CurrencyRateMapper;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.CurrencyRateQueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CurrencyRateQueryService} 的单元测试。
 * <p>
 * 契约来自需求文档 §5.5：
 * 指定日期取当天汇率，取不到则取该日期之前最近一条；
 * 交叉汇率走 USD 三角折算；完全无汇率时返回 null（由调用方标注 rateMissing）。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class CurrencyRateQueryServiceTests {

    @Mock
    private CurrencyRateMapper currencyRateMapper;

    private CurrencyRateQueryService rateQueryService;

    @BeforeEach
    void setUp() {
        rateQueryService = new CurrencyRateQueryServiceImpl(currencyRateMapper);
    }

    @Test
    @DisplayName("取汇率应委托 Mapper 查询当天或之前最近一条")
    void testFindRateDelegatesToMapper() {
        LocalDate date = LocalDate.of(2026, 11, 8);
        when(currencyRateMapper.selectLatestRateOnOrBefore("USD", "CNY", date)).thenReturn(new BigDecimal("6.6635"));

        BigDecimal rate = rateQueryService.findRateOn("USD", "CNY", date);

        assertEquals(0, new BigDecimal("6.6635").compareTo(rate));
        verify(currencyRateMapper).selectLatestRateOnOrBefore("USD", "CNY", date);
    }

    @Test
    @DisplayName("无任何可用汇率时返回 null")
    void testFindRateReturnsNullWhenNothingFound() {
        LocalDate date = LocalDate.of(2026, 11, 8);
        when(currencyRateMapper.selectLatestRateOnOrBefore(eq("USD"), eq("CNY"), any())).thenReturn(null);

        assertNull(rateQueryService.findRateOn("USD", "CNY", date));
    }

    @Test
    @DisplayName("同币种折算返回原值")
    void testConvertSameCurrencyReturnsAmount() {
        BigDecimal amount = new BigDecimal("1250.50");

        assertEquals(0, amount.compareTo(rateQueryService.convert(amount, "USD", "USD", LocalDate.of(2026, 11, 8))));
        verify(currencyRateMapper, never()).selectLatestRateOnOrBefore(any(), any(), any());
    }

    @Test
    @DisplayName("美元折算为目标币种 金额乘以汇率")
    void testConvertFromUsd() {
        LocalDate date = LocalDate.of(2026, 11, 8);
        when(currencyRateMapper.selectLatestRateOnOrBefore("USD", "CNY", date)).thenReturn(new BigDecimal("6.6635"));

        BigDecimal converted = rateQueryService.convert(new BigDecimal("100"), "USD", "CNY", date);

        assertEquals(0, new BigDecimal("666.35").compareTo(converted));
    }

    @Test
    @DisplayName("目标币种为美元时 金额除以汇率")
    void testConvertToUsd() {
        LocalDate date = LocalDate.of(2026, 11, 8);
        when(currencyRateMapper.selectLatestRateOnOrBefore("USD", "CNY", date)).thenReturn(new BigDecimal("6.6635"));

        BigDecimal converted = rateQueryService.convert(new BigDecimal("666.35"), "CNY", "USD", date);

        assertEquals(0, new BigDecimal("100.00").compareTo(converted));
    }

    @Test
    @DisplayName("交叉币种走 USD 三角折算")
    void testConvertCrossCurrencyUsesUsdTriangle() {
        LocalDate date = LocalDate.of(2026, 11, 8);
        when(currencyRateMapper.selectLatestRateOnOrBefore("USD", "CNY", date)).thenReturn(new BigDecimal("6.6635"));
        when(currencyRateMapper.selectLatestRateOnOrBefore("USD", "EUR", date)).thenReturn(new BigDecimal("0.89169"));

        BigDecimal converted = rateQueryService.convert(new BigDecimal("100"), "CNY", "EUR", date);

        // 100 / 6.6635 * 0.89169 = 13.38...
        assertEquals(0, new BigDecimal("13.38").compareTo(converted.setScale(2, java.math.RoundingMode.HALF_UP)));
    }

    @Test
    @DisplayName("缺少任一方向的汇率时折算返回 null")
    void testConvertReturnsNullWhenRateMissing() {
        LocalDate date = LocalDate.of(2026, 11, 8);
        when(currencyRateMapper.selectLatestRateOnOrBefore("USD", "CNY", date)).thenReturn(null);

        assertNull(rateQueryService.convert(new BigDecimal("100"), "CNY", "EUR", date));
    }
}
