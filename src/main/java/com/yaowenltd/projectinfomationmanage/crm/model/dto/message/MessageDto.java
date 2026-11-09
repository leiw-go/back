/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.message;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 站内信出参.
 *
 * @since 2026-11-08
 */
@Data
public class MessageDto {

    /**
     * 消息 ID
     */
    private String id;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 级别 NORMAL / IMPORTANT / URGENT
     */
    private String level;

    /**
     * 标题
     */
    private String title;

    /**
     * 正文
     */
    private String content;

    /**
     * 发送方类型 SYSTEM / USER
     */
    private String senderType;

    /**
     * 发送人；系统消息为空
     */
    private String senderId;

    /**
     * 收件人 ID
     */
    private String receiverId;

    /**
     * 关联业务类型
     */
    private String bizType;

    /**
     * 关联业务主键
     */
    private String bizId;

    /**
     * 关联业务标题
     */
    private String bizTitle;

    /**
     * 是否已读
     */
    private boolean read;

    /**
     * 已读时间
     */
    private LocalDateTime readAt;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
