/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.message;

import lombok.Data;

/**
 * 人工站内信入参.
 *
 * @since 2026-11-08
 */
@Data
public class MessageSendRequest {

    /**
     * 收件人 ID
     */
    private String receiverId;

    /**
     * 标题
     */
    private String title;

    /**
     * 正文
     */
    private String content;

}
