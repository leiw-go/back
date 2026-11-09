/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.message;

import lombok.Data;

/**
 * 站内信列表筛选条件.
 *
 * @since 2026-11-08
 */
@Data
public class MessageQueryRequest {

    /**
     * 页码，从 1 开始
     */
    private Integer page;

    /**
     * 每页条数
     */
    private Integer size;

    /**
     * 是否已读；为空表示不限
     */
    private Boolean read;

    /**
     * 消息类型
     */
    private String messageType;

    /**
     * 级别 NORMAL / IMPORTANT / URGENT
     */
    private String level;

}
