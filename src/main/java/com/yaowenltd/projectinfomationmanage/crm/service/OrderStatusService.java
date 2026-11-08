/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.enums.OrderStatus;

/**
 * 订单状态机.<p>按需求文档 §3.7：CONFIRMED → IN_PRODUCTION → SHIPPED → DELIVERED → COMPLETED，任意未发货态可取消。
 *
 * @since 2026-11-08
 */
public interface OrderStatusService {

    /**
     * 判断两个订单状态之间是否允许流转.
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return 允许返回 true
     */
    boolean canTransit(OrderStatus from, OrderStatus to);

    /**
     * 校验订单状态流转是否合法，非法时抛出业务异常.
     *
     * @param from 当前状态
     * @param to   目标状态
     */
    void assertCanTransit(OrderStatus from, OrderStatus to);

    /**
     * 执行订单状态流转并返回目标状态.
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return 目标状态
     */
    OrderStatus transit(OrderStatus from, OrderStatus to);
}
