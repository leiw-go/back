/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.entity;

import lombok.Data;

/**
 * CRM 国家参考数据实体.
 * <p>
 * 映射表 {@code t_country_ref}：国家 → 主时区 / 主要货币。
 * 汇率不在本表 —— 汇率由汇率同步任务写入 {@code t_currency_rate}（基准币 USD）。
 *
 * @since 2026-11-09
 */
@Data
public class CrmCountryRefEntity {

    /** 主键. */
    private String id;

    /** ISO 3166-1 alpha-2 国家码. */
    private String countryCode;

    /** 英文名. */
    private String countryName;

    /** 中文名. */
    private String countryNameZh;

    /** 常用口语 / 旧称（逗号分隔）. */
    private String countryAliases;

    /** 主时区（IANA 区名）. */
    private String timezone;

    /** 该国全部时区（逗号分隔）. */
    private String timezones;

    /** 主要货币（ISO 4217）. */
    private String currencyCode;

    /** 货币名称. */
    private String currencyName;

    /** 货币符号. */
    private String currencySymbol;

    /** 常用外贸国家标记. */
    private Integer isCommon;

}
