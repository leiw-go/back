/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.customer;

import lombok.Data;

/**
 * 客户列表 / 导出筛选条件.
 *
 * @since 2026-11-08
 */
@Data
public class CustomerQueryRequest {

    /**
     * 公司名关键字
     */
    private String companyName;

    /**
     * 国家码
     */
    private String countryCode;

    /**
     * 客户等级
     */
    private String level;

    /**
     * 来源渠道
     */
    private String source;

    /**
     * 负责人
     */
    private String ownerId;

    /**
     * 页码，从 1 起
     */
    private Integer page;

    /**
     * 每页条数，默认 20，最大 200
     */
    private Integer size;

}
