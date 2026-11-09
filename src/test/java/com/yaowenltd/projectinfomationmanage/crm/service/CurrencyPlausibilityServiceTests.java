/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.config.CrmCurrencyProperties;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CurrencyQuote;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.PlausibilityOutcome;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.CurrencyPlausibilityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CurrencyPlausibilityService} 的纯 JUnit 5 单元测试.
 * <p>
 * 契约来自汇率专项文档 §4.2：值域、正数、日期不超前、日期不太旧、环比突变。
 * 合理性校验是**不依赖第二源**的第二道闸门 —— 用来抓"两个源一起错"的情况。
 */
class CurrencyPlausibilityServiceTests {

    private CurrencyPlausibilityService plausibilityService;

    @BeforeEach
    void setUp() {
        plausibilityService = new CurrencyPlausibilityServiceImpl(new CrmCurrencyProperties());
    }

    private static CurrencyQuote quote(String code, String rate, LocalDate date) {
        CurrencyQuote q = new CurrencyQuote();
        q.setCurrencyCode(code);
        q.setRate(new BigDecimal(rate));
        q.setRateDate(date);
        return q;
    }

    @Test
    @DisplayName("正常汇率通过全部校验")
    void testNormalRatePasses() {
        PlausibilityOutcome outcome = plausibilityService.check(
                quote("CNY", "6.6635", LocalDate.now()), null);

        assertTrue(outcome.isPassed());
        assertFalse(outcome.isWarn());
    }

    @Test
    @DisplayName("汇率非正数被拒绝")
    void testNonPositiveRateRejected() {
        PlausibilityOutcome outcome = plausibilityService.check(
                quote("CNY", "0", LocalDate.now()), null);

        assertFalse(outcome.isPassed());
        assertEquals("RATE_NOT_POSITIVE", outcome.getReasonCode());
    }

    @Test
    @DisplayName("汇率超出值域被拒绝")
    void testOutOfRangeRateRejected() {
        PlausibilityOutcome outcome = plausibilityService.check(
                quote("CNY", "999999", LocalDate.now()), null);

        assertFalse(outcome.isPassed());
        assertEquals("RATE_OUT_OF_RANGE", outcome.getReasonCode());
    }

    @Test
    @DisplayName("日期超前的汇率被拒绝")
    void testFutureDateRejected() {
        PlausibilityOutcome outcome = plausibilityService.check(
                quote("CNY", "6.66", LocalDate.now().plusDays(1)), null);

        assertFalse(outcome.isPassed());
        assertEquals("DATE_IN_FUTURE", outcome.getReasonCode());
    }

    @Test
    @DisplayName("数据过旧的汇率被拒绝")
    void testStaleDateRejected() {
        PlausibilityOutcome outcome = plausibilityService.check(
                quote("CNY", "6.66", LocalDate.now().minusDays(30)), null);

        assertFalse(outcome.isPassed());
        assertEquals("DATE_TOO_OLD", outcome.getReasonCode());
    }

    @Test
    @DisplayName("环比变动超过阈值时通过但标记 WARN")
    void testDailyChangeTooLargeIsWarned() {
        PlausibilityOutcome outcome = plausibilityService.check(
                quote("CNY", "13.32", LocalDate.now()), new BigDecimal("6.66"));

        assertTrue(outcome.isPassed(), "环比突变属于入库但标记，不是直接拒绝");
        assertTrue(outcome.isWarn());
        assertEquals("DAILY_CHANGE_TOO_LARGE", outcome.getReasonCode());
    }

    @Test
    @DisplayName("环比变动在阈值内不算警告")
    void testNormalDailyChangeIsNotWarned() {
        PlausibilityOutcome outcome = plausibilityService.check(
                quote("CNY", "6.70", LocalDate.now()), new BigDecimal("6.66"));

        assertTrue(outcome.isPassed());
        assertFalse(outcome.isWarn());
    }

    @Test
    @DisplayName("汇率为空时拒绝")
    void testMissingRateRejected() {
        CurrencyQuote q = new CurrencyQuote();
        q.setCurrencyCode("CNY");
        q.setRateDate(LocalDate.now());

        PlausibilityOutcome outcome = plausibilityService.check(q, null);

        assertFalse(outcome.isPassed());
        assertEquals("RATE_MISSING", outcome.getReasonCode());
    }

}
