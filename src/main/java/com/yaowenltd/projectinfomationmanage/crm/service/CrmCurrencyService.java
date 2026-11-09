/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencyConvertDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencyDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencyRateCheckDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencyRateDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencyRateRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencySyncStatusDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * CRM 汇率服务.
 *
 * @since 2026-11-08
 */
public interface CrmCurrencyService {

    /**
     * 币种列表（含熔断状态）.
     *
     * @return 币种列表
     */
    List<CrmCurrencyDto> listCurrencies();

    /**
     * 汇率列表（只读本地表，不实时调外部接口）.
     *
     * @param baseCurrency  基准币；为空表示不限
     * @param quoteCurrency 目标币；为空表示不限
     * @param date          汇率日期；为空表示不限
     * @param source        来源；为空表示不限
     * @return 汇率列表
     */
    List<CrmCurrencyRateDto> listRates(String baseCurrency, String quoteCurrency, LocalDate date,
                                       String source);

    /**
     * 手工维护汇率（source = MANUAL；同步任务不覆盖 MANUAL 行）.
     *
     * @param request 汇率内容
     * @return 保存后的汇率
     */
    CrmCurrencyRateDto saveManualRate(CrmCurrencyRateRequest request);

    /**
     * 金额折算（交叉汇率走 USD 三角折算）.
     *
     * @param amount 原币金额
     * @param from   源币
     * @param to     目标币
     * @param date   折算日期
     * @return 折算结果（无可用汇率时 rateMissing = true）
     */
    CrmCurrencyConvertDto convert(BigDecimal amount, String from, String to, LocalDate date);

    /**
     * 手动触发汇率同步（拉两源 → 交叉比对 → 入库）.
     *
     * @return 本次同步批次摘要
     */
    CrmCurrencySyncStatusDto sync();

    /**
     * 同步币种字典（名称 / 符号 / 数字码）到 {@code t_currency}.
     *
     * @return 同步到的币种数
     */
    int syncCurrencyDictionary();

    /**
     * 最近一次同步批次摘要.
     *
     * @return 批次摘要；没有则返回 null
     */
    CrmCurrencySyncStatusDto syncStatus();

    /**
     * 交叉比对明细.
     *
     * @param quoteCurrency 目标币；为空表示不限
     * @param checkResult   比对结果；为空表示不限
     * @return 比对明细列表
     */
    List<CrmCurrencyRateCheckDto> verification(String quoteCurrency, String checkResult);

}
