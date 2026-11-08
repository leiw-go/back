/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.entity;

import lombok.Data;

/**
 * CRM 客户联系人实体，对应表 t_customer_contact.
 *
 * @since 2026-11-08
 */
@Data
public class CustomerContactEntity {

    /**
     * 主键
     */
    private String id;

    /**
     * 所属客户 → t_customer.id
     */
    private String customerId;

    /**
     * 姓名（可外文）
     */
    private String name;

    /**
     * 职位（可外文）
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
     * 沟通语言标记，不驱动任何行为
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
     * 备注（可外文）
     */
    private String remark;

    /**
     * 逻辑删除：0 未删除；删除时写入毫秒时间戳
     */
    private Long deleted;

}
