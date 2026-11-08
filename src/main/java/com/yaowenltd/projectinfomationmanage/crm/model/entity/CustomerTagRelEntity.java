/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.entity;

import lombok.Data;

/**
 * CRM 客户-标签关联实体，对应表 t_customer_tag_rel.
 *
 * @since 2026-11-08
 */
@Data
public class CustomerTagRelEntity {

    /**
     * 主键
     */
    private String id;

    /**
     * 客户 → t_customer.id
     */
    private String customerId;

    /**
     * 标签 → t_customer_tag.id
     */
    private String tagId;

}
