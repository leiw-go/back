/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yaowenltd.projectinfomationmanage.crm.mapper.MessageMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.message.MessageDraft;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.message.MessageDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.message.MessageQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.MessageEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.MessageType;
import com.yaowenltd.projectinfomationmanage.crm.service.MessageService;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    /** 列表默认每页条数. */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 列表最大每页条数. */
    private static final int MAX_PAGE_SIZE = 200;

    /** 单次查询允许的最大偏移量. */
    private static final int MAX_PAGE_OFFSET = 10000;

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

    @Override
    public PageResponse<MessageDto> list(String receiverId, MessageQueryRequest query) {
        if (receiverId == null || receiverId.trim().isEmpty()) {
            throw new IllegalArgumentException("receiverId must not be blank");
        }
        MessageQueryRequest actual = query == null ? new MessageQueryRequest() : query;
        int page = actual.getPage() == null || actual.getPage() < 1 ? 1 : actual.getPage();
        int size = actual.getSize() == null || actual.getSize() < 1
                ? DEFAULT_PAGE_SIZE : Math.min(actual.getSize(), MAX_PAGE_SIZE);
        if ((long) page * size > MAX_PAGE_OFFSET) {
            throw new IllegalArgumentException("page * size must not exceed " + MAX_PAGE_OFFSET);
        }
        PageHelper.startPage(page, size);
        try {
            PageInfo<MessageEntity> info =
                    new PageInfo<>(messageMapper.selectPage(receiverId, actual));
            List<MessageDto> rows = new ArrayList<>(info.getList().size());
            for (MessageEntity entity : info.getList()) {
                rows.add(toDto(entity));
            }
            PageResponse<MessageDto> response = new PageResponse<>();
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
    public MessageDto getById(String messageId, String receiverId) {
        if (messageId == null || messageId.trim().isEmpty()) {
            throw new IllegalArgumentException("messageId must not be blank");
        }
        MessageEntity entity = messageMapper.selectById(messageId);
        if (entity == null || !receiverId.equals(entity.getReceiverId())) {
            throw new IllegalArgumentException("message not found: " + messageId);
        }
        if (entity.getIsRead() == 0) {
            messageMapper.markRead(messageId, receiverId);
            entity.setIsRead(1);
        }
        return toDto(entity);
    }

    @Override
    public void delete(String messageId, String receiverId) {
        if (messageId == null || messageId.trim().isEmpty()) {
            throw new IllegalArgumentException("messageId must not be blank");
        }
        messageMapper.softDelete(messageId, receiverId, System.currentTimeMillis());
    }

    @Override
    public int sendManual(String receiverId, String title, String content, String senderId) {
        if (receiverId == null || receiverId.trim().isEmpty()) {
            throw new IllegalArgumentException("receiverId must not be blank");
        }
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        MessageDraft draft = new MessageDraft();
        draft.setMessageType(MessageType.MANUAL.name());
        draft.setLevel(MessageType.MANUAL.getDefaultLevel().name());
        draft.setTitle(title.trim());
        draft.setContent(content);
        return sendToReceivers(draft, java.util.Collections.singletonList(receiverId.trim()), senderId);
    }

    @Override
    public int cleanupReadMessages(int retentionDays) {
        if (retentionDays < 1) {
            throw new IllegalArgumentException("retentionDays must be positive");
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        return messageMapper.deleteReadBefore(cutoff, System.currentTimeMillis());
    }

    @Override
    public boolean existsSystemMessage(String bizType, String bizId, String messageType) {
        if (bizType == null || bizType.trim().isEmpty()
                || bizId == null || bizId.trim().isEmpty()
                || messageType == null || messageType.trim().isEmpty()) {
            return false;
        }
        return messageMapper.countByBiz(bizType.trim(), bizId.trim(), messageType.trim()) > 0;
    }

    private MessageDto toDto(MessageEntity entity) {
        MessageDto dto = new MessageDto();
        dto.setId(entity.getId());
        dto.setMessageType(entity.getMessageType());
        dto.setLevel(entity.getLevel());
        dto.setTitle(entity.getTitle());
        dto.setContent(entity.getContent());
        dto.setSenderType(entity.getSenderType());
        dto.setSenderId(entity.getSenderId());
        dto.setReceiverId(entity.getReceiverId());
        dto.setBizType(entity.getBizType());
        dto.setBizId(entity.getBizId());
        dto.setBizTitle(entity.getBizTitle());
        dto.setRead(entity.getIsRead() == 1);
        dto.setReadAt(entity.getReadAt());
        dto.setCreateTime(entity.getCreateTime());
        return dto;
    }
}
