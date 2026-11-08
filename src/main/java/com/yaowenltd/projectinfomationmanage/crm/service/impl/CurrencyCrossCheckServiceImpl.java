/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrossCheckOutcome;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrossCheckRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CurrencyQuote;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.CurrencyCheckResult;
import com.yaowenltd.projectinfomationmanage.crm.service.CurrencyCrossCheckService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 两源汇率交叉比对与熔断判定实现.
 * <p>按汇率专项文档 §4：偏差 = |主源 - 比对源| / ((主源 + 比对源) / 2) × 100%；
 * 偏差不超过阈值判 MATCH，超过阈值判 REJECT；
 * 两源数据日期差超过上限时跳过比对判 SKIP；
 * 连续超标达到阈值天数触发熔断，连续达标达到阈值天数解除熔断。
 * </p>
 *
 * @since 2026-11-08
 */
@Service
public class CurrencyCrossCheckServiceImpl implements CurrencyCrossCheckService {

    /** 偏差百分比的小数位. */
    private static final int DEVIATION_SCALE = 6;

    @Override
    public CrossCheckOutcome check(CrossCheckRequest request) {
        CrossCheckOutcome outcome = new CrossCheckOutcome();
        outcome.setResult(CurrencyCheckResult.SKIP);
        outcome.setDeviation(BigDecimal.ZERO);
        outcome.setDateGapDays(0);

        if (request == null) {
            outcome.setReason("REQUEST_MISSING");
            return outcome;
        }
        CurrencyQuote primary = request.getPrimary();
        CurrencyQuote secondary = request.getSecondary();
        if (primary == null || secondary == null) {
            outcome.setReason("NOT_IN_SECONDARY");
            return outcome;
        }

        int dateGapDays = dateGapDays(primary.getRateDate(), secondary.getRateDate());
        outcome.setDateGapDays(dateGapDays);

        int maxGapDays = request.getMaxDateGapDays();
        if (maxGapDays > 0 && dateGapDays > maxGapDays) {
            outcome.setReason("DATE_GAP_EXCEEDED");
            outcome.setAppliedThreshold(request.getThreshold());
            return outcome;
        }

        BigDecimal deviation = deviation(primary.getRate(), secondary.getRate());
        outcome.setDeviation(deviation);
        outcome.setAppliedThreshold(request.getThreshold());

        if (request.getThreshold() == null) {
            outcome.setResult(CurrencyCheckResult.SKIP);
            outcome.setReason("THRESHOLD_MISSING");
            return outcome;
        }
        if (deviation.compareTo(request.getThreshold()) <= 0) {
            outcome.setResult(CurrencyCheckResult.MATCH);
            outcome.setReason("WITHIN_THRESHOLD");
        } else {
            outcome.setResult(CurrencyCheckResult.REJECT);
            outcome.setReason("DEVIATION_EXCEEDED");
        }
        return outcome;
    }

    @Override
    public boolean shouldSuspend(int consecutiveBreachDays, int suspectDays) {
        return suspectDays > 0 && consecutiveBreachDays >= suspectDays;
    }

    @Override
    public boolean shouldRecover(int consecutiveOkDays, int recoverDays) {
        return recoverDays > 0 && consecutiveOkDays >= recoverDays;
    }

    private int dateGapDays(LocalDate primaryDate, LocalDate secondaryDate) {
        if (primaryDate == null || secondaryDate == null) {
            return 0;
        }
        return (int) Math.abs(ChronoUnit.DAYS.between(primaryDate, secondaryDate));
    }

    private BigDecimal deviation(BigDecimal primaryRate, BigDecimal secondaryRate) {
        if (primaryRate == null || secondaryRate == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal difference = primaryRate.subtract(secondaryRate).abs();
        BigDecimal average = primaryRate.add(secondaryRate).divide(BigDecimal.valueOf(2), 16, RoundingMode.HALF_UP);
        if (average.signum() == 0) {
            return BigDecimal.ZERO;
        }
        return difference.multiply(BigDecimal.valueOf(100))
                .divide(average, DEVIATION_SCALE, RoundingMode.HALF_UP);
    }
}
