/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmOrderMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmShipmentMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmShipmentDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmShipmentQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmShipmentRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmOrderEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmShipmentEntity;
import com.yaowenltd.projectinfomationmanage.crm.common.MessageNotifier;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.MessageBizType;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.MessageType;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.OrderStatus;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmReceivableService;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmShipmentService;
import com.yaowenltd.projectinfomationmanage.crm.service.OrderStatusService;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.HashMap;
import java.util.Map;

/**
 * CRM 发货单服务实现.
 * <p>
 * 按需求文档 §3.7 / §3.8：一条订单可**多次发货**（分批）；
 * **首次**登记发货时订单置 {@code SHIPPED}，之后订单保持 {@code SHIPPED}。
 *
 * @since 2026-11-08
 */
@Service
public class CrmShipmentServiceImpl implements CrmShipmentService {

    /** 列表默认每页条数. */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 列表最大每页条数. */
    private static final int MAX_PAGE_SIZE = 200;

    /** 单次查询允许的最大偏移量. */
    private static final int MAX_PAGE_OFFSET = 10000;

    /** 发货单默认状态. */
    private static final String DEFAULT_SHIPMENT_STATUS = "PREPARING";

    private final CrmShipmentMapper shipmentMapper;
    private final CrmOrderMapper orderMapper;
    private final OrderStatusService orderStatusService;
    private final CrmReceivableService receivableService;
    private final MessageNotifier messageNotifier;

    /**
     * 使用指定的 Mapper 构造 CrmShipmentServiceImpl.
     *
     * @param shipmentMapper     发货单 Mapper
     * @param orderMapper        订单 Mapper
     * @param orderStatusService 订单状态机
     * @param receivableService  应收服务（交付后回判订单是否可完成）
     */
    public CrmShipmentServiceImpl(CrmShipmentMapper shipmentMapper,
                                   CrmOrderMapper orderMapper,
                                   OrderStatusService orderStatusService,
                                   CrmReceivableService receivableService,
                                   MessageNotifier messageNotifier) {
        this.shipmentMapper = shipmentMapper;
        this.orderMapper = orderMapper;
        this.orderStatusService = orderStatusService;
        this.receivableService = receivableService;
        this.messageNotifier = messageNotifier;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrmShipmentDto create(CrmShipmentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("shipment request must not be null");
        }
        if (request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
            throw new IllegalArgumentException("orderId must not be blank");
        }
        if (request.getShippingType() == null || request.getShippingType().trim().isEmpty()) {
            throw new IllegalArgumentException("shippingType must not be blank");
        }
        CrmOrderEntity order = requireOrder(request.getOrderId());

        CrmShipmentEntity entity = new CrmShipmentEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setOrderId(order.getId());
        applyRequest(entity, request);
        entity.setShipmentStatus(request.getShipmentStatus() == null || request.getShipmentStatus().trim().isEmpty()
                ? (request.getShipDate() == null ? DEFAULT_SHIPMENT_STATUS : "SHIPPED")
                : request.getShipmentStatus().trim().toUpperCase());
        entity.setDeleted(0L);
        shipmentMapper.insert(entity);

        // 首次发货：订单置 SHIPPED（分批发货时订单保持 SHIPPED）
        OrderStatus current = OrderStatus.valueOf(order.getOrderStatus());
        if (current != OrderStatus.SHIPPED) {
            orderStatusService.assertCanTransit(current, OrderStatus.SHIPPED);
            order.setOrderStatus(OrderStatus.SHIPPED.name());
        }
        if (order.getShipDate() == null && request.getShipDate() != null) {
            order.setShipDate(request.getShipDate());
        }
        if (order.getExpectArriveDate() == null && request.getExpectArriveDate() != null) {
            order.setExpectArriveDate(request.getExpectArriveDate());
        }
        orderMapper.update(order);
        markDeliveredIfArrived(order, entity);
        notifyOrderShipped(order, entity);
        return toDto(entity);
    }

    @Override
    public CrmShipmentDto update(String id, CrmShipmentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("shipment request must not be null");
        }
        CrmShipmentEntity entity = requireShipment(id);
        applyRequest(entity, request);
        if (request.getShipmentStatus() != null && !request.getShipmentStatus().trim().isEmpty()) {
            entity.setShipmentStatus(request.getShipmentStatus().trim().toUpperCase());
        }
        shipmentMapper.update(entity);
        markDeliveredIfArrived(requireOrder(entity.getOrderId()), entity);
        return toDto(entity);
    }

    @Override
    public CrmShipmentDto getById(String id) {
        return toDto(requireShipment(id));
    }

    @Override
    public PageResponse<CrmShipmentDto> list(CrmShipmentQueryRequest query) {
        CrmShipmentQueryRequest actual = query == null ? new CrmShipmentQueryRequest() : query;
        int page = actual.getPage() == null || actual.getPage() < 1 ? 1 : actual.getPage();
        int size = actual.getSize() == null || actual.getSize() < 1
                ? DEFAULT_PAGE_SIZE : Math.min(actual.getSize(), MAX_PAGE_SIZE);
        if ((long) page * size > MAX_PAGE_OFFSET) {
            throw new IllegalArgumentException("page * size must not exceed " + MAX_PAGE_OFFSET);
        }
        PageHelper.startPage(page, size);
        try {
            PageInfo<CrmShipmentEntity> info = new PageInfo<>(shipmentMapper.selectPage(actual));

            List<CrmShipmentDto> rows = new ArrayList<>(info.getList().size());
            for (CrmShipmentEntity entity : info.getList()) {
                rows.add(toDto(entity));
            }
            PageResponse<CrmShipmentDto> response = new PageResponse<>();
            response.setData(rows);
            response.setTotal(info.getTotal());
            response.setPage(info.getPageNum());
            response.setSize(info.getPageSize());
            return response;
        } finally {
            PageHelper.clearPage();
        }
    }

    /**
     * 登记了实际到港日 → 订单置 DELIVERED，并回判是否可完成.
     */
    private void markDeliveredIfArrived(CrmOrderEntity order, CrmShipmentEntity shipment) {
        if (shipment.getActualArriveDate() == null) {
            return;
        }
        OrderStatus current = OrderStatus.valueOf(order.getOrderStatus());
        if (current == OrderStatus.SHIPPED) {
            orderStatusService.assertCanTransit(current, OrderStatus.DELIVERED);
            order.setOrderStatus(OrderStatus.DELIVERED.name());
            orderMapper.update(order);
        }
        receivableService.tryCompleteOrder(order.getId());
    }

    private void applyRequest(CrmShipmentEntity entity, CrmShipmentRequest request) {
        entity.setShippingNo(request.getShippingNo());
        entity.setShippingType(request.getShippingType() == null ? null : request.getShippingType().trim());
        entity.setDeparturePort(request.getDeparturePort());
        entity.setDestinationPort(request.getDestinationPort());
        entity.setShipDate(request.getShipDate());
        entity.setExpectArriveDate(request.getExpectArriveDate());
        entity.setActualArriveDate(request.getActualArriveDate());
        entity.setTotalCbm(request.getTotalCbm());
        entity.setTotalWeight(request.getTotalWeight());
        entity.setRemark(request.getRemark());
    }

    private CrmOrderEntity requireOrder(String orderId) {
        CrmOrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("order not found: " + orderId);
        }
        return order;
    }

    private CrmShipmentEntity requireShipment(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("shipment id must not be blank");
        }
        CrmShipmentEntity entity = shipmentMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("shipment not found: " + id);
        }
        return entity;
    }

    private CrmShipmentDto toDto(CrmShipmentEntity entity) {
        CrmShipmentDto dto = new CrmShipmentDto();
        dto.setId(entity.getId());
        dto.setOrderId(entity.getOrderId());
        dto.setShippingNo(entity.getShippingNo());
        dto.setShippingType(entity.getShippingType());
        dto.setDeparturePort(entity.getDeparturePort());
        dto.setDestinationPort(entity.getDestinationPort());
        dto.setShipDate(entity.getShipDate());
        dto.setExpectArriveDate(entity.getExpectArriveDate());
        dto.setActualArriveDate(entity.getActualArriveDate());
        dto.setTotalCbm(entity.getTotalCbm());
        dto.setTotalWeight(entity.getTotalWeight());
        dto.setShipmentStatus(entity.getShipmentStatus());
        dto.setRemark(entity.getRemark());
        return dto;
    }

    /**
     * 发货后发站内信（需求文档 §3.11 的"订单已发货"）.
     * <p>
     * 站内信是旁路，发送失败只记日志，不影响发货事务.
     * </p>
     *
     * @param order    订单
     * @param shipment 发货单
     */
    private void notifyOrderShipped(CrmOrderEntity order, CrmShipmentEntity shipment) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("shippingNo", shipment.getShippingNo() == null ? "-" : shipment.getShippingNo());
        placeholders.put("expectArriveDate",
                shipment.getExpectArriveDate() == null ? "-" : shipment.getExpectArriveDate().toString());
        messageNotifier.notifyOwner(MessageType.ORDER_SHIPPED, MessageBizType.ORDER.name(), order.getId(),
                order.getOrderNo(), order.getOwnerId(), placeholders);
    }
}
