/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.order;

import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 应收列表筛选条件.
 *
 * @since 2026-11-08
 */
@Data
public class CrmReceivableQueryRequest {

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
    private String receivableStatus;

    /**
     * 负责人 ID
     */
    private String ownerId;

    /**
     * 约定收款日起
     */
    private LocalDate dueDateFrom;

    /**
     * 约定收款日止
     */
    private LocalDate dueDateTo;

    /**
     * 只看逾期未收清
     */
    private Boolean overdueOnly;

}
