/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmCurrencyRateEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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

    /**
     * 查询某币种对在指定日期的汇率.
     *
     * @param baseCurrency  基准币
     * @param quoteCurrency 目标币
     * @param date          日期
     * @return 汇率记录；不存在返回 null
     */
    CrmCurrencyRateEntity selectByPairDate(@Param("baseCurrency") String baseCurrency,
                                            @Param("quoteCurrency") String quoteCurrency,
                                            @Param("date") LocalDate date);

    /**
     * 查询某币种对最新的一条汇率.
     *
     * @param baseCurrency  基准币
     * @param quoteCurrency 目标币
     * @return 汇率记录；不存在返回 null
     */
    CrmCurrencyRateEntity selectLatest(@Param("baseCurrency") String baseCurrency,
                                        @Param("quoteCurrency") String quoteCurrency);

    /**
     * 查询某币种对在指定日期**之前**最近一条的汇率（环比突变校验用）.
     *
     * @param baseCurrency  基准币
     * @param quoteCurrency 目标币
     * @param date          日期（不含当天）
     * @return 汇率记录；不存在返回 null
     */
    CrmCurrencyRateEntity selectPrevious(@Param("baseCurrency") String baseCurrency,
                                          @Param("quoteCurrency") String quoteCurrency,
                                          @Param("date") LocalDate date);

    /**
     * 查询汇率列表（只读本地表）.
     *
     * @param baseCurrency  基准币；为空表示不限
     * @param quoteCurrency 目标币；为空表示不限
     * @param date          汇率日期；为空表示不限
     * @param source        来源；为空表示不限
     * @return 汇率列表
     */
    List<CrmCurrencyRateEntity> selectRates(@Param("baseCurrency") String baseCurrency,
                                             @Param("quoteCurrency") String quoteCurrency,
                                             @Param("date") LocalDate date,
                                             @Param("source") String source);

    /**
     * 写入一条汇率.
     *
     * @param rate 汇率实体
     * @return 受影响的行数
     */
    int insert(CrmCurrencyRateEntity rate);

}
