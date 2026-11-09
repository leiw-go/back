/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 应收实体，对应表 t_receivable.
 *
 * @since 2026-11-08
 */
@Data
public class CrmReceivableEntity {

    /**
     * 主键
     */
    private String id;
    /**
     * 订单 → t_order.id
     */
    private String orderId;
    /**
     * 币种（继承订单）
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
     * 已收金额（累计值，无流水表）
     */
    private BigDecimal receivedAmount;
    /**
     * 约定收款日
     */
    private LocalDate dueDate;
    /**
     * 状态 UNPAID/PARTIAL/PAID/OVERDUE
     */
    private String receivableStatus;
    /**
     * 逾期天数（查询时按 due_date 实时算）
     */
    private Integer overdueDays;
    /**
     * 负责人 → t_user.id
     */
    private String ownerId;
    /**
     * 逻辑删除：0 未删除；删除时写入毫秒时间戳
     */
    private Long deleted;
}
