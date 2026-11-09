/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.enums;

/**
 * 站内信关联业务类型（对应 {@code t_message.biz_type}）.
 * <p>
 * 与 {@code t_message.biz_type} 列注释里的取值保持一致：
 * CUSTOMER / OPPORTUNITY / INQUIRY / QUOTATION / ORDER / RECEIVABLE / CURRENCY。
 * </p>
 *
 * @since 2026-11-08
 */
public enum MessageBizType {

    /**
     * 客户.
     */
    CUSTOMER,

    /**
     * 商机.
     */
    OPPORTUNITY,

    /**
     * 询盘.
     */
    INQUIRY,

    /**
     * 报价单.
     */
    QUOTATION,

    /**
     * 订单.
     */
    ORDER,

    /**
     * 应收.
     */
    RECEIVABLE,

    /**
     * 币种 / 汇率.
     */
    CURRENCY

}
