/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.enums;

/**
 * 订单状态。<p>枚举值以稳定的英文码存储，可读文案走字典。
 *
 * @since 2026-11-08
 */
public enum OrderStatus {

    /**
     * CONFIRMED 已确认.
     */
    CONFIRMED,

    /**
     * IN_PRODUCTION 生产中.
     */
    IN_PRODUCTION,

    /**
     * SHIPPED 已发货.
     */
    SHIPPED,

    /**
     * DELIVERED 已交付.
     */
    DELIVERED,

    /**
     * COMPLETED 已完成.
     */
    COMPLETED,

    /**
     * CANCELLED 已取消.
     */
    CANCELLED

}
