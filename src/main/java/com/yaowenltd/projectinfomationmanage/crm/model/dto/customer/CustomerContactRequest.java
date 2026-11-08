/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.customer;

import lombok.Data;

/**
 * 客户联系人新建 / 更新请求.
 *
 * @since 2026-11-08
 */
@Data
public class CustomerContactRequest {

    /**
     * 姓名（必填）
     */
    private String name;

    /**
     * 职位
     */
    private String position;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 电话（E.164）
     */
    private String phone;

    /**
     * WhatsApp（E.164）
     */
    private String whatsapp;

    /**
     * 沟通语言标记
     */
    private String language;

    /**
     * 时区
     */
    private String timezone;

    /**
     * 1 = 主要联系人
     */
    private int isPrimary;

    /**
     * 备注
     */
    private String remark;

}
