/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 订单实体，对应表 t_order.
 *
 * @since 2026-11-08
 */
@Data
public class CrmOrderEntity {

    /**
     * 主键
     */
    private String id;
    /**
     * 订单号
     */
    private String orderNo;
    /**
     * 客户 → t_customer.id
     */
    private String customerId;
    /**
     * 来源报价单 → t_quotation.id
     */
    private String quotationId;
    /**
     * 来源商机 → t_opportunity.id
     */
    private String opportunityId;
    /**
     * 币种（一单一币种）
     */
    private String currencyCode;
    /**
     * 订单金额（原币）
     */
    private BigDecimal totalAmount;
    /**
     * 本位币（USD）快照
     */
    private BigDecimal totalAmountBase;
    /**
     * 贸易术语
     */
    private String incoterm;
    /**
     * 付款方式
     */
    private String paymentTerm;
    /**
     * 定金比例 %
     */
    private Integer depositRate;
    /**
     * 状态 CONFIRMED/IN_PRODUCTION/SHIPPED/DELIVERED/COMPLETED/CANCELLED
     */
    private String orderStatus;
    /**
     * 发货日期
     */
    private LocalDate shipDate;
    /**
     * 预计到港日
     */
    private LocalDate expectArriveDate;
    /**
     * 负责人 → t_user.id
     */
    private String ownerId;
    /**
     * 备注
     */
    private String remark;
    /**
     * 逻辑删除：0 未删除；删除时写入毫秒时间戳
     */
    private Long deleted;
}
