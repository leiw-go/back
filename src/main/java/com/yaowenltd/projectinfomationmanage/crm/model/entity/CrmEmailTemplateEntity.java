/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.entity;

import java.util.UUID;
import lombok.Data;

/**
 * CRM 邮件模板实体，对应表 t_email_template.
 *
 * @since 2026-11-08
 */
@Data
public class CrmEmailTemplateEntity {

    /**
     * 主键
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
     * 正文，变量占位 {{customer.company_name}}
     */
    private String content;

    /**
     * 语言分类（人工标签）
     */
    private String language;

    /**
     * 类型
     */
    private String templateType;

    /**
     * 逻辑删除：0 未删除；删除时写入毫秒时间戳
     */
    private Long deleted;
}
