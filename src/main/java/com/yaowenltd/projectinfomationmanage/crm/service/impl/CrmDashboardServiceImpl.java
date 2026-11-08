/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmDashboardMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardAggregateDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardCustomerSourceDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardPeriodAggregateDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardSummaryDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardTrendDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityPipelineDto;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmDashboardService;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmOpportunityService;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CRM 经营看板服务实现.
 * <p>
 * 本期（M2）看板只接已经落地的客户域与商机域：成交额与逾期应收在订单域（M4）
 * 落地前恒为 0，趋势里的赢单率按"当期新增商机的当前阶段"统计（同期群口径）。
 * </p>
 *
 * @since 2026-11-08
 */
@Service
public class CrmDashboardServiceImpl implements CrmDashboardService {

    /** 按月统计的粒度码. */
    public static final String GRANULARITY_MONTH = "MONTH";

    /** 按季统计的粒度码. */
    public static final String GRANULARITY_QUARTER = "QUARTER";

    /** 趋势默认返回的时间桶个数. */
    private static final int DEFAULT_PERIODS = 6;

    /** 趋势最多返回的时间桶个数. */
    private static final int MAX_PERIODS = 24;

    /** 百分比保留的小数位. */
    private static final int RATE_SCALE = 2;

    private final CrmDashboardMapper dashboardMapper;
    private final CrmOpportunityService crmOpportunityService;

    /**
     * 使用指定依赖构造 CrmDashboardServiceImpl.
     *
     * @param dashboardMapper       看板聚合 Mapper
     * @param crmOpportunityService 商机服务（复用漏斗统计）
     */
    public CrmDashboardServiceImpl(CrmDashboardMapper dashboardMapper,
                                   CrmOpportunityService crmOpportunityService) {
        this.dashboardMapper = dashboardMapper;
        this.crmOpportunityService = crmOpportunityService;
    }

    @Override
    public CrmDashboardSummaryDto summary() {
        CrmDashboardSummaryDto summary = new CrmDashboardSummaryDto();
        summary.setActiveCustomerCount(dashboardMapper.countActiveCustomers());
        CrmDashboardAggregateDto open = dashboardMapper.sumOpenOpportunities();
        if (open != null) {
            summary.setOpenOpportunityCount(open.getCount());
            if (open.getAmountBase() != null) {
                summary.setOpenOpportunityAmountBase(open.getAmountBase());
            }
        }
        return summary;
    }

    @Override
    public CrmOpportunityPipelineDto pipeline() {
        return crmOpportunityService.pipeline();
    }

    @Override
    public List<CrmDashboardTrendDto> trend(String granularity, Integer periods) {
        String actualGranularity = normalizeGranularity(granularity);
        int actualPeriods = periods == null || periods < 1
                ? DEFAULT_PERIODS : Math.min(periods, MAX_PERIODS);

        List<String> keys = buildPeriodKeys(actualGranularity, actualPeriods);
        Map<String, CrmDashboardPeriodAggregateDto> customers = toMap(
                dashboardMapper.countNewCustomersByPeriod(actualGranularity));
        Map<String, CrmDashboardPeriodAggregateDto> opportunities = toMap(
                dashboardMapper.countNewOpportunitiesByPeriod(actualGranularity));

        List<CrmDashboardTrendDto> rows = new ArrayList<>(keys.size());
        for (String key : keys) {
            CrmDashboardTrendDto row = new CrmDashboardTrendDto();
            row.setPeriod(key);

            CrmDashboardPeriodAggregateDto customer = customers.get(key);
            if (customer != null) {
                row.setNewCustomerCount(customer.getCount());
            }
            CrmDashboardPeriodAggregateDto opportunity = opportunities.get(key);
            if (opportunity != null) {
                row.setNewOpportunityCount(opportunity.getCount());
                if (opportunity.getAmountBase() != null) {
                    row.setNewOpportunityAmountBase(opportunity.getAmountBase());
                }
                row.setWinRate(opportunity.getDecided() <= 0 ? BigDecimal.ZERO
                        : BigDecimal.valueOf(opportunity.getWon())
                                .multiply(BigDecimal.valueOf(100))
                                .divide(BigDecimal.valueOf(opportunity.getDecided()), RATE_SCALE,
                                        RoundingMode.HALF_UP));
            }
            rows.add(row);
        }
        return rows;
    }

    @Override
    public List<CrmDashboardCustomerSourceDto> customerSource() {
        List<CrmDashboardCustomerSourceDto> rows = dashboardMapper.countByCustomerSource();
        return rows == null ? new ArrayList<>() : rows;
    }

    private String normalizeGranularity(String granularity) {
        if (granularity == null || granularity.trim().isEmpty()) {
            return GRANULARITY_MONTH;
        }
        String upper = granularity.trim().toUpperCase();
        if (GRANULARITY_QUARTER.equals(upper)) {
            return GRANULARITY_QUARTER;
        }
        if (GRANULARITY_MONTH.equals(upper)) {
            return GRANULARITY_MONTH;
        }
        throw new IllegalArgumentException("granularity must be MONTH or QUARTER: " + granularity);
    }

    private Map<String, CrmDashboardPeriodAggregateDto> toMap(
            List<CrmDashboardPeriodAggregateDto> rows) {
        Map<String, CrmDashboardPeriodAggregateDto> map = new HashMap<>();
        if (rows != null) {
            for (CrmDashboardPeriodAggregateDto row : rows) {
                if (row != null && row.getPeriod() != null) {
                    map.put(row.getPeriod(), row);
                }
            }
        }
        return map;
    }

    /**
     * 生成最近 N 个时间桶的标识（按时间正序）.
     */
    private List<String> buildPeriodKeys(String granularity, int periods) {
        List<String> keys = new ArrayList<>(periods);
        YearMonth now = YearMonth.now();
        if (GRANULARITY_QUARTER.equals(granularity)) {
            int year = now.getYear();
            int quarter = (now.getMonthValue() - 1) / 3 + 1;
            for (int i = periods - 1; i >= 0; i--) {
                int q = quarter - i;
                int y = year;
                while (q <= 0) {
                    q += 4;
                    y -= 1;
                }
                keys.add(y + "-Q" + q);
            }
            return keys;
        }
        for (int i = periods - 1; i >= 0; i--) {
            YearMonth month = now.minusMonths(i);
            keys.add(String.format("%04d-%02d", month.getYear(), month.getMonthValue()));
        }
        return keys;
    }

}
