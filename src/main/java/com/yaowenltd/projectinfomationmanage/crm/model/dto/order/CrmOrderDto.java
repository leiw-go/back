/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

/**
 * CRM 订单出参.
 *
 * @since 2026-11-08
 */
@Data
public class CrmOrderDto {

    /**
     * 订单 ID
     */
    private String id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 客户 ID
     */
    private String customerId;

    /**
     * 来源报价单 ID
     */
    private String quotationId;

    /**
     * 来源商机 ID
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
     * 状态
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
     * 负责人 ID
     */
    private String ownerId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 订单明细
     */
    private List<CrmOrderItemDto> items;

    /**
     * 发货单
     */
    private List<CrmShipmentDto> shipments;

    /**
     * 应收
     */
    private List<CrmReceivableDto> receivables;

}
