/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.enums.OrderStatus;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.OrderStatusServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link OrderStatusService} 的纯 JUnit 5 单元测试。
 * <p>
 * 契约来自需求文档 §3.7：
 * CONFIRMED → IN_PRODUCTION → SHIPPED → DELIVERED → COMPLETED；
 * CONFIRMED / IN_PRODUCTION 可取消；已发货、已完成、已取消均为终态。
 * </p>
 */
class OrderStatusServiceTests {

    private OrderStatusService orderStatusService;

    @BeforeEach
    void setUp() {
        orderStatusService = new OrderStatusServiceImpl();
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1} 允许流转")
    @CsvSource({
            "CONFIRMED,IN_PRODUCTION",
            "CONFIRMED,SHIPPED",
            "IN_PRODUCTION,SHIPPED",
            "SHIPPED,DELIVERED",
            "DELIVERED,COMPLETED",
            "CONFIRMED,CANCELLED",
            "IN_PRODUCTION,CANCELLED"
    })
    @DisplayName("合法流转应被允许")
    void testLegalTransitionsAreAllowed(OrderStatus from, OrderStatus to) {
        assertTrue(orderStatusService.canTransit(from, to),
                from + " -> " + to + " should be a legal transition");
        assertDoesNotThrow(() -> orderStatusService.assertCanTransit(from, to));
        assertEquals(to, orderStatusService.transit(from, to));
    }

    @ParameterizedTest(name = "[{index}] {0} -> {1} 非法流转")
    @CsvSource({
            "CONFIRMED,DELIVERED",
            "CONFIRMED,COMPLETED",
            "IN_PRODUCTION,DELIVERED",
            "IN_PRODUCTION,COMPLETED",
            "SHIPPED,COMPLETED",
            "SHIPPED,CANCELLED",
            "DELIVERED,SHIPPED",
            "COMPLETED,CANCELLED",
            "CANCELLED,CONFIRMED"
    })
    @DisplayName("非法流转应被拒绝并抛出业务异常")
    void testIllegalTransitionsAreRejected(OrderStatus from, OrderStatus to) {
        assertFalse(orderStatusService.canTransit(from, to),
                from + " -> " + to + " should be an illegal transition");
        assertThrows(IllegalArgumentException.class, () -> orderStatusService.assertCanTransit(from, to));
        assertThrows(IllegalArgumentException.class, () -> orderStatusService.transit(from, to));
    }

    @ParameterizedTest(name = "[{index}] {0} -> {0} 同状态非法")
    @EnumSource(OrderStatus.class)
    @DisplayName("流转到自身应被拒绝")
    void testSelfTransitionIsRejected(OrderStatus status) {
        assertFalse(orderStatusService.canTransit(status, status),
                status + " -> " + status + " should be an illegal transition");
    }

    @ParameterizedTest(name = "[{index}] {0} 是终态")
    @EnumSource(value = OrderStatus.class, names = {"COMPLETED", "CANCELLED"})
    @DisplayName("终态不能再流转到任何状态")
    void testTerminalStatesCannotTransit(OrderStatus terminal) {
        for (OrderStatus target : OrderStatus.values()) {
            assertFalse(orderStatusService.canTransit(terminal, target),
                    terminal + " is terminal and must not transit to " + target);
        }
    }

    @Test
    @DisplayName("null 状态应被拒绝")
    void testNullStateIsRejected() {
        // canTransit 是全函数：null 一律返回 false，调用方不会因为漏判 null 而拿到 NPE
        assertFalse(orderStatusService.canTransit(null, OrderStatus.SHIPPED));
        assertFalse(orderStatusService.canTransit(OrderStatus.CONFIRMED, null));
        // assertCanTransit 才是对外的强校验入口，null 直接抛业务异常
        assertThrows(IllegalArgumentException.class,
                () -> orderStatusService.assertCanTransit(null, OrderStatus.SHIPPED));
        assertThrows(IllegalArgumentException.class,
                () -> orderStatusService.assertCanTransit(OrderStatus.CONFIRMED, null));
    }
}
