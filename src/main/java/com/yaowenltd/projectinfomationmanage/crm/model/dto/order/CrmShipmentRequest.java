/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 发货单入参.
 *
 * @since 2026-11-08
 */
@Data
public class CrmShipmentRequest {

    /**
     * 订单 ID
     */
    private String orderId;

    /**
     * 运单 / 提单号
     */
    private String shippingNo;

    /**
     * 发货方式 SEA/AIR/EXPRESS/TRUCK
     */
    private String shippingType;

    /**
     * 起运港
     */
    private String departurePort;

    /**
     * 目的港
     */
    private String destinationPort;

    /**
     * 发货日期
     */
    private LocalDate shipDate;

    /**
     * 预计到港日
     */
    private LocalDate expectArriveDate;

    /**
     * 实际到港日
     */
    private LocalDate actualArriveDate;

    /**
     * 总体积 CBM
     */
    private BigDecimal totalCbm;

    /**
     * 总重量 kg
     */
    private BigDecimal totalWeight;

    /**
     * 状态；为空时默认 PREPARING
     */
    private String shipmentStatus;

    /**
     * 备注
     */
    private String remark;

}
