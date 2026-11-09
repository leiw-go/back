/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.email;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * CRM 邮件群发任务出参.
 *
 * @since 2026-11-08
 */
@Data
public class CrmEmailCampaignDto {

    /**
     * 群发任务 ID
     */
    private String id;

    /**
     * 群发任务名
     */
    private String name;

    /**
     * 模板 ID
     */
    private String templateId;

    /**
     * 范围
     */
    private String targetType;

    /**
     * 范围值
     */
    private String targetValue;

    /**
     * 状态
     */
    private String campaignStatus;

    /**
     * 目标收件人数
     */
    private Integer totalCount;

    /**
     * 发送成功数
     */
    private Integer successCount;

    /**
     * 发送失败数
     */
    private Integer failCount;

    /**
     * 发送时间
     */
    private LocalDateTime sentAt;

    /**
     * 创建人
     */
    private String ownerId;

}
