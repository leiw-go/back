/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.task;

import com.yaowenltd.projectinfomationmanage.crm.common.MessageNotifier;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmInquiryMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmOrderMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmReceivableMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.message.MessageDraft;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmInquiryEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmOrderEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmReceivableEntity;
import com.yaowenltd.projectinfomationmanage.crm.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CRM 定时任务（站内信清理 / 应收逾期 / 询盘超时）的单元测试.
 * <p>
 * 契约来自需求文档 §3.11：已读消息 90 天后清理、未读不清理；
 * 应收逾期与询盘超时都要**幂等**，同一业务单据不重复发消息。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CrmScheduledTaskTests {

    @Mock
    private MessageService messageService;

    @Mock
    private CrmReceivableMapper receivableMapper;

    @Mock
    private CrmOrderMapper orderMapper;

    @Mock
    private CrmInquiryMapper inquiryMapper;

    private MessageNotifier messageNotifier;

    @BeforeEach
    void setUp() {
        messageNotifier = new MessageNotifier(messageService);
    }

    private static CrmReceivableEntity receivable(String id, LocalDate dueDate, String status) {
        CrmReceivableEntity entity = new CrmReceivableEntity();
        entity.setId(id);
        entity.setOrderId("O1");
        entity.setCurrencyCode("USD");
        entity.setReceivableType("BALANCE");
        entity.setReceivableAmount(new BigDecimal("1260.00"));
        entity.setReceivedAmount(new BigDecimal("260.00"));
        entity.setDueDate(dueDate);
        entity.setReceivableStatus(status);
        entity.setOwnerId("U1");
        entity.setDeleted(0L);
        return entity;
    }

    private static CrmOrderEntity order() {
        CrmOrderEntity entity = new CrmOrderEntity();
        entity.setId("O1");
        entity.setOrderNo("ORD20261108000001");
        entity.setCurrencyCode("USD");
        entity.setTotalAmount(new BigDecimal("1260.00"));
        entity.setTotalAmountBase(new BigDecimal("1260.00"));
        entity.setOrderStatus("SHIPPED");
        entity.setOwnerId("U1");
        entity.setDeleted(0L);
        return entity;
    }

    private static CrmInquiryEntity inquiry(String id) {
        CrmInquiryEntity entity = new CrmInquiryEntity();
        entity.setId(id);
        entity.setInquiryNo("INQ20261108000001");
        entity.setOwnerId("U1");
        entity.setInquiryStatus("NEW");
        entity.setDeleted(0L);
        return entity;
    }

    // ------------------------------------------------------------------ 清理任务

    @Test
    @DisplayName("清理任务按保留天数清理已读消息")
    void testCleanupUsesRetentionDays() {
        when(messageService.cleanupReadMessages(anyInt())).thenReturn(7);
        CrmMessageCleanupTask task = new CrmMessageCleanupTask(messageService, 90);

        int cleaned = task.cleanupExpired();

        assertEquals(7, cleaned);
        assertEquals(90, task.getRetentionDays());
        verify(messageService).cleanupReadMessages(90);
    }

    // ------------------------------------------------------------------ 应收逾期

    @Test
    @DisplayName("逾期应收置为 OVERDUE 并给订单负责人发催款站内信")
    void testOverdueReceivableIsNotified() {
        LocalDate today = LocalDate.of(2026, 11, 9);
        when(receivableMapper.selectOverdueList(today))
                .thenReturn(Collections.singletonList(receivable("R1", today.minusDays(5), "UNPAID")));
        when(orderMapper.selectById("O1")).thenReturn(order());
        CrmReceivableOverdueTask task = new CrmReceivableOverdueTask(receivableMapper, orderMapper,
                messageService, messageNotifier);

        int notified = task.scanOverdue(today);

        assertEquals(1, notified);
        ArgumentCaptor<CrmReceivableEntity> captor = ArgumentCaptor.forClass(CrmReceivableEntity.class);
        verify(receivableMapper).update(captor.capture());
        assertEquals("OVERDUE", captor.getValue().getReceivableStatus());
        assertEquals(5, captor.getValue().getOverdueDays());

        ArgumentCaptor<MessageDraft> draftCaptor = ArgumentCaptor.forClass(MessageDraft.class);
        verify(messageService).sendToReceivers(draftCaptor.capture(), eq(Collections.singletonList("U1")));
        MessageDraft draft = draftCaptor.getValue();
        assertEquals("RECEIVABLE_OVERDUE", draft.getMessageType());
        assertEquals("RECEIVABLE", draft.getBizType());
        assertTrue(draft.getContent().contains("1000.00"));
    }

    @Test
    @DisplayName("已发过逾期消息的应收不重复通知")
    void testOverdueReceivableIsIdempotent() {
        LocalDate today = LocalDate.of(2026, 11, 9);
        when(receivableMapper.selectOverdueList(today))
                .thenReturn(Collections.singletonList(receivable("R1", today.minusDays(5), "OVERDUE")));
        when(messageService.existsSystemMessage("RECEIVABLE", "R1", "RECEIVABLE_OVERDUE"))
                .thenReturn(true);
        CrmReceivableOverdueTask task = new CrmReceivableOverdueTask(receivableMapper, orderMapper,
                messageService, messageNotifier);

        int notified = task.scanOverdue(today);

        assertEquals(0, notified);
        verify(messageService, never()).sendToReceivers(any(), any());
    }

    @Test
    @DisplayName("没有逾期应收时不发任何消息")
    void testNoOverdueReceivable() {
        LocalDate today = LocalDate.of(2026, 11, 9);
        when(receivableMapper.selectOverdueList(today)).thenReturn(Collections.emptyList());
        CrmReceivableOverdueTask task = new CrmReceivableOverdueTask(receivableMapper, orderMapper,
                messageService, messageNotifier);

        assertEquals(0, task.scanOverdue(today));
        verify(messageService, never()).sendToReceivers(any(), any());
    }

    // ------------------------------------------------------------------ 询盘超时

    @Test
    @DisplayName("超时未报价的询盘给负责人发提醒")
    void testTimeoutInquiryIsNotified() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 9, 9, 0);
        when(inquiryMapper.selectPendingQuoteList(any())).thenReturn(Collections.singletonList(inquiry("I1")));
        CrmInquiryTimeoutTask task = new CrmInquiryTimeoutTask(inquiryMapper, messageService, messageNotifier, 3);

        int notified = task.scanTimeout(now);

        assertEquals(1, notified);
        ArgumentCaptor<MessageDraft> draftCaptor = ArgumentCaptor.forClass(MessageDraft.class);
        verify(messageService).sendToReceivers(draftCaptor.capture(), eq(Collections.singletonList("U1")));
        MessageDraft draft = draftCaptor.getValue();
        assertEquals("INQUIRY_TIMEOUT", draft.getMessageType());
        assertEquals("INQUIRY", draft.getBizType());
        assertTrue(draft.getContent().contains("3"));
    }

    @Test
    @DisplayName("已发过超时提醒的询盘不重复通知")
    void testTimeoutInquiryIsIdempotent() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 9, 9, 0);
        when(inquiryMapper.selectPendingQuoteList(any())).thenReturn(Collections.singletonList(inquiry("I1")));
        when(messageService.existsSystemMessage("INQUIRY", "I1", "INQUIRY_TIMEOUT")).thenReturn(true);
        CrmInquiryTimeoutTask task = new CrmInquiryTimeoutTask(inquiryMapper, messageService, messageNotifier, 3);

        assertEquals(0, task.scanTimeout(now));
        verify(messageService, never()).sendToReceivers(any(), any());
    }

    @Test
    @DisplayName("没有超时询盘时不发任何消息")
    void testNoTimeoutInquiry() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 9, 9, 0);
        when(inquiryMapper.selectPendingQuoteList(any())).thenReturn(Collections.emptyList());
        CrmInquiryTimeoutTask task = new CrmInquiryTimeoutTask(inquiryMapper, messageService, messageNotifier, 3);

        assertEquals(0, task.scanTimeout(now));
        verify(messageService, never()).sendToReceivers(any(), any());
    }

    @Test
    @DisplayName("站内信发送失败不影响业务主流程")
    void testMessageFailureDoesNotBreakBusiness() {
        when(messageService.sendToReceivers(any(), any()))
                .thenThrow(new IllegalStateException("message table down"));
        CrmInquiryTimeoutTask task = new CrmInquiryTimeoutTask(inquiryMapper, messageService, messageNotifier, 3);
        when(inquiryMapper.selectPendingQuoteList(any())).thenReturn(Collections.singletonList(inquiry("I1")));

        // 不抛异常 —— 站内信是旁路
        task.scanTimeout(LocalDateTime.of(2026, 11, 9, 9, 0));
        assertTrue(true);
    }

}
