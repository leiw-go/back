/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.customer;

import lombok.Data;

/**
 * 客户新建 / 更新请求.
 *
 * @since 2026-11-08
 */
@Data
public class CustomerRequest {

    /**
     * 公司全称（必填）
     */
    private String companyName;

    /**
     * 简称
     */
    private String shortName;

    /**
     * 国家码（必填）
     */
    private String countryCode;

    /**
     * 省 / 州
     */
    private String province;

    /**
     * 城市
     */
    private String city;

    /**
     * 地址
     */
    private String address;

    /**
     * 官网
     */
    private String website;

    /**
     * 行业
     */
    private String industry;

    /**
     * 来源渠道（必填）
     */
    private String source;

    /**
     * 客户等级（必填，默认 B）
     */
    private String level;

    /**
     * 客户状态
     */
    private String customerStatus;

    /**
     * 负责人（必填）
     */
    private String ownerId;

    /**
     * 备注
     */
    private String remark;

}
