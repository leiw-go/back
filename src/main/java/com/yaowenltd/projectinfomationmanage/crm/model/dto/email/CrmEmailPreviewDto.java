/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.email;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * 邮件模板预览结果.
 *
 * @since 2026-11-08
 */
@Data
public class CrmEmailPreviewDto {

    /**
     * 渲染后的主题
     */
    private String subject;

    /**
     * 渲染后的正文
     */
    private String content;

    /**
     * 模板 ID
     */
    private String templateId;

    /**
     * 未被替换的占位符
     */
    private List<String> unresolvedPlaceholders = new ArrayList<>();

}
