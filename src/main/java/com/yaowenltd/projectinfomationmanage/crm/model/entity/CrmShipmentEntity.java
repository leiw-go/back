/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 发货单实体，对应表 t_shipment.
 *
 * @since 2026-11-08
 */
@Data
public class CrmShipmentEntity {

    /**
     * 主键
     */
    private String id;
    /**
     * 订单 → t_order.id
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
     * 状态 PREPARING/SHIPPED/ARRIVED
     */
    private String shipmentStatus;
    /**
     * 备注
     */
    private String remark;
    /**
     * 逻辑删除：0 未删除；删除时写入毫秒时间戳
     */
    private Long deleted;
}
