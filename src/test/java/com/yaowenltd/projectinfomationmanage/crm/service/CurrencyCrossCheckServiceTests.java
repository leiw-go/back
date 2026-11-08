/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrossCheckOutcome;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrossCheckRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CurrencyQuote;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.CurrencyCheckResult;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.CurrencyCrossCheckServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CurrencyCrossCheckService} 的纯 JUnit 5 单元测试。
 * <p>
 * 契约来自汇率专项文档 §4：
 * 偏差 = |主源 - 比对源| / ((主源 + 比对源) / 2) × 100%；
 * 偏差不超过阈值判 MATCH，超过阈值判 REJECT；
 * 两源数据日期差超过上限时跳过比对判 SKIP；
 * 连续超标达到阈值天数触发熔断，连续达标达到阈值天数解除熔断。
 * </p>
 */
class CurrencyCrossCheckServiceTests {

    /** 核心币种偏差阈值（%）. */
    private static final BigDecimal CORE_THRESHOLD = new BigDecimal("1.0");

    /** 一般币种偏差阈值（%）. */
    private static final BigDecimal NORMAL_THRESHOLD = new BigDecimal("3.0");

    /** 允许的最大数据日期差（天）. */
    private static final int MAX_DATE_GAP_DAYS = 3;

    private CurrencyCrossCheckService crossCheckService;

    @BeforeEach
    void setUp() {
        crossCheckService = new CurrencyCrossCheckServiceImpl();
    }

    private static CurrencyQuote quote(String code, String rate, LocalDate date) {
        CurrencyQuote q = new CurrencyQuote();
        q.setCurrencyCode(code);
        q.setRate(new BigDecimal(rate));
        q.setRateDate(date);
        return q;
    }

    private static CrossCheckRequest request(CurrencyQuote primary, CurrencyQuote secondary,
                                             BigDecimal threshold, boolean core, LocalDate primaryDate,
                                             LocalDate secondaryDate) {
        CrossCheckRequest req = new CrossCheckRequest();
        req.setPrimary(primary);
        req.setSecondary(secondary);
        req.setThreshold(threshold);
        req.setCoreCurrency(core);
        req.setMaxDateGapDays(MAX_DATE_GAP_DAYS);
        return req;
    }

    private static CrossCheckRequest sameDateRequest(String primaryRate, String secondaryRate,
                                                      BigDecimal threshold, boolean core) {
        LocalDate date = LocalDate.of(2026, 11, 8);
        return request(quote("CNY", primaryRate, date), quote("CNY", secondaryRate, date),
                threshold, core, date, date);
    }

    private static void assertDeviationNear(BigDecimal actual, String expected) {
        BigDecimal tolerance = new BigDecimal("0.0001");
        BigDecimal diff = actual.subtract(new BigDecimal(expected)).abs();
        assertTrue(diff.compareTo(tolerance) <= 0,
                "deviation should be about " + expected + "% but was " + actual);
    }

    @Test
    @DisplayName("两源完全一致时偏差为 0 且判 MATCH")
    void testIdenticalRatesMatch() {
        CrossCheckOutcome outcome = crossCheckService.check(sameDateRequest("6.6635", "6.6635", CORE_THRESHOLD, true));

        assertEquals(CurrencyCheckResult.MATCH, outcome.getResult());
        assertEquals(0, BigDecimal.ZERO.compareTo(outcome.getDeviation()));
        assertEquals(0, CORE_THRESHOLD.compareTo(outcome.getAppliedThreshold()));
    }

    @Test
    @DisplayName("偏差用对称分母计算")
    void testDeviationUsesSymmetricDenominator() {
        CrossCheckOutcome outcome = crossCheckService.check(sameDateRequest("100", "101", CORE_THRESHOLD, true));

        assertDeviationNear(outcome.getDeviation(), "0.995025");
    }

    @Test
    @DisplayName("核心币种偏差在阈值内判 MATCH")
    void testCoreCurrencyWithinThresholdMatches() {
        CrossCheckOutcome outcome = crossCheckService.check(sameDateRequest("100", "100.5", CORE_THRESHOLD, true));

        assertEquals(CurrencyCheckResult.MATCH, outcome.getResult());
    }

    @Test
    @DisplayName("核心币种偏差超过阈值判 REJECT")
    void testCoreCurrencyBeyondThresholdIsRejected() {
        CrossCheckOutcome outcome = crossCheckService.check(sameDateRequest("100", "101.5", CORE_THRESHOLD, true));

        assertEquals(CurrencyCheckResult.REJECT, outcome.getResult());
        assertDeviationNear(outcome.getDeviation(), "1.488834");
    }

    @Test
    @DisplayName("一般币种使用更宽的阈值")
    void testNormalCurrencyUsesWiderThreshold() {
        CrossCheckOutcome outcome = crossCheckService.check(
                sameDateRequest("100", "101.5", NORMAL_THRESHOLD, false));

        assertEquals(CurrencyCheckResult.MATCH, outcome.getResult(),
                "1.48% should be accepted for a normal currency whose threshold is 3%");
    }

    @Test
    @DisplayName("一般币种偏差超过阈值判 REJECT")
    void testNormalCurrencyBeyondThresholdIsRejected() {
        CrossCheckOutcome outcome = crossCheckService.check(
                sameDateRequest("100", "104", NORMAL_THRESHOLD, false));

        assertEquals(CurrencyCheckResult.REJECT, outcome.getResult());
    }

    @Test
    @DisplayName("两源数据日期差超过上限时跳过比对")
    void testLargeDateGapIsSkipped() {
        LocalDate primaryDate = LocalDate.of(2026, 11, 4);
        LocalDate secondaryDate = LocalDate.of(2026, 11, 8);
        CrossCheckRequest req = request(
                quote("CNY", "6.6635", primaryDate), quote("CNY", "6.6908", secondaryDate),
                CORE_THRESHOLD, true, primaryDate, secondaryDate);

        CrossCheckOutcome outcome = crossCheckService.check(req);

        assertEquals(CurrencyCheckResult.SKIP, outcome.getResult(),
                "a 3-day gap beyond the limit must not be compared");
        assertEquals(4, outcome.getDateGapDays());
    }

    @Test
    @DisplayName("日期差在上限内时正常比对")
    void testDateGapWithinLimitIsCompared() {
        LocalDate primaryDate = LocalDate.of(2026, 11, 7);
        LocalDate secondaryDate = LocalDate.of(2026, 11, 8);
        CrossCheckRequest req = request(
                quote("CNY", "6.6635", primaryDate), quote("CNY", "6.6635", secondaryDate),
                CORE_THRESHOLD, true, primaryDate, secondaryDate);

        CrossCheckOutcome outcome = crossCheckService.check(req);

        assertEquals(CurrencyCheckResult.MATCH, outcome.getResult());
        assertEquals(1, outcome.getDateGapDays());
    }

    @Test
    @DisplayName("比对源缺该币种时判 SKIP")
    void testMissingSecondaryIsSkipped() {
        LocalDate date = LocalDate.of(2026, 11, 8);
        CrossCheckRequest req = request(quote("ZWL", "26.87", date), null,
                CORE_THRESHOLD, true, date, date);

        CrossCheckOutcome outcome = crossCheckService.check(req);

        assertEquals(CurrencyCheckResult.SKIP, outcome.getResult());
    }

    @Test
    @DisplayName("主源缺该币种时判 SKIP")
    void testMissingPrimaryIsSkipped() {
        LocalDate date = LocalDate.of(2026, 11, 8);
        CrossCheckRequest req = request(null, quote("CNY", "6.69", date),
                CORE_THRESHOLD, true, date, date);

        CrossCheckOutcome outcome = crossCheckService.check(req);

        assertEquals(CurrencyCheckResult.SKIP, outcome.getResult());
    }

    @Test
    @DisplayName("请求为 null 时不抛异常 返回 SKIP")
    void testNullRequestIsSkipped() {
        CrossCheckOutcome outcome = crossCheckService.check(null);

        assertEquals(CurrencyCheckResult.SKIP, outcome.getResult());
    }

    @Test
    @DisplayName("连续超标达到阈值天数应熔断")
    void testShouldSuspendWhenConsecutiveBreachesReachThreshold() {
        assertTrue(crossCheckService.shouldSuspend(3, 3), "3 consecutive breaches with a 3 day threshold should suspend");
        assertTrue(crossCheckService.shouldSuspend(4, 3), "more breaches than the threshold should suspend");
    }

    @Test
    @DisplayName("连续超标未达阈值天数不熔断")
    void testShouldNotSuspendBeforeThreshold() {
        assertTrue(!crossCheckService.shouldSuspend(2, 3), "2 consecutive breaches should not suspend yet");
        assertTrue(!crossCheckService.shouldSuspend(0, 3));
    }

    @Test
    @DisplayName("连续达标达到阈值天数应解除熔断")
    void testShouldRecoverWhenConsecutiveOkDaysReachThreshold() {
        assertTrue(crossCheckService.shouldRecover(5, 5), "5 consecutive ok days with a 5 day threshold should recover");
        assertTrue(crossCheckService.shouldRecover(6, 5));
    }

    @Test
    @DisplayName("连续达标未达阈值天数不解除熔断")
    void testShouldNotRecoverBeforeThreshold() {
        assertTrue(!crossCheckService.shouldRecover(4, 5), "4 consecutive ok days should not recover yet");
    }
}
