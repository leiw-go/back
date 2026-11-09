/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.email;

import lombok.Data;

/**
 * CRM 邮件模板入参（新建 / 更新）.
 *
 * @since 2026-11-08
 */
@Data
public class CrmEmailTemplateRequest {

    /**
     * 模板 ID；为空表示新建
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
     * 语言分类；为空时默认 zh
     */
    private String language;

    /**
     * 类型
     */
    private String templateType;

}
