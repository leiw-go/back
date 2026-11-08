/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.message.MessageDraft;

import java.util.List;

/**
 * 站内信服务.
 * <p>按需求文档 §3.11：群发靠 fan-out，一行一个收件人，各自独立已读状态。
 * </p>
 *
 * @since 2026-11-08
 */
public interface MessageService {

    /**
     * 向一组接收人发送系统站内信（fan-out 为多行）.
     *
     * @param draft       消息内容
     * @param receiverIds 接收人 ID 列表
     * @return 实际写入的消息行数
     */
    int sendToReceivers(MessageDraft draft, List<String> receiverIds);

    /**
     * 向一组接收人发送站内信，指定发送人时记为人工消息.
     *
     * @param draft       消息内容
     * @param receiverIds 接收人 ID 列表
     * @param senderId    发送人；为空表示系统消息
     * @return 实际写入的消息行数
     */
    int sendToReceivers(MessageDraft draft, List<String> receiverIds, String senderId);

    /**
     * 标记某条消息为已读（仅限指定收件人）.
     *
     * @param messageId  消息 ID
     * @param receiverId 收件人 ID
     */
    void markRead(String messageId, String receiverId);

    /**
     * 将某收件人的消息全部标记为已读.
     *
     * @param receiverId  收件人 ID
     * @param messageType 消息类型；为空表示不限类型
     * @return 受影响的行数
     */
    int markAllRead(String receiverId, String messageType);

    /**
     * 统计某收件人的未读消息数.
     *
     * @param receiverId 收件人 ID
     * @return 未读消息数
     */
    long countUnread(String receiverId);
}
