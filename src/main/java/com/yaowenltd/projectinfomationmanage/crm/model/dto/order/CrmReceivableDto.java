/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 应收出参.
 *
 * @since 2026-11-08
 */
@Data
public class CrmReceivableDto {

    /**
     * 应收 ID
     */
    private String id;

    /**
     * 订单 ID
     */
    private String orderId;

    /**
     * 币种
     */
    private String currencyCode;

    /**
     * 类型 DEPOSIT/BALANCE/FULL
     */
    private String receivableType;

    /**
     * 应收总额
     */
    private BigDecimal receivableAmount;

    /**
     * 已收金额
     */
    private BigDecimal receivedAmount;

    /**
     * 未收金额
     */
    private BigDecimal outstandingAmount;

    /**
     * 约定收款日
     */
    private LocalDate dueDate;

    /**
     * 状态 UNPAID/PARTIAL/PAID/OVERDUE
     */
    private String receivableStatus;

    /**
     * 逾期天数
     */
    private Integer overdueDays;

    /**
     * 负责人 ID
     */
    private String ownerId;

}
