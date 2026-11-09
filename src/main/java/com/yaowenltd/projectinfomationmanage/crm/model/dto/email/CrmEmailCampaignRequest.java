/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.email;

import lombok.Data;

/**
 * CRM 邮件群发任务入参.
 *
 * @since 2026-11-08
 */
@Data
public class CrmEmailCampaignRequest {

    /**
     * 群发任务名
     */
    private String name;

    /**
     * 模板 ID
     */
    private String templateId;

    /**
     * 范围 TAG / OWNER / CUSTOMER
     */
    private String targetType;

    /**
     * 范围值：标签 ID / 负责人 ID / 客户 ID
     */
    private String targetValue;

}
