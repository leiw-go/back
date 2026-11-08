/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.enums.QuotationStatus;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.QuotationStatusServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link QuotationStatusService} 的纯 JUnit 5 单元测试。
 * <p>
 * 契约来自需求文档 §3.5：
 * DRAFT → SENT → CONFIRMED / REJECTED；只有 DRAFT 可改；
 * 转 REJECTED 必须填拒绝原因；超过有效期的 DRAFT / SENT 视为 EXPIRED。
 * </p>
 */
class QuotationStatusServiceTests {

    private QuotationStatusService quotationStatusService;

    @BeforeEach
    void setUp() {
        quotationStatusService = new QuotationStatusServiceImpl();
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1} 允许流转")
    @CsvSource({
            "DRAFT,SENT,",
            "SENT,CONFIRMED,",
            "SENT,REJECTED,价格谈不拢"
    })
    @DisplayName("合法流转应被允许")
    void testLegalTransitionsAreAllowed(QuotationStatus from, QuotationStatus to, String rejectReason) {
        assertTrue(quotationStatusService.canTransit(from, to),
                from + " -> " + to + " should be a legal transition");
        // 转 REJECTED 额外要求填拒绝原因，其余流转不需要
        assertDoesNotThrow(() -> quotationStatusService.assertCanTransit(from, to, rejectReason));
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1} 非法流转")
    @CsvSource({
            "DRAFT,CONFIRMED",
            "DRAFT,REJECTED",
            "DRAFT,EXPIRED",
            "SENT,DRAFT",
            "CONFIRMED,SENT",
            "REJECTED,SENT"
    })
    @DisplayName("非法流转应被拒绝")
    void testIllegalTransitionsAreRejected(QuotationStatus from, QuotationStatus to) {
        assertFalse(quotationStatusService.canTransit(from, to),
                from + " -> " + to + " should be an illegal transition");
        assertThrows(IllegalArgumentException.class,
                () -> quotationStatusService.assertCanTransit(from, to, null));
    }

    @Test
    @DisplayName("转为 REJECTED 必须填拒绝原因")
    void testRejectReasonIsMandatory() {
        assertThrows(IllegalArgumentException.class,
                () -> quotationStatusService.assertCanTransit(QuotationStatus.SENT, QuotationStatus.REJECTED, null));
        assertThrows(IllegalArgumentException.class,
                () -> quotationStatusService.assertCanTransit(QuotationStatus.SENT, QuotationStatus.REJECTED, " "));
        assertDoesNotThrow(
                () -> quotationStatusService.assertCanTransit(QuotationStatus.SENT, QuotationStatus.REJECTED, "价格过高"));
    }

    @ParameterizedTest(name = "[{index}] {0} 是终态")
    @EnumSource(value = QuotationStatus.class, names = {"CONFIRMED", "REJECTED"})
    @DisplayName("已确认与已拒绝为终态")
    void testTerminalStatusesCannotTransit(QuotationStatus terminal) {
        for (QuotationStatus target : QuotationStatus.values()) {
            assertFalse(quotationStatusService.canTransit(terminal, target),
                    terminal + " is terminal and must not transit to " + target);
        }
    }

    @Test
    @DisplayName("超过有效期的草稿与已发送报价单视为过期")
    void testExpiredWhenValidUntilPassed() {
        LocalDate today = LocalDate.of(2026, 11, 8);

        assertTrue(quotationStatusService.isExpired(QuotationStatus.DRAFT, today.minusDays(1), today));
        assertTrue(quotationStatusService.isExpired(QuotationStatus.SENT, today.minusDays(1), today));
    }

    @Test
    @DisplayName("有效期当天仍然有效")
    void testNotExpiredOnTheLastDay() {
        LocalDate today = LocalDate.of(2026, 11, 8);

        assertFalse(quotationStatusService.isExpired(QuotationStatus.DRAFT, today, today));
        assertFalse(quotationStatusService.isExpired(QuotationStatus.SENT, today, today));
    }

    @Test
    @DisplayName("有效期为空表示长期有效")
    void testNullValidUntilNeverExpires() {
        assertFalse(quotationStatusService.isExpired(QuotationStatus.DRAFT, null, LocalDate.of(2026, 11, 8)));
    }

    @Test
    @DisplayName("已确认与已拒绝的报价单不因过期而改变状态")
    void testTerminalStatusIsNeverExpired() {
        LocalDate today = LocalDate.of(2026, 11, 8);

        assertFalse(quotationStatusService.isExpired(QuotationStatus.CONFIRMED, today.minusDays(30), today));
        assertFalse(quotationStatusService.isExpired(QuotationStatus.REJECTED, today.minusDays(30), today));
    }
}
