/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.order;

import java.time.LocalDate;
import java.util.List;
import lombok.Data;

/**
 * CRM 订单入参（新建 / 更新）.
 *
 * @since 2026-11-08
 */
@Data
public class CrmOrderRequest {

    /**
     * 客户 ID；由报价生成订单时可不传，由报价带出
     */
    private String customerId;

    /**
     * 来源报价单 ID；传入则按报价快照生成订单
     */
    private String quotationId;

    /**
     * 来源商机 ID
     */
    private String opportunityId;

    /**
     * 币种；为空时默认 USD
     */
    private String currencyCode;

    /**
     * 贸易术语
     */
    private String incoterm;

    /**
     * 付款方式 TT/LC/DP/DA/OA/PAYPAL
     */
    private String paymentTerm;

    /**
     * 定金比例 %，0~100；决定应收拆分成定金 + 尾款
     */
    private Integer depositRate;

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
    private List<CrmOrderItemRequest> items;

}
