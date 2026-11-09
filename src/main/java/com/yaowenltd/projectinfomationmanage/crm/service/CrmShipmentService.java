/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmShipmentDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmShipmentQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmShipmentRequest;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;

/**
 * CRM 发货单服务.
 *
 * @since 2026-11-08
 */
public interface CrmShipmentService {

    /**
     * 新建发货单（首次发货时订单置 SHIPPED）.
     *
     * @param request 发货信息
     * @return 新建后的发货单
     */
    CrmShipmentDto create(CrmShipmentRequest request);

    /**
     * 更新发货单.
     *
     * @param id      发货单 ID
     * @param request 发货信息
     * @return 更新后的发货单
     */
    CrmShipmentDto update(String id, CrmShipmentRequest request);

    /**
     * 查询发货单详情.
     *
     * @param id 发货单 ID
     * @return 发货单
     */
    CrmShipmentDto getById(String id);

    /**
     * 分页查询发货单.
     *
     * @param query 筛选条件
     * @return 分页结果
     */
    PageResponse<CrmShipmentDto> list(CrmShipmentQueryRequest query);

}
