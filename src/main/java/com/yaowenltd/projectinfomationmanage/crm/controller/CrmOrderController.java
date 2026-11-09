/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmOrderDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmOrderQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmOrderReceiveRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmOrderRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmShipmentRequest;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmOrderService;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRM 订单 HTTP 接口.
 *
 * @since 2026-11-08
 */
@RestController
@RequestMapping("/api/crm/orders")
@Tag(name = "CRM Order", description = "CRM order APIs")
public class CrmOrderController {

    private final CrmOrderService crmOrderService;

    /**
     * 使用指定的 Service 构造 CrmOrderController.
     *
     * @param crmOrderService 订单服务
     */
    public CrmOrderController(CrmOrderService crmOrderService) {
        this.crmOrderService = crmOrderService;
    }

    /**
     * 分页查询订单列表.
     *
     * @param query 筛选条件
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "List orders", description = "分页查询订单列表")
    public ResponseResult<PageResponse<CrmOrderDto>> listOrders(CrmOrderQueryRequest query) {
        return ResponseResult.success(crmOrderService.list(query));
    }

    /**
     * 查询订单详情（明细、发货、收款）.
     *
     * @param id 订单 ID
     * @return 订单
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get order", description = "查询订单详情，含明细、发货与收款")
    public ResponseResult<CrmOrderDto> getOrder(@PathVariable String id) {
        return ResponseResult.success(crmOrderService.getById(id));
    }

    /**
     * 新建订单（可带 quotationId 由报价快照生成）.
     *
     * @param request 订单信息
     * @return 新建后的订单
     */
    @PostMapping
    @Operation(summary = "Create order", description = "新建订单，可带 quotationId 由报价生成")
    public ResponseResult<CrmOrderDto> createOrder(@RequestBody CrmOrderRequest request) {
        return ResponseResult.created(crmOrderService.create(request));
    }

    /**
     * 更新订单（仅未发货可改）.
     *
     * @param id      订单 ID
     * @param request 订单信息
     * @return 更新后的订单
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update order", description = "更新订单，仅未发货可改")
    public ResponseResult<CrmOrderDto> updateOrder(@PathVariable String id,
                                                    @RequestBody CrmOrderRequest request) {
        return ResponseResult.success(crmOrderService.update(id, request));
    }

    /**
     * 逻辑删除订单（已发货不可删）.
     *
     * @param id 订单 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete order", description = "逻辑删除订单，已发货不可删")
    public ResponseResult<Void> deleteOrder(@PathVariable String id) {
        crmOrderService.delete(id);
        return ResponseResult.success();
    }

    /**
     * 登记发货（首次发货时订单置 SHIPPED）.
     *
     * @param id      订单 ID
     * @param request 发货信息
     * @return 更新后的订单
     */
    @PostMapping("/{id}/ship")
    @Operation(summary = "Ship order", description = "登记发货，首次发货时订单置 SHIPPED")
    public ResponseResult<CrmOrderDto> shipOrder(@PathVariable String id,
                                                  @RequestBody com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmShipmentRequest request) {
        return ResponseResult.success(crmOrderService.ship(id, request));
    }

    /**
     * 登记收款（核销指定应收）.
     *
     * @param id      订单 ID
     * @param request 收款信息
     * @return 更新后的订单
     */
    @PostMapping("/{id}/receive")
    @Operation(summary = "Receive order payment", description = "登记收款，核销指定应收")
    public ResponseResult<CrmOrderDto> receiveOrder(@PathVariable String id,
                                                    @RequestBody CrmOrderReceiveRequest request) {
        return ResponseResult.success(crmOrderService.receive(id, request));
    }

}
