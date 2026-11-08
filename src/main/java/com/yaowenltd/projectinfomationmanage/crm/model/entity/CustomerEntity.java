/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.entity;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * CRM 客户实体，对应表 t_customer.
 *
 * @since 2026-11-08
 */
@Data
public class CustomerEntity {

    /**
     * 主键
     */
    private String id;

    /**
     * 公司全称（可外文）
     */
    private String companyName;

    /**
     * 归一化名称（大写去空格），用于去重
     */
    private String companyNameNorm;

    /**
     * 简称
     */
    private String shortName;

    /**
     * ISO 3166-1 alpha-2 国家码
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
     * 地址（可外文）
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
     * 来源渠道，字典 customer_source
     */
    private String source;

    /**
     * 客户等级 A/B/C
     */
    private String level;

    /**
     * POTENTIAL/FOLLOWING/DEALED/LOST
     */
    private String customerStatus;

    /**
     * 负责人 → t_user.id
     */
    private String ownerId;

    /**
     * 首次成交时间
     */
    private java.time.LocalDateTime firstDealAt;

    /**
     * 最近联系时间
     */
    private java.time.LocalDateTime lastContactAt;

    /**
     * 备注（可外文）
     */
    private String remark;

    /**
     * 逻辑删除：0 未删除；删除时写入毫秒时间戳
     */
    private Long deleted;

}
