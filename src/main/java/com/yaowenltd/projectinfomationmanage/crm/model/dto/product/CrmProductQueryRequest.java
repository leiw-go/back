/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.product;

import lombok.Data;

/**
 * CRM 产品列表筛选条件.
 *
 * @since 2026-11-08
 */
@Data
public class CrmProductQueryRequest {

    /**
     * 页码，从 1 开始
     */
    private Integer page;

    /**
     * 每页条数
     */
    private Integer size;

    /**
     * 分类
     */
    private String category;

    /**
     * 状态：1 在售 / 0 停售
     */
    private Integer productStatus;

    /**
     * 关键字（匹配编码或名称）
     */
    private String keyword;

}
