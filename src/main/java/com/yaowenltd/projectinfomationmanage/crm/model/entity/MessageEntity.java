/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 站内信持久化实体。
 *
 * @since 2026-11-08
 */
@Data
public class MessageEntity {

    /**
     * 主键
     */
    private String id;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 消息级别
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
     * 发送方类型：SYSTEM / USER
     */
    private String senderType;

    /**
     * 发送人；系统消息为空
     */
    private String senderId;

    /**
     * 收件人
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
     * 是否已读：0 未读 / 1 已读
     */
    private int isRead;

    /**
     * 已读时间
     */
    private java.time.LocalDateTime readAt;

    /**
     * 创建时间
     */
    private java.time.LocalDateTime createTime;

    /**
     * 逻辑删除：0 未删除；删除时写入毫秒时间戳
     */
    private Long deleted;

}
