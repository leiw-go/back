/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.message;

import lombok.Data;

/**
 * 站内信待发送内容。
 *
 * @since 2026-11-08
 */
@Data
public class MessageDraft {

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

}
