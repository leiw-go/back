/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.yaowenltd.projectinfomationmanage.crm.config.CrmCurrencyProperties;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CurrencyQuote;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.PlausibilityOutcome;
import com.yaowenltd.projectinfomationmanage.crm.service.CurrencyPlausibilityService;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 汇率合理性校验实现.
 * <p>
 * 对应汇率专项文档 §4.2 的第 1~5 项：
 * 值域、正数、日期不超前、日期不太旧、环比突变。
 * 环比突变是"抓单源集体错误"的主力 —— 两个源一起错时交叉比对可能集体一致，
 * 但环比会炸；两道闸门互补。
 *
 * @since 2026-11-08
 */
@Service
public class CurrencyPlausibilityServiceImpl implements CurrencyPlausibilityService {

    /** 环比百分比的中间精度. */
    private static final int CALC_SCALE = 16;

    private final CrmCurrencyProperties properties;

    /**
     * 使用指定依赖构造 CurrencyPlausibilityServiceImpl.
     *
     * @param properties 汇率配置
     */
    public CurrencyPlausibilityServiceImpl(CrmCurrencyProperties properties) {
        this.properties = properties;
    }

    @Override
    public PlausibilityOutcome check(CurrencyQuote quote, BigDecimal previousRate) {
        if (quote == null || quote.getRate() == null) {
            return PlausibilityOutcome.fail("RATE_MISSING");
        }
        BigDecimal rate = quote.getRate();
        if (rate.signum() <= 0) {
            return PlausibilityOutcome.fail("RATE_NOT_POSITIVE");
        }
        BigDecimal min = properties.getPlausibility().getMinRate();
        BigDecimal max = properties.getPlausibility().getMaxRate();
        if (rate.compareTo(min) < 0 || rate.compareTo(max) > 0) {
            return PlausibilityOutcome.fail("RATE_OUT_OF_RANGE");
        }

        LocalDate today = LocalDate.now();
        LocalDate rateDate = quote.getRateDate();
        if (rateDate == null) {
            return PlausibilityOutcome.fail("RATE_DATE_MISSING");
        }
        if (rateDate.isAfter(today)) {
            return PlausibilityOutcome.fail("DATE_IN_FUTURE");
        }
        int staleDays = (int) ChronoUnit.DAYS.between(rateDate, today);
        if (staleDays > properties.getPlausibility().getMaxStaleDays()) {
            return PlausibilityOutcome.fail("DATE_TOO_OLD");
        }

        if (previousRate != null && previousRate.signum() > 0) {
            BigDecimal change = rate.subtract(previousRate).abs()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(previousRate, CALC_SCALE, RoundingMode.HALF_UP);
            if (change.compareTo(properties.getPlausibility().getMaxDailyChange()) > 0) {
                return PlausibilityOutcome.warn("DAILY_CHANGE_TOO_LARGE",
                        change.setScale(6, RoundingMode.HALF_UP));
            }
        }
        return PlausibilityOutcome.pass();
    }

}
