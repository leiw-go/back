/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.message.MessageDraft;
import com.yaowenltd.projectinfomationmanage.crm.mapper.MessageMapper;
import java.util.List;
import com.yaowenltd.projectinfomationmanage.crm.service.MessageService;
import org.springframework.stereotype.Service;

/**
 * 站内信服务实现.
 *
 * @since 2026-11-08
 */
@Service
public class MessageServiceImpl implements MessageService {

    /**
     * 站内信 Mapper.
     */
    private final MessageMapper messageMapper;
    /**
     * 构造 MessageServiceImpl.
     *
     * @param messageMapper 站内信 Mapper
     */
    public MessageServiceImpl(MessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    /**
     * 向一组接收人发送站内信（fan-out 为多行）.
     */
    @Override
    public int sendToReceivers(MessageDraft draft, List<String> receiverIds) {
        throw new UnsupportedOperationException("TDD: 待实现");
    }

    /**
     * 标记某条消息为已读（仅限指定收件人）.
     */
    @Override
    public void markRead(String messageId, String receiverId) {
        throw new UnsupportedOperationException("TDD: 待实现");
    }

    /**
     * 将某收件人的消息全部标记为已读.
     */
    @Override
    public int markAllRead(String receiverId, String messageType) {
        throw new UnsupportedOperationException("TDD: 待实现");
    }

    /**
     * 统计某收件人的未读消息数.
     */
    @Override
    public long countUnread(String receiverId) {
        throw new UnsupportedOperationException("TDD: 待实现");
    }
}
