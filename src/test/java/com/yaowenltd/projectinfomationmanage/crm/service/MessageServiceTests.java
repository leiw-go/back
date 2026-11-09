/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.mapper.MessageMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.message.MessageDraft;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.MessageEntity;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.MessageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MessageService} 的单元测试。
 * <p>
 * 契约来自需求文档 §3.11 与 §5.3.15：
 * 群发靠 fan-out，一行一个收件人，各自独立已读状态；
 * 系统消息无发送人；未读数与标记已读走 Mapper。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class MessageServiceTests {

    @Mock
    private MessageMapper messageMapper;

    private MessageService messageService;

    @BeforeEach
    void setUp() {
        messageService = new MessageServiceImpl(messageMapper);
    }

    private static MessageDraft draft() {
        MessageDraft draft = new MessageDraft();
        draft.setMessageType("QUOTATION_REJECTED");
        draft.setLevel("IMPORTANT");
        draft.setTitle("报价被拒绝");
        draft.setContent("客户 {customerName} 拒绝了报价单 {quotationNo}");
        draft.setBizType("QUOTATION");
        draft.setBizId("Q-1");
        draft.setBizTitle("报价单 Q-1");
        return draft;
    }

    @Test
    @DisplayName("群发应按接收人 fan-out 成多行")
    void testSendToReceiversFansOut() {
        when(messageMapper.insertBatch(any())).thenReturn(3);

        int inserted = messageService.sendToReceivers(draft(), Arrays.asList("U1", "U2", "U3"));

        assertEquals(3, inserted);
    }

    @Test
    @DisplayName("fan-out 的每一行对应一个收件人且默认未读")
    void testFanOutProducesOneRowPerReceiver() {
        ArgumentCaptor<List<MessageEntity>> captor = ArgumentCaptor.forClass(List.class);
        when(messageMapper.insertBatch(any())).thenReturn(2);

        messageService.sendToReceivers(draft(), Arrays.asList("U1", "U2"));

        verify(messageMapper).insertBatch(captor.capture());
        List<MessageEntity> rows = captor.getValue();
        assertEquals(2, rows.size());
        assertEquals("U1", rows.get(0).getReceiverId());
        assertEquals("U2", rows.get(1).getReceiverId());
        assertEquals(0, rows.get(0).getIsRead(), "a brand new message must be unread");
        assertEquals(0, rows.get(1).getIsRead());
    }

    @Test
    @DisplayName("系统消息没有发送人 级别与类型来自草稿")
    void testSystemMessageHasNoSender() {
        ArgumentCaptor<List<MessageEntity>> captor = ArgumentCaptor.forClass(List.class);
        when(messageMapper.insertBatch(any())).thenReturn(1);

        messageService.sendToReceivers(draft(), Collections.singletonList("U1"));

        verify(messageMapper).insertBatch(captor.capture());
        MessageEntity row = captor.getValue().get(0);
        assertEquals("SYSTEM", row.getSenderType());
        assertNull(row.getSenderId());
        assertEquals("QUOTATION_REJECTED", row.getMessageType());
        assertEquals("IMPORTANT", row.getLevel());
        assertEquals("QUOTATION", row.getBizType());
        assertEquals("Q-1", row.getBizId());
    }

    @Test
    @DisplayName("接收人为空应被拒绝")
    void testEmptyReceiversIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> messageService.sendToReceivers(draft(), Collections.emptyList()));
        assertThrows(IllegalArgumentException.class,
                () -> messageService.sendToReceivers(draft(), null));
    }

    @Test
    @DisplayName("草稿为空应被拒绝")
    void testNullDraftIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> messageService.sendToReceivers(null, Collections.singletonList("U1")));
    }

    @Test
    @DisplayName("标记已读应带上收件人限定")
    void testMarkReadIsScopedToReceiver() {
        when(messageMapper.markRead("M1", "U1")).thenReturn(1);

        messageService.markRead("M1", "U1");

        verify(messageMapper).markRead("M1", "U1");
    }

    @Test
    @DisplayName("全部已读应支持按类型限定")
    void testMarkAllReadSupportsTypeFilter() {
        when(messageMapper.markAllRead("U1", "RECEIVABLE_OVERDUE")).thenReturn(4);

        int affected = messageService.markAllRead("U1", "RECEIVABLE_OVERDUE");

        assertEquals(4, affected);
        verify(messageMapper).markAllRead("U1", "RECEIVABLE_OVERDUE");
    }

    @Test
    @DisplayName("全部已读不传类型时表示不限类型")
    void testMarkAllReadWithoutTypeFilter() {
        when(messageMapper.markAllRead(eq("U1"), any())).thenReturn(9);

        int affected = messageService.markAllRead("U1", null);

        assertEquals(9, affected);
        verify(messageMapper).markAllRead("U1", null);
    }

    @Test
    @DisplayName("未读数应透传 Mapper 结果")
    void testCountUnreadDelegatesToMapper() {
        when(messageMapper.countUnread("U1")).thenReturn(7L);

        long count = messageService.countUnread("U1");

        assertEquals(7L, count);
        verify(messageMapper).countUnread("U1");
    }

    @Test
    @DisplayName("未读数查询不应修改任何数据")
    void testCountUnreadIsReadOnly() {
        when(messageMapper.countUnread("U1")).thenReturn(0L);

        messageService.countUnread("U1");

        verify(messageMapper, org.mockito.Mockito.never()).insertBatch(any());
        assertTrue(true);
    }

    @Test
    @DisplayName("判断某业务是否已发过系统消息")
    void testExistsSystemMessage() {
        when(messageMapper.countByBiz("ORDER", "O1", "ORDER_SHIPPED")).thenReturn(1);

        assertTrue(messageService.existsSystemMessage("ORDER", "O1", "ORDER_SHIPPED"));
    }

    @Test
    @DisplayName("参数不全时判定为未发过")
    void testExistsSystemMessageWithBlankArguments() {
        assertFalse(messageService.existsSystemMessage(null, "O1", "ORDER_SHIPPED"));
        assertFalse(messageService.existsSystemMessage("ORDER", " ", "ORDER_SHIPPED"));
        assertFalse(messageService.existsSystemMessage("ORDER", "O1", ""));
    }

}
