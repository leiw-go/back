/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.order;

import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 订单列表筛选条件.
 *
 * @since 2026-11-08
 */
@Data
public class CrmOrderQueryRequest {

    /**
     * 页码，从 1 开始
     */
    private Integer page;

    /**
     * 每页条数
     */
    private Integer size;

    /**
     * 状态
     */
    private String orderStatus;

    /**
     * 客户 ID
     */
    private String customerId;

    /**
     * 负责人 ID
     */
    private String ownerId;

    /**
     * 发货日期起
     */
    private LocalDate shipDateFrom;

    /**
     * 发货日期止
     */
    private LocalDate shipDateTo;

}
