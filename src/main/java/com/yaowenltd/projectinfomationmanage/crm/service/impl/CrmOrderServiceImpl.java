/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmOpportunityMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmOrderItemMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmOrderMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmQuotationItemMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmQuotationMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmReceivableMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmShipmentMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerMapper;
import com.yaowenltd.projectinfomationmanage.crm.common.MessageNotifier;
import com.yaowenltd.projectinfomationmanage.crm.common.MessageTextFactory;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmOrderDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmOrderItemDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmOrderItemRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmOrderQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmOrderReceiveRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmOrderRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableWriteOffRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmShipmentDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmShipmentRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmOpportunityEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmOrderEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmOrderItemEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmQuotationEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmQuotationItemEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmReceivableEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmShipmentEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.OpportunityStage;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.MessageBizType;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.MessageType;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.OrderStatus;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.QuotationStatus;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.ReceivableStatus;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmOrderService;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmReceivableService;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmShipmentService;
import com.yaowenltd.projectinfomationmanage.crm.service.CurrencyRateQueryService;
import com.yaowenltd.projectinfomationmanage.crm.service.OpportunityStageService;
import com.yaowenltd.projectinfomationmanage.crm.service.OrderStatusService;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * CRM 订单服务实现.
 * <p>
 * 按需求文档 §3.7 / §3.8：由报价转订单时**快照**报价行的单价金额，
 * 报价后续改价不影响已生成的订单；订单创建时按 {@code deposit_rate} 拆出应收。
 *
 * @since 2026-11-08
 */
@Service
public class CrmOrderServiceImpl implements CrmOrderService {

    /** 列表默认每页条数. */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 列表最大每页条数. */
    private static final int MAX_PAGE_SIZE = 200;

    /** 单次查询允许的最大偏移量. */
    private static final int MAX_PAGE_OFFSET = 10000;

    /** 本位币（金额快照的目标币种）. */
    private static final String BASE_CURRENCY = "USD";

    /** 默认币种. */
    private static final String DEFAULT_CURRENCY = "USD";

    /** 金额保留的小数位. */
    private static final int AMOUNT_SCALE = 2;

    /** 单号里的日期段. */
    private static final DateTimeFormatter NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CrmOrderMapper orderMapper;
    private final CrmOrderItemMapper orderItemMapper;
    private final CrmShipmentMapper shipmentMapper;
    private final CrmReceivableMapper receivableMapper;
    private final CrmQuotationMapper quotationMapper;
    private final CrmQuotationItemMapper quotationItemMapper;
    private final CrmOpportunityMapper opportunityMapper;
    private final CustomerMapper customerMapper;
    private final OrderStatusService orderStatusService;
    private final OpportunityStageService opportunityStageService;
    private final CrmShipmentService shipmentService;
    private final CrmReceivableService receivableService;
    private final CurrencyRateQueryService currencyRateQueryService;
    private final MessageNotifier messageNotifier;

    /**
     * 使用指定依赖构造 CrmOrderServiceImpl.
     *
     * @param orderMapper            订单 Mapper
     * @param orderItemMapper        订单明细 Mapper
     * @param shipmentMapper         发货单 Mapper
     * @param receivableMapper       应收 Mapper
     * @param quotationMapper        报价单 Mapper
     * @param quotationItemMapper    报价明细 Mapper
     * @param opportunityMapper      商机 Mapper
     * @param customerMapper         客户 Mapper（逻辑外键校验）
     * @param orderStatusService     订单状态机
     * @param opportunityStageService 商机阶段机
     * @param shipmentService        发货单服务
     * @param receivableService      应收服务
     * @param currencyRateQueryService 汇率折算服务
     */
    public CrmOrderServiceImpl(CrmOrderMapper orderMapper,
                               CrmOrderItemMapper orderItemMapper,
                               CrmShipmentMapper shipmentMapper,
                               CrmReceivableMapper receivableMapper,
                               CrmQuotationMapper quotationMapper,
                               CrmQuotationItemMapper quotationItemMapper,
                               CrmOpportunityMapper opportunityMapper,
                               CustomerMapper customerMapper,
                               OrderStatusService orderStatusService,
                               OpportunityStageService opportunityStageService,
                               CrmShipmentService shipmentService,
                               CrmReceivableService receivableService,
                               CurrencyRateQueryService currencyRateQueryService,
                               MessageNotifier messageNotifier) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.shipmentMapper = shipmentMapper;
        this.receivableMapper = receivableMapper;
        this.quotationMapper = quotationMapper;
        this.quotationItemMapper = quotationItemMapper;
        this.opportunityMapper = opportunityMapper;
        this.customerMapper = customerMapper;
        this.orderStatusService = orderStatusService;
        this.opportunityStageService = opportunityStageService;
        this.shipmentService = shipmentService;
        this.receivableService = receivableService;
        this.currencyRateQueryService = currencyRateQueryService;
        this.messageNotifier = messageNotifier;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrmOrderDto create(CrmOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("order request must not be null");
        }
        CrmOrderEntity order = new CrmOrderEntity();
        order.setId(UUID.randomUUID().toString());
        order.setOrderNo(generateNo());

        List<CrmOrderItemRequest> items = request.getItems();
        BigDecimal totalAmount;
        if (request.getQuotationId() != null && !request.getQuotationId().trim().isEmpty()) {
            CrmQuotationEntity quotation = requireQuotation(request.getQuotationId());
            applyQuotation(order, quotation);
            items = toOrderItems(quotation);
            totalAmount = quotation.getTotalAmount();
        } else {
            if (request.getCustomerId() == null || request.getCustomerId().trim().isEmpty()) {
                throw new IllegalArgumentException("customerId must not be blank");
            }
            if (items == null || items.isEmpty()) {
                throw new IllegalArgumentException("order must contain at least one item");
            }
            requireCustomer(request.getCustomerId());
            order.setCustomerId(request.getCustomerId().trim());
            order.setCurrencyCode(resolveCurrency(request.getCurrencyCode(), null));
            totalAmount = sumAmount(items);
        }

        order.setTotalAmount(totalAmount.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP));
        order.setTotalAmountBase(snapshotBaseAmount(order.getTotalAmount(), order.getCurrencyCode()));
        order.setIncoterm(request.getIncoterm());
        order.setPaymentTerm(request.getPaymentTerm());
        order.setDepositRate(request.getDepositRate() == null ? 0 : request.getDepositRate());
        if (order.getDepositRate() < 0 || order.getDepositRate() > 100) {
            throw new IllegalArgumentException("depositRate must be between 0 and 100");
        }
        order.setOrderStatus(OrderStatus.CONFIRMED.name());
        order.setShipDate(request.getShipDate());
        order.setExpectArriveDate(request.getExpectArriveDate());
        order.setOwnerId(request.getOwnerId() == null || request.getOwnerId().trim().isEmpty()
                ? null : request.getOwnerId().trim());
        order.setRemark(request.getRemark());
        order.setDeleted(0L);

        orderMapper.insert(order);
        insertItems(order.getId(), items);
        createReceivables(order);

        if (order.getOpportunityId() != null) {
            markOpportunityWon(order.getOpportunityId());
        }
        notifyOrderConfirmed(order);
        return toDto(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrmOrderDto update(String id, CrmOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("order request must not be null");
        }
        CrmOrderEntity existing = requireOrder(id);
        OrderStatus current = OrderStatus.valueOf(existing.getOrderStatus());
        if (current != OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException(
                    "only unshipped order can be modified: " + existing.getOrderStatus());
        }

        List<CrmOrderItemRequest> items = request.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("order must contain at least one item");
        }
        BigDecimal totalAmount = sumAmount(items).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        boolean amountChanged = existing.getTotalAmount() == null
                || existing.getTotalAmount().compareTo(totalAmount) != 0;

        if (request.getCustomerId() != null && !request.getCustomerId().trim().isEmpty()) {
            requireCustomer(request.getCustomerId());
            existing.setCustomerId(request.getCustomerId().trim());
        }
        existing.setCurrencyCode(resolveCurrency(request.getCurrencyCode(), existing.getCurrencyCode()));
        existing.setTotalAmount(totalAmount);
        existing.setTotalAmountBase(snapshotBaseAmount(totalAmount, existing.getCurrencyCode()));
        existing.setIncoterm(request.getIncoterm());
        existing.setPaymentTerm(request.getPaymentTerm());
        existing.setDepositRate(request.getDepositRate() == null ? existing.getDepositRate() : request.getDepositRate());
        if (existing.getDepositRate() < 0 || existing.getDepositRate() > 100) {
            throw new IllegalArgumentException("depositRate must be between 0 and 100");
        }
        existing.setShipDate(request.getShipDate());
        existing.setExpectArriveDate(request.getExpectArriveDate());
        if (request.getOwnerId() != null && !request.getOwnerId().trim().isEmpty()) {
            existing.setOwnerId(request.getOwnerId().trim());
        }
        existing.setRemark(request.getRemark());

        orderMapper.update(existing);
        orderItemMapper.softDeleteByOrderId(existing.getId(), System.currentTimeMillis());
        insertItems(existing.getId(), items);
        if (amountChanged) {
            regenerateReceivables(existing);
        }
        return toDto(existing);
    }

    @Override
    public CrmOrderDto getById(String id) {
        return toDto(requireOrder(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        CrmOrderEntity existing = requireOrder(id);
        OrderStatus current = OrderStatus.valueOf(existing.getOrderStatus());
        if (current == OrderStatus.SHIPPED || current == OrderStatus.DELIVERED
                || current == OrderStatus.COMPLETED) {
            throw new IllegalArgumentException("shipped order cannot be deleted: " + existing.getOrderStatus());
        }
        // 先清关联再删主体（无物理外键，级联要自己管）
        orderItemMapper.softDeleteByOrderId(id, System.currentTimeMillis());
        for (CrmReceivableEntity receivable : receivableMapper.selectByOrderId(id)) {
            receivableMapper.update(receivable);
        }
        for (CrmShipmentEntity shipment : shipmentMapper.selectByOrderId(id)) {
            shipmentMapper.softDelete(shipment.getId(), System.currentTimeMillis());
        }
        orderMapper.softDelete(id, System.currentTimeMillis());
    }

    @Override
    public PageResponse<CrmOrderDto> list(CrmOrderQueryRequest query) {
        CrmOrderQueryRequest actual = query == null ? new CrmOrderQueryRequest() : query;
        int page = actual.getPage() == null || actual.getPage() < 1 ? 1 : actual.getPage();
        int size = actual.getSize() == null || actual.getSize() < 1
                ? DEFAULT_PAGE_SIZE : Math.min(actual.getSize(), MAX_PAGE_SIZE);
        if ((long) page * size > MAX_PAGE_OFFSET) {
            throw new IllegalArgumentException("page * size must not exceed " + MAX_PAGE_OFFSET);
        }
        PageHelper.startPage(page, size);
        try {
            PageInfo<CrmOrderEntity> info = new PageInfo<>(orderMapper.selectPage(actual));

            List<CrmOrderDto> rows = new ArrayList<>(info.getList().size());
            for (CrmOrderEntity entity : info.getList()) {
                rows.add(toDto(entity));
            }
            PageResponse<CrmOrderDto> response = new PageResponse<>();
            response.setData(rows);
            response.setTotal(info.getTotal());
            response.setPage(info.getPageNum());
            response.setSize(info.getPageSize());
            return response;
        } finally {
            PageHelper.clearPage();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrmOrderDto ship(String id, CrmShipmentRequest request) {
        requireOrder(id);
        if (request == null) {
            throw new IllegalArgumentException("shipment request must not be null");
        }
        CrmShipmentRequest actual = request;
        if (actual.getOrderId() == null || actual.getOrderId().trim().isEmpty()) {
            actual.setOrderId(id);
        } else if (!id.equals(actual.getOrderId())) {
            throw new IllegalArgumentException("shipment orderId does not match the order");
        }
        shipmentService.create(actual);
        return toDto(requireOrder(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrmOrderDto receive(String id, CrmOrderReceiveRequest request) {
        requireOrder(id);
        if (request == null || request.getReceivableId() == null
                || request.getReceivableId().trim().isEmpty()) {
            throw new IllegalArgumentException("receivableId must not be blank");
        }
        CrmReceivableWriteOffRequest writeOff = new CrmReceivableWriteOffRequest();
        writeOff.setAmount(request.getAmount());
        writeOff.setPaidDate(request.getPaidDate());
        writeOff.setPaymentMethod(request.getPaymentMethod());
        writeOff.setRemark(request.getRemark());
        receivableService.writeOff(request.getReceivableId().trim(), writeOff);
        return toDto(requireOrder(id));
    }

    /**
     * 由报价单生成订单：快照报价行的单价金额，报价单置 CONFIRMED，关联商机置 WON.
     */
    private void applyQuotation(CrmOrderEntity order, CrmQuotationEntity quotation) {
        QuotationStatus status = QuotationStatus.valueOf(quotation.getQuotationStatus());
        if (status == QuotationStatus.REJECTED || status == QuotationStatus.EXPIRED) {
            throw new IllegalArgumentException("quotation cannot be converted to order: "
                    + quotation.getQuotationStatus());
        }
        requireCustomer(quotation.getCustomerId());

        order.setCustomerId(quotation.getCustomerId());
        order.setQuotationId(quotation.getId());
        order.setOpportunityId(quotation.getOpportunityId());
        order.setCurrencyCode(resolveCurrency(quotation.getCurrencyCode(), null));
        order.setIncoterm(quotation.getIncoterm());
        order.setOwnerId(quotation.getOwnerId());

        if (status == QuotationStatus.SENT) {
            // 客户确认报价 → 报价单置 CONFIRMED
            quotation.setQuotationStatus(QuotationStatus.CONFIRMED.name());
            quotationMapper.update(quotation);
        }
    }

    /**
     * 报价明细快照成订单明细（单价金额以报价为准，报价后续改价不影响订单）.
     */
    private List<CrmOrderItemRequest> toOrderItems(CrmQuotationEntity quotation) {
        List<CrmQuotationItemEntity> quoted = quotationItemMapper.selectByQuotationId(quotation.getId());
        if (quoted.isEmpty()) {
            throw new IllegalArgumentException("quotation has no item to convert: " + quotation.getId());
        }
        List<CrmOrderItemRequest> items = new ArrayList<>(quoted.size());
        for (CrmQuotationItemEntity item : quoted) {
            CrmOrderItemRequest request = new CrmOrderItemRequest();
            request.setProductId(item.getProductId());
            request.setProductName(item.getProductName());
            request.setSpecification(item.getSpecification());
            request.setQuantity(item.getQuantity());
            request.setUnit(item.getUnit());
            request.setUnitPrice(item.getUnitPrice());
            request.setAmount(item.getAmount());
            items.add(request);
        }
        return items;
    }

    private void insertItems(String orderId, List<CrmOrderItemRequest> items) {
        if (items == null) {
            return;
        }
        for (CrmOrderItemRequest request : items) {
            CrmOrderItemEntity entity = new CrmOrderItemEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setOrderId(orderId);
            entity.setProductId(request.getProductId());
            entity.setProductName(request.getProductName());
            entity.setSpecification(request.getSpecification());
            entity.setQuantity(request.getQuantity());
            entity.setUnit(request.getUnit());
            entity.setUnitPrice(request.getUnitPrice());
            entity.setAmount(request.getAmount());
            entity.setDeleted(0L);
            orderItemMapper.insert(entity);
        }
    }

    /**
     * 按定金比例拆应收：定金 + 尾款；无定金则一条全款.
     */
    private void createReceivables(CrmOrderEntity order) {
        BigDecimal total = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
        int depositRate = order.getDepositRate() == null ? 0 : order.getDepositRate();

        if (depositRate <= 0) {
            insertReceivable(order, "FULL", total, order.getExpectArriveDate());
            return;
        }
        BigDecimal deposit = total.multiply(BigDecimal.valueOf(depositRate))
                .divide(BigDecimal.valueOf(100), AMOUNT_SCALE, RoundingMode.HALF_UP);
        insertReceivable(order, "DEPOSIT", deposit, order.getShipDate() == null
                ? LocalDate.now() : order.getShipDate());
        BigDecimal balance = total.subtract(deposit).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        if (balance.signum() > 0) {
            insertReceivable(order, "BALANCE", balance, order.getExpectArriveDate());
        }
    }

    /**
     * 订单金额变化后重新拆应收（未发货订单还没有收款记录，安全重建）.
     */
    private void regenerateReceivables(CrmOrderEntity order) {
        for (CrmReceivableEntity receivable : receivableMapper.selectByOrderId(order.getId())) {
            receivableMapper.update(receivable);
        }
        createReceivables(order);
    }

    private void insertReceivable(CrmOrderEntity order, String type, BigDecimal amount, LocalDate dueDate) {
        CrmReceivableEntity entity = new CrmReceivableEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setOrderId(order.getId());
        entity.setCurrencyCode(order.getCurrencyCode());
        entity.setReceivableType(type);
        entity.setReceivableAmount(amount.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP));
        entity.setReceivedAmount(BigDecimal.ZERO.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP));
        entity.setDueDate(dueDate);
        entity.setReceivableStatus(ReceivableStatus.UNPAID.name());
        entity.setOverdueDays(0);
        entity.setOwnerId(order.getOwnerId());
        entity.setDeleted(0L);
        receivableMapper.insert(entity);
    }

    /**
     * 关联商机自动置 WON（赢单）.
     */
    private void markOpportunityWon(String opportunityId) {
        CrmOpportunityEntity opportunity = opportunityMapper.selectById(opportunityId);
        if (opportunity == null) {
            return;
        }
        OpportunityStage current = OpportunityStage.fromCode(opportunity.getStage());
        if (current == OpportunityStage.WON) {
            return;
        }
        opportunityStageService.assertCanTransit(current, OpportunityStage.WON, null);
        opportunity.setStage(OpportunityStage.WON.name());
        opportunity.setWinRate(OpportunityStage.WON.getDefaultWinRate());
        opportunityMapper.update(opportunity);
    }

    private BigDecimal sumAmount(List<CrmOrderItemRequest> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (CrmOrderItemRequest item : items) {
            if (item == null) {
                throw new IllegalArgumentException("order item must not be null");
            }
            if (item.getQuantity() == null || item.getQuantity().signum() <= 0) {
                throw new IllegalArgumentException("order item quantity must be positive");
            }
            BigDecimal amount = item.getAmount();
            if (amount == null) {
                // 未给行金额时按单价 × 数量算
                if (item.getUnitPrice() == null) {
                    throw new IllegalArgumentException("order item requires unitPrice or amount");
                }
                amount = item.getUnitPrice().multiply(item.getQuantity())
                        .setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
            }
            total = total.add(amount);
        }
        return total;
    }

    private CrmOrderEntity requireOrder(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("order id must not be blank");
        }
        CrmOrderEntity entity = orderMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("order not found: " + id);
        }
        return entity;
    }

    private CrmQuotationEntity requireQuotation(String id) {
        CrmQuotationEntity entity = quotationMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("quotation not found: " + id);
        }
        return entity;
    }

    private void requireCustomer(String customerId) {
        if (customerMapper.selectById(customerId) == null) {
            throw new IllegalArgumentException("customer not found: " + customerId);
        }
    }

    private String resolveCurrency(String requested, String fallback) {
        if (requested != null && !requested.trim().isEmpty()) {
            return requested.trim().toUpperCase(Locale.ROOT);
        }
        if (fallback != null && !fallback.trim().isEmpty()) {
            return fallback.trim().toUpperCase(Locale.ROOT);
        }
        return DEFAULT_CURRENCY;
    }

    /**
     * 折算本位币快照：无汇率可用时置 0（汇率模块在 M5 落地后才有数据）.
     */
    private BigDecimal snapshotBaseAmount(BigDecimal amount, String currencyCode) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (BASE_CURRENCY.equalsIgnoreCase(currencyCode)) {
            return amount.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal converted = currencyRateQueryService.convert(
                amount, currencyCode, BASE_CURRENCY, LocalDate.now());
        if (converted == null) {
            return BigDecimal.ZERO;
        }
        return converted.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    /**
     * 生成订单号：ORD + 日期 + 6 位随机数，唯一性由数据库唯一索引兜底.
     */
    private String generateNo() {
        int suffix = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "ORD" + LocalDate.now().format(NO_DATE) + suffix;
    }

    private CrmOrderDto toDto(CrmOrderEntity entity) {
        CrmOrderDto dto = new CrmOrderDto();
        dto.setId(entity.getId());
        dto.setOrderNo(entity.getOrderNo());
        dto.setCustomerId(entity.getCustomerId());
        dto.setQuotationId(entity.getQuotationId());
        dto.setOpportunityId(entity.getOpportunityId());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setTotalAmountBase(entity.getTotalAmountBase());
        dto.setIncoterm(entity.getIncoterm());
        dto.setPaymentTerm(entity.getPaymentTerm());
        dto.setDepositRate(entity.getDepositRate());
        dto.setOrderStatus(entity.getOrderStatus());
        dto.setShipDate(entity.getShipDate());
        dto.setExpectArriveDate(entity.getExpectArriveDate());
        dto.setOwnerId(entity.getOwnerId());
        dto.setRemark(entity.getRemark());

        List<CrmOrderItemDto> items = new ArrayList<>();
        for (CrmOrderItemEntity item : orderItemMapper.selectByOrderId(entity.getId())) {
            CrmOrderItemDto itemDto = new CrmOrderItemDto();
            itemDto.setId(item.getId());
            itemDto.setProductId(item.getProductId());
            itemDto.setProductName(item.getProductName());
            itemDto.setSpecification(item.getSpecification());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setUnit(item.getUnit());
            itemDto.setUnitPrice(item.getUnitPrice());
            itemDto.setAmount(item.getAmount());
            items.add(itemDto);
        }
        dto.setItems(items);

        List<CrmShipmentDto> shipments = new ArrayList<>();
        for (CrmShipmentEntity shipment : shipmentMapper.selectByOrderId(entity.getId())) {
            CrmShipmentDto shipmentDto = new CrmShipmentDto();
            shipmentDto.setId(shipment.getId());
            shipmentDto.setOrderId(shipment.getOrderId());
            shipmentDto.setShippingNo(shipment.getShippingNo());
            shipmentDto.setShippingType(shipment.getShippingType());
            shipmentDto.setDeparturePort(shipment.getDeparturePort());
            shipmentDto.setDestinationPort(shipment.getDestinationPort());
            shipmentDto.setShipDate(shipment.getShipDate());
            shipmentDto.setExpectArriveDate(shipment.getExpectArriveDate());
            shipmentDto.setActualArriveDate(shipment.getActualArriveDate());
            shipmentDto.setTotalCbm(shipment.getTotalCbm());
            shipmentDto.setTotalWeight(shipment.getTotalWeight());
            shipmentDto.setShipmentStatus(shipment.getShipmentStatus());
            shipmentDto.setRemark(shipment.getRemark());
            shipments.add(shipmentDto);
        }
        dto.setShipments(shipments);

        List<CrmReceivableDto> receivables = receivableService.listByOrder(entity.getId());
        dto.setReceivables(receivables);
        return dto;
    }

    /**
     * 订单确认后发站内信（需求文档 §3.11 的"订单已确认"）.
     * <p>
     * 站内信是旁路，发送失败只记日志，不影响订单创建事务.
     * </p>
     *
     * @param order 订单
     */
    private void notifyOrderConfirmed(CrmOrderEntity order) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("amount", MessageTextFactory.money(order.getTotalAmount(), order.getCurrencyCode()));
        messageNotifier.notifyOwner(MessageType.ORDER_CONFIRMED, MessageBizType.ORDER.name(), order.getId(),
                order.getOrderNo(), order.getOwnerId(), placeholders);
    }
}
