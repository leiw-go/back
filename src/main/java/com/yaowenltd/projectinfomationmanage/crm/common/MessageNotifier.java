/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.common;

import com.yaowenltd.projectinfomationmanage.crm.model.enums.MessageType;
import com.yaowenltd.projectinfomationmanage.crm.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.Map;

/**
 * 业务事件站内信通知器.
 * <p>
 * 需求文档 §3.11：业务事件触发站内信，收件人是该业务单据的负责人。
 * <p>
 * **站内信是旁路**：发送失败只记日志并跳过，**绝不影响主业务事务** ——
 * 消息发不出去不能导致订单创建失败或状态流转回滚。
 *
 * @since 2026-11-08
 */
@Component
public class MessageNotifier {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessageNotifier.class);

    private final MessageService messageService;

    /**
     * 使用指定依赖构造 MessageNotifier.
     *
     * @param messageService 站内信服务
     */
    public MessageNotifier(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * 给单个收件人发一条业务事件站内信.
     *
     * @param type         消息类型
     * @param bizType      关联业务类型
     * @param bizId        关联业务主键
     * @param bizTitle     关联业务标题
     * @param receiverId   收件人；为空表示跳过
     * @param placeholders 文案占位符
     */
    public void notifyOwner(MessageType type, String bizType, String bizId, String bizTitle,
                             String receiverId, Map<String, String> placeholders) {
        if (receiverId == null || receiverId.trim().isEmpty()) {
            LOGGER.warn("站内信跳过：收件人为空, type={}, bizId={}", type, bizId);
            return;
        }
        try {
            messageService.sendToReceivers(
                    MessageTextFactory.draft(type, bizType, bizId, bizTitle, placeholders),
                    Collections.singletonList(receiverId.trim()));
        } catch (Exception e) {
            LOGGER.warn("站内信发送失败：type={}, bizId={}, receiverId={}", type, bizId, receiverId, e);
        }
    }

    /**
     * 给一组收件人发一条业务事件站内信.
     *
     * @param type         消息类型
     * @param bizType      关联业务类型
     * @param bizId        关联业务主键
     * @param bizTitle     关联业务标题
     * @param receiverIds  收件人列表
     * @param placeholders 文案占位符
     */
    public void notifyReceivers(MessageType type, String bizType, String bizId, String bizTitle,
                                 java.util.List<String> receiverIds, Map<String, String> placeholders) {
        if (receiverIds == null || receiverIds.isEmpty()) {
            LOGGER.warn("站内信跳过：收件人列表为空, type={}, bizId={}", type, bizId);
            return;
        }
        try {
            messageService.sendToReceivers(
                    MessageTextFactory.draft(type, bizType, bizId, bizTitle, placeholders),
                    receiverIds);
        } catch (Exception e) {
            LOGGER.warn("站内信发送失败：type={}, bizId={}", type, bizId, e);
        }
    }

}
