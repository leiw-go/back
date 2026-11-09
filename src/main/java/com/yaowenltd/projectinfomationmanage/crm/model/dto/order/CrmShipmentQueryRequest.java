/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.order;

import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 发货单列表筛选条件.
 *
 * @since 2026-11-08
 */
@Data
public class CrmShipmentQueryRequest {

    /**
     * 页码，从 1 开始
     */
    private Integer page;

    /**
     * 每页条数
     */
    private Integer size;

    /**
     * 订单 ID
     */
    private String orderId;

    /**
     * 状态
     */
    private String shippingStatus;

    /**
     * 发货日期起
     */
    private LocalDate shipDateFrom;

    /**
     * 发货日期止
     */
    private LocalDate shipDateTo;

}
