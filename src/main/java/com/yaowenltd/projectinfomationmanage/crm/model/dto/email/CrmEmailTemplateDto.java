/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.email;

import lombok.Data;

/**
 * CRM 邮件模板出参.
 *
 * @since 2026-11-08
 */
@Data
public class CrmEmailTemplateDto {

    /**
     * 模板 ID
     */
    private String id;

    /**
     * 模板名
     */
    private String name;

    /**
     * 主题
     */
    private String subject;

    /**
     * 正文
     */
    private String content;

    /**
     * 语言分类
     */
    private String language;

    /**
     * 类型
     */
    private String templateType;

}
