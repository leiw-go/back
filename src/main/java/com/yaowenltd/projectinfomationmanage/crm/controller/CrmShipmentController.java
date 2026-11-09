/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmShipmentDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmShipmentQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmShipmentRequest;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmShipmentService;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRM 发货单 HTTP 接口.
 *
 * @since 2026-11-08
 */
@RestController
@RequestMapping("/api/crm/shipments")
@Tag(name = "CRM Shipment", description = "CRM shipment APIs")
public class CrmShipmentController {

    private final CrmShipmentService crmShipmentService;

    /**
     * 使用指定的 Service 构造 CrmShipmentController.
     *
     * @param crmShipmentService 发货单服务
     */
    public CrmShipmentController(CrmShipmentService crmShipmentService) {
        this.crmShipmentService = crmShipmentService;
    }

    /**
     * 分页查询发货单列表.
     *
     * @param query 筛选条件
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "List shipments", description = "分页查询发货单列表")
    public ResponseResult<PageResponse<CrmShipmentDto>> listShipments(CrmShipmentQueryRequest query) {
        return ResponseResult.success(crmShipmentService.list(query));
    }

    /**
     * 新建发货单（首次发货时订单置 SHIPPED）.
     *
     * @param request 发货信息
     * @return 新建后的发货单
     */
    @PostMapping
    @Operation(summary = "Create shipment", description = "新建发货单，首次发货时订单置 SHIPPED")
    public ResponseResult<CrmShipmentDto> createShipment(@RequestBody CrmShipmentRequest request) {
        return ResponseResult.created(crmShipmentService.create(request));
    }

    /**
     * 更新发货单.
     *
     * @param id      发货单 ID
     * @param request 发货信息
     * @return 更新后的发货单
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update shipment", description = "更新发货单")
    public ResponseResult<CrmShipmentDto> updateShipment(@PathVariable String id,
                                                         @RequestBody CrmShipmentRequest request) {
        return ResponseResult.success(crmShipmentService.update(id, request));
    }

}
