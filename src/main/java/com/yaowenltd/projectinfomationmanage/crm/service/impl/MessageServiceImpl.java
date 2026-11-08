/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.yaowenltd.projectinfomationmanage.crm.mapper.MessageMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.message.MessageDraft;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.MessageEntity;
import com.yaowenltd.projectinfomationmanage.crm.service.MessageService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 站内信服务实现.
 * <p>按需求文档 §3.11 与 §5.3.15：群发靠 fan-out，一行一个收件人，各自独立已读状态；
 * 系统消息没有发送人，人工消息的发送人是当前用户。
 * </p>
 *
 * @since 2026-11-08
 */
@Service
public class MessageServiceImpl implements MessageService {

    /** 发送方类型：系统. */
    private static final String SENDER_TYPE_SYSTEM = "SYSTEM";

    /** 发送方类型：用户. */
    private static final String SENDER_TYPE_USER = "USER";

    private final MessageMapper messageMapper;

    /**
     * 使用指定的 Mapper 构造 MessageServiceImpl.
     *
     * @param messageMapper 站内信 Mapper
     */
    public MessageServiceImpl(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    @Override
    public int sendToReceivers(MessageDraft draft, List<String> receiverIds) {
        return sendToReceivers(draft, receiverIds, null);
    }

    @Override
    public int sendToReceivers(MessageDraft draft, List<String> receiverIds, String senderId) {
        if (draft == null) {
            throw new IllegalArgumentException("message draft must not be null");
        }
        if (receiverIds == null || receiverIds.isEmpty()) {
            throw new IllegalArgumentException("message must have at least one receiver");
        }
        boolean manual = senderId != null && !senderId.trim().isEmpty();

        List<MessageEntity> rows = new ArrayList<>(receiverIds.size());
        for (String receiverId : receiverIds) {
            if (receiverId == null || receiverId.trim().isEmpty()) {
                throw new IllegalArgumentException("message receiver must not be blank");
            }
            MessageEntity entity = new MessageEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setMessageType(draft.getMessageType());
            entity.setLevel(draft.getLevel());
            entity.setTitle(draft.getTitle());
            entity.setContent(draft.getContent());
            entity.setSenderType(manual ? SENDER_TYPE_USER : SENDER_TYPE_SYSTEM);
            entity.setSenderId(manual ? senderId : null);
            entity.setReceiverId(receiverId);
            entity.setBizType(draft.getBizType());
            entity.setBizId(draft.getBizId());
            entity.setBizTitle(draft.getBizTitle());
            entity.setIsRead(0);
            entity.setReadAt(null);
            rows.add(entity);
        }
        messageMapper.insertBatch(rows);
        return rows.size();
    }

    @Override
    public void markRead(String messageId, String receiverId) {
        if (messageId == null || receiverId == null) {
            throw new IllegalArgumentException("messageId and receiverId must not be null");
        }
        messageMapper.markRead(messageId, receiverId);
    }

    @Override
    public int markAllRead(String receiverId, String messageType) {
        if (receiverId == null) {
            throw new IllegalArgumentException("receiverId must not be null");
        }
        return messageMapper.markAllRead(receiverId, messageType);
    }

    @Override
    public long countUnread(String receiverId) {
        if (receiverId == null) {
            throw new IllegalArgumentException("receiverId must not be null");
        }
        return messageMapper.countUnread(receiverId);
    }
}
