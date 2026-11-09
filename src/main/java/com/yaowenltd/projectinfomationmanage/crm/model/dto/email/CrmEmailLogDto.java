/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.email;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * CRM 邮件发送记录出参.
 *
 * @since 2026-11-08
 */
@Data
public class CrmEmailLogDto {

    /**
     * 记录 ID
     */
    private String id;

    /**
     * 群发任务 ID
     */
    private String campaignId;

    /**
     * 联系人 ID
     */
    private String contactId;

    /**
     * 收件邮箱
     */
    private String email;

    /**
     * 发送状态
     */
    private String sendStatus;

    /**
     * 失败原因
     */
    private String errorMsg;

    /**
     * 发送时间
     */
    private LocalDateTime sentAt;

}
