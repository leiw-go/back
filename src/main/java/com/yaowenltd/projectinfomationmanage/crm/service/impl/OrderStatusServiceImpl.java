/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.yaowenltd.projectinfomationmanage.crm.model.enums.OrderStatus;
import com.yaowenltd.projectinfomationmanage.crm.service.OrderStatusService;
import org.springframework.stereotype.Service;

/**
 * 订单状态机实现.
 *
 * @since 2026-11-08
 */
@Service
public class OrderStatusServiceImpl implements OrderStatusService {


    /**
     * 判断两个订单状态之间是否允许流转.
     */
    @Override
    public boolean canTransit(OrderStatus from, OrderStatus to) {
        throw new UnsupportedOperationException("TDD: 待实现");
    }

    /**
     * 校验订单状态流转是否合法，非法时抛出业务异常.
     */
    @Override
    public void assertCanTransit(OrderStatus from, OrderStatus to) {
        throw new UnsupportedOperationException("TDD: 待实现");
    }

    /**
     * 执行订单状态流转并返回目标状态.
     */
    @Override
    public OrderStatus transit(OrderStatus from, OrderStatus to) {
        throw new UnsupportedOperationException("TDD: 待实现");
    }
}
