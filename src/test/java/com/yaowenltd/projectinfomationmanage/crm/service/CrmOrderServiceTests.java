/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.common.MessageNotifier;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmOpportunityMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmOrderItemMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmOrderMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmQuotationItemMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmQuotationMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmReceivableMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmShipmentMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmOrderDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmOrderItemRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmOrderRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmOrderReceiveRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmShipmentRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmOpportunityEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmOrderEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmOrderItemEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmQuotationEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmQuotationItemEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmReceivableEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CustomerEntity;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.CrmOrderServiceImpl;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.CrmReceivableServiceImpl;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.CrmShipmentServiceImpl;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.OpportunityStageServiceImpl;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.OrderStatusServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.message.MessageDraft;

/**
 * {@link CrmOrderService} 的单元测试。
 * <p>
 * 契约来自需求文档 §3.7、§3.8、§5.4：
 * 由报价转订单时**快照**报价行的单价金额，报价后续改价不影响订单；
 * 订单创建时按定金比例拆出应收；只有未发货的订单可改；
 * 已发货的订单不可删。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class CrmOrderServiceTests {

    @Mock
    private CrmOrderMapper orderMapper;

    @Mock
    private CrmOrderItemMapper orderItemMapper;

    @Mock
    private CrmShipmentMapper shipmentMapper;

    @Mock
    private CrmReceivableMapper receivableMapper;

    @Mock
    private CrmQuotationMapper quotationMapper;

    @Mock
    private CrmQuotationItemMapper quotationItemMapper;

    @Mock
    private CrmOpportunityMapper opportunityMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private CurrencyRateQueryService currencyRateQueryService;

    @Mock
    private MessageService messageService;

    private CrmOrderService crmOrderService;

    @BeforeEach
    void setUp() {
        OrderStatusService orderStatusService = new OrderStatusServiceImpl();
        CrmReceivableService receivableService = new CrmReceivableServiceImpl(
                receivableMapper, orderMapper, orderStatusService);
        CrmShipmentService shipmentService = new CrmShipmentServiceImpl(
                shipmentMapper, orderMapper, orderStatusService, receivableService, new MessageNotifier(messageService));
        crmOrderService = new CrmOrderServiceImpl(orderMapper, orderItemMapper, shipmentMapper,
                receivableMapper, quotationMapper, quotationItemMapper, opportunityMapper, customerMapper,
                orderStatusService, new OpportunityStageServiceImpl(), shipmentService, receivableService,
                currencyRateQueryService, new MessageNotifier(messageService));
    }

    private static CrmOrderRequest request() {
        CrmOrderRequest request = new CrmOrderRequest();
        request.setCustomerId("C1");
        request.setCurrencyCode("USD");
        request.setIncoterm("FOB");
        request.setPaymentTerm("TT");
        request.setOwnerId("U1");

        CrmOrderItemRequest item = new CrmOrderItemRequest();
        item.setProductId("P1");
        item.setProductName("不锈钢水壶");
        item.setQuantity(new BigDecimal("100"));
        item.setUnit("PCS");
        item.setUnitPrice(new BigDecimal("12.600000"));
        item.setAmount(new BigDecimal("1260.00"));
        request.setItems(Collections.singletonList(item));
        return request;
    }

    private static CustomerEntity customer() {
        CustomerEntity entity = new CustomerEntity();
        entity.setId("C1");
        entity.setCompanyName("Acme Trading Co., Ltd.");
        entity.setCountryCode("US");
        entity.setSource("TRADE_SHOW");
        entity.setLevel("B");
        entity.setOwnerId("U1");
        entity.setDeleted(0L);
        return entity;
    }

    private static CrmOrderItemEntity orderItem() {
        CrmOrderItemEntity item = new CrmOrderItemEntity();
        item.setId("OI1");
        item.setOrderId("O1");
        item.setProductId("P1");
        item.setProductName("不锈钢水壶");
        item.setQuantity(new BigDecimal("100"));
        item.setUnit("PCS");
        item.setUnitPrice(new BigDecimal("12.600000"));
        item.setAmount(new BigDecimal("1260.00"));
        return item;
    }

    private static CrmOrderEntity existing(String status) {
        CrmOrderEntity entity = new CrmOrderEntity();
        entity.setId("O1");
        entity.setOrderNo("ORD20261108000001");
        entity.setCustomerId("C1");
        entity.setCurrencyCode("USD");
        entity.setTotalAmount(new BigDecimal("1260.00"));
        entity.setTotalAmountBase(new BigDecimal("1260.00"));
        entity.setDepositRate(0);
        entity.setOrderStatus(status);
        entity.setOwnerId("U1");
        entity.setDeleted(0L);
        return entity;
    }

    @Test
    @DisplayName("直接建单时订单金额由明细行汇总得出")
    void testCreateComputesTotalFromItems() {
        when(customerMapper.selectById("C1")).thenReturn(customer());
        when(orderItemMapper.selectByOrderId(any())).thenReturn(Collections.singletonList(orderItem()));

        CrmOrderDto dto = crmOrderService.create(request());

        assertEquals("CONFIRMED", dto.getOrderStatus());
        assertEquals(0, new BigDecimal("1260.00").compareTo(dto.getTotalAmount()));
        assertEquals(1, dto.getItems().size());
        assertEquals("不锈钢水壶", dto.getItems().get(0).getProductName());
        assertEquals(0, new BigDecimal("12.600000").compareTo(dto.getItems().get(0).getUnitPrice()));
    }

    @Test
    @DisplayName("新建订单时客户必须存在")
    void testCreateRejectsMissingCustomer() {
        when(customerMapper.selectById("C1")).thenReturn(null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmOrderService.create(request()));
        assertTrue(error.getMessage().contains("customer not found"));
        verify(orderMapper, never()).insert(any());
    }

    @Test
    @DisplayName("新建订单时明细不能为空")
    void testCreateRejectsEmptyItems() {
        CrmOrderRequest request = request();
        request.setItems(new java.util.ArrayList<>());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmOrderService.create(request));
        assertTrue(error.getMessage().contains("at least one item"));
    }

    @Test
    @DisplayName("定金比例必须在 0-100 之间")
    void testCreateValidatesDepositRate() {
        when(customerMapper.selectById("C1")).thenReturn(customer());
        CrmOrderRequest request = request();
        request.setDepositRate(101);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmOrderService.create(request));
        assertTrue(error.getMessage().contains("depositRate"));
    }

    @Test
    @DisplayName("有定金时应收拆成定金 + 尾款两条")
    void testCreateSplitsReceivablesByDepositRate() {
        when(customerMapper.selectById("C1")).thenReturn(customer());
        CrmOrderRequest request = request();
        request.setDepositRate(30);

        crmOrderService.create(request);

        ArgumentCaptor<CrmReceivableEntity> captor =
                ArgumentCaptor.forClass(CrmReceivableEntity.class);
        verify(receivableMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        List<CrmReceivableEntity> receivables = captor.getAllValues();
        assertEquals("DEPOSIT", receivables.get(0).getReceivableType());
        assertEquals(0, new BigDecimal("378.00").compareTo(receivables.get(0).getReceivableAmount()));
        assertEquals("BALANCE", receivables.get(1).getReceivableType());
        assertEquals(0, new BigDecimal("882.00").compareTo(receivables.get(1).getReceivableAmount()));
    }

    @Test
    @DisplayName("无定金时应收只有一条全款")
    void testCreateMakesSingleReceivableWithoutDeposit() {
        when(customerMapper.selectById("C1")).thenReturn(customer());

        crmOrderService.create(request());

        ArgumentCaptor<CrmReceivableEntity> captor =
                ArgumentCaptor.forClass(CrmReceivableEntity.class);
        verify(receivableMapper).insert(captor.capture());
        assertEquals("FULL", captor.getValue().getReceivableType());
        assertEquals(0, new BigDecimal("1260.00").compareTo(captor.getValue().getReceivableAmount()));
    }

    @Test
    @DisplayName("由报价建单时快照报价行的单价金额并把报价置 CONFIRMED")
    void testCreateFromQuotationSnapshotsItems() {
        CrmQuotationEntity quotation = new CrmQuotationEntity();
        quotation.setId("Q1");
        quotation.setCustomerId("C1");
        quotation.setCurrencyCode("USD");
        quotation.setIncoterm("FOB");
        quotation.setOwnerId("U1");
        quotation.setTotalAmount(new BigDecimal("1260.00"));
        quotation.setQuotationStatus("SENT");

        CrmQuotationItemEntity quoted = new CrmQuotationItemEntity();
        quoted.setId("QI1");
        quoted.setQuotationId("Q1");
        quoted.setProductId("P1");
        quoted.setProductName("不锈钢水壶");
        quoted.setQuantity(new BigDecimal("100"));
        quoted.setUnit("PCS");
        quoted.setUnitPrice(new BigDecimal("12.600000"));
        quoted.setAmount(new BigDecimal("1260.00"));

        when(quotationMapper.selectById("Q1")).thenReturn(quotation);
        when(quotationItemMapper.selectByQuotationId("Q1")).thenReturn(Collections.singletonList(quoted));
        when(customerMapper.selectById("C1")).thenReturn(customer());
        when(orderItemMapper.selectByOrderId(any())).thenReturn(Collections.singletonList(orderItem()));

        CrmOrderDto dto = crmOrderService.create(quotationRequest());

        assertEquals("Q1", dto.getQuotationId());
        assertEquals(0, new BigDecimal("1260.00").compareTo(dto.getTotalAmount()));
        assertEquals(0, new BigDecimal("12.600000").compareTo(dto.getItems().get(0).getUnitPrice()));
        verify(quotationMapper).update(any());
        assertEquals("CONFIRMED", quotation.getQuotationStatus());
    }

    @Test
    @DisplayName("由报价建单时关联商机自动置 WON")
    void testCreateFromQuotationMarksOpportunityWon() {
        CrmQuotationEntity quotation = new CrmQuotationEntity();
        quotation.setId("Q1");
        quotation.setCustomerId("C1");
        quotation.setCurrencyCode("USD");
        quotation.setOwnerId("U1");
        quotation.setTotalAmount(new BigDecimal("1260.00"));
        quotation.setOpportunityId("OP1");
        quotation.setQuotationStatus("CONFIRMED");

        CrmQuotationItemEntity quoted = new CrmQuotationItemEntity();
        quoted.setId("QI1");
        quoted.setQuotationId("Q1");
        quoted.setProductId("P1");
        quoted.setProductName("不锈钢水壶");
        quoted.setQuantity(new BigDecimal("100"));
        quoted.setUnitPrice(new BigDecimal("12.600000"));
        quoted.setAmount(new BigDecimal("1260.00"));

        CrmOpportunityEntity opportunity = new CrmOpportunityEntity();
        opportunity.setId("OP1");
        opportunity.setStage("NEGOTIATION");

        when(quotationMapper.selectById("Q1")).thenReturn(quotation);
        when(quotationItemMapper.selectByQuotationId("Q1")).thenReturn(Collections.singletonList(quoted));
        when(customerMapper.selectById("C1")).thenReturn(customer());
        when(opportunityMapper.selectById("OP1")).thenReturn(opportunity);

        crmOrderService.create(quotationRequest());

        verify(opportunityMapper).update(any());
        assertEquals("WON", opportunity.getStage());
    }

    @Test
    @DisplayName("已拒绝或已过期的报价不能转订单")
    void testCreateFromRejectedQuotationIsRejected() {
        CrmQuotationEntity quotation = new CrmQuotationEntity();
        quotation.setId("Q1");
        quotation.setCustomerId("C1");
        quotation.setQuotationStatus("REJECTED");
        when(quotationMapper.selectById("Q1")).thenReturn(quotation);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmOrderService.create(quotationRequest()));
        assertTrue(error.getMessage().contains("cannot be converted to order"));
    }

    @Test
    @DisplayName("只有未发货的订单可以改")
    void testUpdateRejectsShippedOrder() {
        when(orderMapper.selectById("O1")).thenReturn(existing("SHIPPED"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmOrderService.update("O1", request()));
        assertTrue(error.getMessage().contains("only unshipped order"));
        verify(orderMapper, never()).update(any());
    }

    @Test
    @DisplayName("更新订单会整批替换明细")
    void testUpdateReplacesItems() {
        when(orderMapper.selectById("O1")).thenReturn(existing("CONFIRMED"));
        when(customerMapper.selectById("C1")).thenReturn(customer());

        crmOrderService.update("O1", request());

        verify(orderItemMapper).softDeleteByOrderId(eq("O1"), anyLong());
        verify(orderItemMapper).insert(any());
        verify(orderMapper).update(any());
    }

    @Test
    @DisplayName("已发货的订单不允许删除")
    void testDeleteRejectsShippedOrder() {
        when(orderMapper.selectById("O1")).thenReturn(existing("SHIPPED"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmOrderService.delete("O1"));
        assertTrue(error.getMessage().contains("cannot be deleted"));
        verify(orderMapper, never()).softDelete(any(), anyLong());
    }

    @Test
    @DisplayName("未发货的订单删除时先清关联")
    void testDeleteRemovesRelations() {
        when(orderMapper.selectById("O1")).thenReturn(existing("CONFIRMED"));
        when(receivableMapper.selectByOrderId("O1")).thenReturn(Collections.emptyList());
        when(shipmentMapper.selectByOrderId("O1")).thenReturn(Collections.emptyList());

        crmOrderService.delete("O1");

        verify(orderItemMapper).softDeleteByOrderId(eq("O1"), anyLong());
        verify(orderMapper).softDelete(eq("O1"), anyLong());
    }

    @Test
    @DisplayName("登记发货会把订单置 SHIPPED")
    void testShipMovesOrderToShipped() {
        CrmOrderEntity order = existing("CONFIRMED");
        when(orderMapper.selectById("O1")).thenReturn(order);

        CrmShipmentRequest request = new CrmShipmentRequest();
        request.setShippingType("SEA");
        request.setShipDate(java.time.LocalDate.now());

        crmOrderService.ship("O1", request);

        verify(shipmentMapper).insert(any());
        verify(orderMapper).update(any());
        assertEquals("SHIPPED", order.getOrderStatus());
    }

    @Test
    @DisplayName("登记了实际到港日时订单置 DELIVERED")
    void testShipWithArrivalMarksOrderDelivered() {
        CrmOrderEntity order = existing("SHIPPED");
        when(orderMapper.selectById("O1")).thenReturn(order);

        CrmShipmentService shipmentService = new CrmShipmentServiceImpl(shipmentMapper, orderMapper,
                new OrderStatusServiceImpl(), new CrmReceivableServiceImpl(receivableMapper, orderMapper,
                        new OrderStatusServiceImpl()), new MessageNotifier(messageService));
        CrmShipmentRequest request = new CrmShipmentRequest();
        request.setOrderId("O1");
        request.setShippingType("SEA");
        request.setActualArriveDate(java.time.LocalDate.now());

        shipmentService.create(request);

        assertEquals("DELIVERED", order.getOrderStatus());
    }

    @Test
    @DisplayName("订单还在 SHIPPED 时核销不应报错，也不越级置 COMPLETED")
    void testWriteOffOnShippedOrderDoesNotSkipStateMachine() {
        when(orderMapper.selectById("O1")).thenReturn(existing("SHIPPED"));
        CrmReceivableEntity receivable = new CrmReceivableEntity();
        receivable.setId("RV1");
        receivable.setOrderId("O1");
        receivable.setReceivableAmount(new BigDecimal("100.00"));
        receivable.setReceivedAmount(BigDecimal.ZERO);
        receivable.setReceivableStatus("UNPAID");
        when(receivableMapper.selectById("RV1")).thenReturn(receivable);

        com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableWriteOffRequest request =
                new com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableWriteOffRequest();
        request.setAmount(new BigDecimal("100.00"));

        CrmReceivableService receivableService = new CrmReceivableServiceImpl(receivableMapper, orderMapper,
                new OrderStatusServiceImpl());
        receivableService.writeOff("RV1", request);

        verify(receivableMapper).update(any());
        assertEquals("PAID", receivable.getReceivableStatus());
    }

    @Test
    @DisplayName("登记收款会核销指定应收")
    void testReceiveWritesOffReceivable() {
        when(orderMapper.selectById("O1")).thenReturn(existing("SHIPPED"));
        CrmReceivableEntity receivable = new CrmReceivableEntity();
        receivable.setId("RV1");
        receivable.setOrderId("O1");
        receivable.setCurrencyCode("USD");
        receivable.setReceivableAmount(new BigDecimal("1260.00"));
        receivable.setReceivedAmount(BigDecimal.ZERO);
        receivable.setReceivableStatus("UNPAID");
        receivable.setOwnerId("U1");
        when(receivableMapper.selectById("RV1")).thenReturn(receivable);

        CrmOrderReceiveRequest request = new CrmOrderReceiveRequest();
        request.setReceivableId("RV1");
        request.setAmount(new BigDecimal("500.00"));

        crmOrderService.receive("O1", request);

        verify(receivableMapper).update(any());
        assertEquals(0, new BigDecimal("500.00").compareTo(receivable.getReceivedAmount()));
        assertEquals("PARTIAL", receivable.getReceivableStatus());
    }

    @Test
    @DisplayName("核销金额超过未收金额应被拒绝")
    void testWriteOffRejectsAmountOverOutstanding() {
        CrmReceivableEntity receivable = new CrmReceivableEntity();
        receivable.setId("RV1");
        receivable.setOrderId("O1");
        receivable.setReceivableAmount(new BigDecimal("100.00"));
        receivable.setReceivedAmount(new BigDecimal("60.00"));
        receivable.setReceivableStatus("PARTIAL");
        when(receivableMapper.selectById("RV1")).thenReturn(receivable);

        com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableWriteOffRequest request =
                new com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableWriteOffRequest();
        request.setAmount(new BigDecimal("50.00"));

        CrmReceivableService receivableService = new CrmReceivableServiceImpl(receivableMapper, orderMapper,
                new OrderStatusServiceImpl());
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> receivableService.writeOff("RV1", request));
        assertTrue(error.getMessage().contains("exceeds the outstanding amount"));
    }

    @Test
    @DisplayName("应收全部收清且订单已交付时订单置 COMPLETED")
    void testAllPaidOrderBecomesCompleted() {
        CrmOrderEntity order = existing("DELIVERED");
        when(orderMapper.selectById("O1")).thenReturn(order);

        CrmReceivableEntity receivable = new CrmReceivableEntity();
        receivable.setId("RV1");
        receivable.setOrderId("O1");
        receivable.setReceivableAmount(new BigDecimal("100.00"));
        receivable.setReceivedAmount(BigDecimal.ZERO);
        receivable.setReceivableStatus("UNPAID");
        when(receivableMapper.selectById("RV1")).thenReturn(receivable);
        when(receivableMapper.selectByOrderId("O1")).thenReturn(Collections.singletonList(receivable));

        com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableWriteOffRequest request =
                new com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableWriteOffRequest();
        request.setAmount(new BigDecimal("100.00"));

        CrmReceivableService receivableService = new CrmReceivableServiceImpl(receivableMapper, orderMapper,
                new OrderStatusServiceImpl());
        receivableService.writeOff("RV1", request);

        assertEquals("COMPLETED", order.getOrderStatus());
    }

    @Test
    @DisplayName("账龄分析按 4 个区间补齐空桶")
    void testAgingFillsEmptyBuckets() {
        com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableAgingBucket bucket =
                new com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableAgingBucket();
        bucket.setBucket("31-60");
        bucket.setAmount(new BigDecimal("1000.00"));
        bucket.setCount(2);
        when(receivableMapper.selectAging()).thenReturn(Collections.singletonList(bucket));

        CrmReceivableService receivableService = new CrmReceivableServiceImpl(receivableMapper, orderMapper,
                new OrderStatusServiceImpl());
        com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableAgingDto dto =
                receivableService.aging();

        assertEquals(4, dto.getBuckets().size());
        assertEquals(0, new BigDecimal("1000.00").compareTo(dto.getTotalOutstandingAmount()));
        assertEquals(2L, dto.getTotalOutstandingCount());
    }

    @Test
    @DisplayName("分页查询应把结果映射成 DTO")
    void testListMapsRows() {
        when(orderMapper.selectPage(any())).thenReturn(Collections.singletonList(existing("CONFIRMED")));

        com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmOrderQueryRequest query =
                new com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmOrderQueryRequest();
        query.setPage(1);
        query.setSize(10);

        assertEquals(1, crmOrderService.list(query).getData().size());
    }

    private static CrmOrderRequest quotationRequest() {
        CrmOrderRequest request = new CrmOrderRequest();
        request.setQuotationId("Q1");
        return request;
    }

    @Test
    @DisplayName("创建订单后给负责人发订单已确认站内信")
    void testCreateSendsOrderConfirmedMessage() {
        when(customerMapper.selectById("C1")).thenReturn(customer());
        when(orderItemMapper.selectByOrderId(any())).thenReturn(Collections.singletonList(orderItem()));

        crmOrderService.create(request());

        ArgumentCaptor<MessageDraft> captor = ArgumentCaptor.forClass(MessageDraft.class);
        verify(messageService).sendToReceivers(captor.capture(), eq(Collections.singletonList("U1")));
        MessageDraft draft = captor.getValue();
        assertEquals("ORDER_CONFIRMED", draft.getMessageType());
        assertEquals("ORDER", draft.getBizType());
    }

    @Test
    @DisplayName("发货后给订单负责人发订单已发货站内信")
    void testShipmentSendsOrderShippedMessage() {
        OrderStatusService orderStatusService = new OrderStatusServiceImpl();
        CrmShipmentService shipmentService = new CrmShipmentServiceImpl(shipmentMapper, orderMapper,
                orderStatusService,
                new CrmReceivableServiceImpl(receivableMapper, orderMapper, orderStatusService),
                new MessageNotifier(messageService));
        when(orderMapper.selectById("O1")).thenReturn(existing("CONFIRMED"));
        CrmShipmentRequest request = new CrmShipmentRequest();
        request.setOrderId("O1");
        request.setShippingType("SEA");
        request.setShippingNo("BL20261108001");

        shipmentService.create(request);

        ArgumentCaptor<MessageDraft> captor = ArgumentCaptor.forClass(MessageDraft.class);
        verify(messageService).sendToReceivers(captor.capture(), eq(Collections.singletonList("U1")));
        MessageDraft draft = captor.getValue();
        assertEquals("ORDER_SHIPPED", draft.getMessageType());
        assertTrue(draft.getContent().contains("BL20261108001"));
    }

}
