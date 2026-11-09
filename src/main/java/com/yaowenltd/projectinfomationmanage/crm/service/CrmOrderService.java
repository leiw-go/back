/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmOrderDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmOrderQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmOrderReceiveRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmOrderRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmShipmentRequest;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;

/**
 * CRM 订单服务.
 *
 * @since 2026-11-08
 */
public interface CrmOrderService {

    /**
     * 新建订单（可带 quotationId 由报价快照生成）.
     *
     * @param request 订单信息
     * @return 新建后的订单
     */
    CrmOrderDto create(CrmOrderRequest request);

    /**
     * 更新订单（仅未发货可改）.
     *
     * @param id      订单 ID
     * @param request 订单信息
     * @return 更新后的订单
     */
    CrmOrderDto update(String id, CrmOrderRequest request);

    /**
     * 查询订单详情（明细、发货、收款）.
     *
     * @param id 订单 ID
     * @return 订单
     */
    CrmOrderDto getById(String id);

    /**
     * 逻辑删除订单（已发货不可删）.
     *
     * @param id 订单 ID
     */
    void delete(String id);

    /**
     * 分页查询订单.
     *
     * @param query 筛选条件
     * @return 分页结果
     */
    PageResponse<CrmOrderDto> list(CrmOrderQueryRequest query);

    /**
     * 登记发货（首次发货时订单置 SHIPPED）.
     *
     * @param id      订单 ID
     * @param request 发货信息
     * @return 更新后的订单
     */
    CrmOrderDto ship(String id, CrmShipmentRequest request);

    /**
     * 登记收款（核销指定应收）.
     *
     * @param id      订单 ID
     * @param request 收款信息
     * @return 更新后的订单
     */
    CrmOrderDto receive(String id, CrmOrderReceiveRequest request);

}
