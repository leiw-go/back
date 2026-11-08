/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.enums.OpportunityStage;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.OpportunityStageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OpportunityStageService} 的纯 JUnit 5 单元测试。
 * <p>
 * 契约来自需求文档 §3.3：
 * INITIAL_CONTACT → QUALIFIED → QUOTED → NEGOTIATION → WON 为标准漏斗，逐级推进不可跳级；
 * 任意阶段都可以转 LOST，但必须填丢单原因；WON 与 LOST 为终态。
 * </p>
 */
class OpportunityStageServiceTests {

    private OpportunityStageService stageService;

    @BeforeEach
    void setUp() {
        stageService = new OpportunityStageServiceImpl();
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1} 允许流转")
    @CsvSource({
            "INITIAL_CONTACT,QUALIFIED",
            "QUALIFIED,QUOTED",
            "QUOTED,NEGOTIATION",
            "NEGOTIATION,WON"
    })
    @DisplayName("标准漏斗逐级推进应被允许")
    void testForwardTransitionsAreAllowed(OpportunityStage from, OpportunityStage to) {
        assertTrue(stageService.canTransit(from, to),
                from + " -> " + to + " should be a legal transition");
        assertDoesNotThrow(() -> stageService.assertCanTransit(from, to, null));
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1} 非法流转")
    @CsvSource({
            "INITIAL_CONTACT,QUOTED",
            "INITIAL_CONTACT,NEGOTIATION",
            "INITIAL_CONTACT,WON",
            "QUALIFIED,WON",
            "QUOTED,WON",
            "NEGOTIATION,QUALIFIED",
            "QUALIFIED,INITIAL_CONTACT"
    })
    @DisplayName("跳级与逆向流转应被拒绝")
    void testSkippingAndBackwardTransitionsAreRejected(OpportunityStage from, OpportunityStage to) {
        assertFalse(stageService.canTransit(from, to),
                from + " -> " + to + " should be an illegal transition");
        assertThrows(IllegalArgumentException.class,
                () -> stageService.assertCanTransit(from, to, null));
    }

    @ParameterizedTest(name = "[{index}] {0} -> LOST 允许")
    @EnumSource(value = OpportunityStage.class, names = {"INITIAL_CONTACT", "QUALIFIED", "QUOTED", "NEGOTIATION"})
    @DisplayName("任意进行中阶段都可以转 LOST")
    void testAnyStageCanBeLost(OpportunityStage from) {
        assertTrue(stageService.canTransit(from, OpportunityStage.LOST),
                from + " -> LOST should be a legal transition");
    }

    @Test
    @DisplayName("转为 LOST 必须填丢单原因")
    void testLostReasonIsMandatory() {
        assertThrows(IllegalArgumentException.class,
                () -> stageService.assertCanTransit(OpportunityStage.QUALIFIED, OpportunityStage.LOST, null));
        assertThrows(IllegalArgumentException.class,
                () -> stageService.assertCanTransit(OpportunityStage.QUALIFIED, OpportunityStage.LOST, "  "));
        assertDoesNotThrow(
                () -> stageService.assertCanTransit(OpportunityStage.QUALIFIED, OpportunityStage.LOST, "价格谈不拢"));
    }

    @ParameterizedTest(name = "[{index}] {0} 是终态")
    @EnumSource(value = OpportunityStage.class, names = {"WON", "LOST"})
    @DisplayName("赢单与丢单为终态")
    void testTerminalStagesCannotTransit(OpportunityStage terminal) {
        for (OpportunityStage target : OpportunityStage.values()) {
            assertFalse(stageService.canTransit(terminal, target),
                    terminal + " is terminal and must not transit to " + target);
        }
    }

    @Test
    @DisplayName("流转到自身应被拒绝")
    void testSelfTransitionIsRejected() {
        assertFalse(stageService.canTransit(OpportunityStage.QUALIFIED, OpportunityStage.QUALIFIED));
    }
}
