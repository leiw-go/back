/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.country;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 国家信息（详情）.
 * <p>
 * 输入国家，返回该国的**时区、当前时间、主要货币、对美元与人民币的汇率**。
 *
 * @since 2026-11-09
 */
@Data
public class CrmCountryInfoDto {

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

    /** 该国当前时间（ISO-8601，含时区偏移）. */
    private String currentTime;

    /** 该国当前时间（可读格式）. */
    private String currentTimeText;

    /** 当前 UTC 偏移（如 {@code -05:00}）. */
    private String utcOffset;

    /** 与北京时间的时差（小时，北京为 0；正数表示比北京早）. */
    private BigDecimal hoursFromBeijing;

    /** 与北京时间的时差（可读文本）. */
    private String beijingOffsetText;

    /** 主要货币（ISO 4217）. */
    private String currencyCode;

    /** 货币名称. */
    private String currencyName;

    /** 货币符号. */
    private String currencySymbol;

    /** 汇率日期（库中最近一次成功汇率的日期）. */
    private String rateDate;

    /** 1 本币 = ? 美元. */
    private BigDecimal rateToUsd;

    /** 1 本币 = ? 人民币. */
    private BigDecimal rateToCny;

    /** 1 美元 = ? 本币. */
    private BigDecimal rateFromUsd;

    /** 1 人民币 = ? 本币. */
    private BigDecimal rateFromCny;

    /** 汇率是否缺失（该币种还没有同步到本地汇率表）. */
    private boolean rateMissing;

}
