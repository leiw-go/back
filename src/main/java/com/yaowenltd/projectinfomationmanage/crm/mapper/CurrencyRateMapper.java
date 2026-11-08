/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 汇率数据库操作的 Mapper 接口.
 *
 * @since 2026-11-08
 */
@Mapper
public interface CurrencyRateMapper {

    /**
     * 查询某币种对在指定日期当天或之前最近一条的汇率.
     *
     * @param baseCurrency  基准币
     * @param quoteCurrency 目标币
     * @param date          日期（含当天）
     * @return 汇率；无记录时返回 null
     */
    BigDecimal selectLatestRateOnOrBefore(@Param("baseCurrency") String baseCurrency,
                                           @Param("quoteCurrency") String quoteCurrency,
                                           @Param("date") LocalDate date);

}
