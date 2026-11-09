/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 订单收款（核销指定应收）入参.
 *
 * @since 2026-11-08
 */
@Data
public class CrmOrderReceiveRequest {

    /**
     * 要核销的应收 ID
     */
    private String receivableId;

    /**
     * 本次收款金额
     */
    private BigDecimal amount;

    /**
     * 收款日期；为空时取当天
     */
    private LocalDate paidDate;

    /**
     * 收款方式
     */
    private String paymentMethod;

    /**
     * 备注
     */
    private String remark;

}
