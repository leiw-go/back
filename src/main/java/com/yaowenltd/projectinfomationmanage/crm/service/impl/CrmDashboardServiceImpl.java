/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmDashboardMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardAggregateDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardCustomerSourceDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardPeriodAggregateDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardReceivableAgingDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardSummaryDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardTopProductDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardTrendDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmReceivableAgingBucketDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmReceivableAgingRowDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityPipelineDto;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.ReceivableAgingBucket;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmDashboardService;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmOpportunityService;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CRM 经营看板服务实现.
 * <p>
 * 口径说明：
 * <ul>
 *   <li><b>成交额</b>：订单域（{@code t_order.total_amount_base}，本位币 USD 快照）已接入。
 *       <b>成交口径</b> = 订单已确认即视为成交（{@code CONFIRMED / IN_PRODUCTION / SHIPPED /
 *       DELIVERED / COMPLETED}），只排除已取消（{@code CANCELLED}）。
 *       取舍理由：{@code COMPLETED} 只是履约终态，若只统计它，一旦订单发货 / 交付就会从
 *       "成交额"里消失，看板指标会随时间倒挂下降，不符合经营看板语义；
 *       订单一旦创建（{@code CONFIRMED}）即代表合同成立，这正是销售口径的"成交/签约额"。
 *       归期按订单创建时间（{@code create_time}）。</li>
 *   <li><b>赢单率</b>：按"当期新增商机的当前阶段"统计（同期群口径）。</li>
 *   <li><b>应收账龄</b>：只统计未收齐（已收金额 &lt; 应收金额）的应收，
 *       按到期日与今天的天数差分桶；金额为本位币 USD 口径
 *       （{@code t_receivable} 上没有 {@code *_base} 列，按所属订单的
 *       {@code total_amount_base / total_amount} 汇率快照推算）。</li>
 * </ul>
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

    /** 产品排行默认返回的名次. */
    private static final int DEFAULT_TOP_LIMIT = 10;

    /** 产品排行最多返回的名次. */
    private static final int MAX_TOP_LIMIT = 50;

    /** 百分比保留的小数位. */
    private static final int RATE_SCALE = 2;

    /** 金额保留的小数位. */
    private static final int AMOUNT_SCALE = 2;

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
        BigDecimal monthDealAmount = dashboardMapper.sumMonthDealAmount();
        if (monthDealAmount != null) {
            summary.setMonthDealAmount(scale(monthDealAmount));
        }
        BigDecimal overdueAmount = dashboardMapper.sumOverdueReceivableAmountBase();
        if (overdueAmount != null) {
            summary.setOverdueReceivableAmount(scale(overdueAmount));
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
        Map<String, CrmDashboardPeriodAggregateDto> dealAmounts = toMap(
                dashboardMapper.sumDealAmountByPeriod(actualGranularity));

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
            CrmDashboardPeriodAggregateDto dealAmount = dealAmounts.get(key);
            if (dealAmount != null && dealAmount.getAmountBase() != null) {
                row.setDealAmountBase(scale(dealAmount.getAmountBase()));
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

    @Override
    public CrmDashboardReceivableAgingDto receivableAging() {
        List<CrmReceivableAgingRowDto> rows = dashboardMapper.selectOutstandingReceivables();
        LocalDate today = LocalDate.now();

        Map<ReceivableAgingBucket, CrmReceivableAgingBucketDto> buckets = new LinkedHashMap<>();
        for (ReceivableAgingBucket bucket : ReceivableAgingBucket.values()) {
            CrmReceivableAgingBucketDto dto = new CrmReceivableAgingBucketDto();
            dto.setBucket(bucket.getBucket());
            dto.setLabel(bucket.getLabel());
            dto.setCount(0L);
            dto.setAmountBase(BigDecimal.ZERO);
            buckets.put(bucket, dto);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        long totalCount = 0L;
        if (rows != null) {
            for (CrmReceivableAgingRowDto row : rows) {
                if (row == null) {
                    continue;
                }
                int days = agingDays(row.getDueDate(), today);
                CrmReceivableAgingBucketDto bucket = buckets.get(ReceivableAgingBucket.of(days));
                BigDecimal amount = row.getAmountBase() == null ? BigDecimal.ZERO : row.getAmountBase();
                bucket.setCount(bucket.getCount() + 1L);
                bucket.setAmountBase(bucket.getAmountBase().add(amount));
                totalAmount = totalAmount.add(amount);
                totalCount++;
            }
        }

        CrmDashboardReceivableAgingDto result = new CrmDashboardReceivableAgingDto();
        result.setTotalCount(totalCount);
        result.setTotalAmountBase(scale(totalAmount));
        result.setBuckets(new ArrayList<>(buckets.values()));
        return result;
    }

    @Override
    public List<CrmDashboardTopProductDto> topProducts(Integer limit) {
        int actualLimit = normalizeTopLimit(limit);
        List<CrmDashboardTopProductDto> rows = dashboardMapper.sumTopProducts(actualLimit);
        if (rows == null) {
            return new ArrayList<>();
        }
        List<CrmDashboardTopProductDto> result = new ArrayList<>(rows.size());
        for (CrmDashboardTopProductDto row : rows) {
            if (row == null) {
                continue;
            }
            if (row.getDealAmountBase() != null) {
                row.setDealAmountBase(scale(row.getDealAmountBase()));
            }
            result.add(row);
        }
        return result;
    }

    /**
     * 计算到期日与今天的天数差（负数表示尚未到期，按 0 处理）.
     */
    private int agingDays(LocalDate dueDate, LocalDate today) {
        if (dueDate == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(dueDate, today);
        return days <= 0 ? 0 : (int) days;
    }

    /**
     * 归一化产品排行的 limit：为空取默认值，越界直接报错.
     */
    private int normalizeTopLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_TOP_LIMIT;
        }
        if (limit < 1 || limit > MAX_TOP_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_TOP_LIMIT + ": " + limit);
        }
        return limit;
    }

    /**
     * 金额统一保留 2 位小数.
     */
    private BigDecimal scale(BigDecimal amount) {
        return amount.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
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
