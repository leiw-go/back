/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.yaowenltd.projectinfomationmanage.crm.model.enums.OrderStatus;
import com.yaowenltd.projectinfomationmanage.crm.service.OrderStatusService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态机实现.
 * <p>按需求文档 §3.7：CONFIRMED → IN_PRODUCTION → SHIPPED → DELIVERED → COMPLETED；
 * CONFIRMED / IN_PRODUCTION 可取消；已发货、已完成、已取消均为终态。
 * </p>
 *
 * @since 2026-11-08
 */
@Service
public class OrderStatusServiceImpl implements OrderStatusService {

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = buildTransitions();

    private static Map<OrderStatus, Set<OrderStatus>> buildTransitions() {
        Map<OrderStatus, Set<OrderStatus>> map = new EnumMap<>(OrderStatus.class);
        map.put(OrderStatus.CONFIRMED,
                EnumSet.of(OrderStatus.IN_PRODUCTION, OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        map.put(OrderStatus.IN_PRODUCTION, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED));
        map.put(OrderStatus.SHIPPED, EnumSet.of(OrderStatus.DELIVERED));
        map.put(OrderStatus.DELIVERED, EnumSet.of(OrderStatus.COMPLETED));
        map.put(OrderStatus.COMPLETED, Collections.emptySet());
        map.put(OrderStatus.CANCELLED, Collections.emptySet());
        return Collections.unmodifiableMap(map);
    }

    @Override
    public boolean canTransit(OrderStatus from, OrderStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(from, Collections.emptySet()).contains(to);
    }

    @Override
    public void assertCanTransit(OrderStatus from, OrderStatus to) {
        if (!canTransit(from, to)) {
            throw new IllegalArgumentException(
                    "illegal order status transition: " + from + " -> " + to);
        }
    }

    @Override
    public OrderStatus transit(OrderStatus from, OrderStatus to) {
        assertCanTransit(from, to);
        return to;
    }
}
