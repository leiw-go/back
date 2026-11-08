/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.customer;

import lombok.Data;

/**
 * 客户联系人响应对象.
 *
 * @since 2026-11-08
 */
@Data
public class CustomerContactDto {

    /**
     * 主键
     */
    private String id;

    /**
     * 所属客户
     */
    private String customerId;

    /**
     * 姓名
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
     * 电话
     */
    private String phone;

    /**
     * WhatsApp
     */
    private String whatsapp;

    /**
     * 沟通语言
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
