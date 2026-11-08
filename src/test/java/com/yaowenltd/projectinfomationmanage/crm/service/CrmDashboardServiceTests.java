/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmDashboardMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardAggregateDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardCustomerSourceDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardPeriodAggregateDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardSummaryDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardTrendDto;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.CrmDashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CrmDashboardService} 的单元测试。
 * <p>
 * 契约来自需求文档 §3.10、§4.11：顶部指标只看已落地域（客户 + 商机）；
 * 趋势按时间桶补齐空桶（没有数据的月份也要出现，值为 0）；
 * 赢单率按"当期新增商机的当前阶段"统计。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class CrmDashboardServiceTests {

    @Mock
    private CrmDashboardMapper dashboardMapper;

    @Mock
    private CrmOpportunityService crmOpportunityService;

    private CrmDashboardService crmDashboardService;

    @BeforeEach
    void setUp() {
        crmDashboardService = new CrmDashboardServiceImpl(dashboardMapper, crmOpportunityService);
    }

    private static CrmDashboardPeriodAggregateDto period(String key, long count) {
        CrmDashboardPeriodAggregateDto dto = new CrmDashboardPeriodAggregateDto();
        dto.setPeriod(key);
        dto.setCount(count);
        return dto;
    }

    @Test
    @DisplayName("顶部指标应汇总活跃客户与进行中商机")
    void testSummaryAggregates() {
        when(dashboardMapper.countActiveCustomers()).thenReturn(12L);
        CrmDashboardAggregateDto open = new CrmDashboardAggregateDto();
        open.setCount(5);
        open.setAmountBase(new BigDecimal("80000.00"));
        when(dashboardMapper.sumOpenOpportunities()).thenReturn(open);

        CrmDashboardSummaryDto summary = crmDashboardService.summary();

        assertEquals(12L, summary.getActiveCustomerCount());
        assertEquals(5L, summary.getOpenOpportunityCount());
        assertEquals(0, new BigDecimal("80000.00").compareTo(summary.getOpenOpportunityAmountBase()));
    }

    @Test
    @DisplayName("订单域未落地时成交额与逾期应收恒为 0")
    void testSummaryOrderMetricsAreZeroBeforeOrderDomain() {
        when(dashboardMapper.countActiveCustomers()).thenReturn(0L);
        when(dashboardMapper.sumOpenOpportunities()).thenReturn(null);

        CrmDashboardSummaryDto summary = crmDashboardService.summary();

        assertEquals(0, summary.getMonthDealAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0, summary.getOverdueReceivableAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0L, summary.getOpenOpportunityCount());
    }

    @Test
    @DisplayName("销售漏斗直接复用商机漏斗")
    void testPipelineDelegatesToOpportunityService() {
        when(crmOpportunityService.pipeline()).thenReturn(new com.yaowenltd.projectinfomationmanage.crm.model.dto
                .opportunity.CrmOpportunityPipelineDto());

        assertNotNull(crmDashboardService.pipeline());
        verify(crmOpportunityService).pipeline();
    }

    @Test
    @DisplayName("趋势按月补齐空桶并按当期新增商机统计赢单率")
    void testTrendFillsEmptyBuckets() {
        YearMonth lastMonth = YearMonth.now().minusMonths(1);
        String lastMonthKey = String.format("%04d-%02d", lastMonth.getYear(), lastMonth.getMonthValue());

        when(dashboardMapper.countNewCustomersByPeriod(anyString()))
                .thenReturn(Collections.singletonList(period(lastMonthKey, 4)));
        CrmDashboardPeriodAggregateDto opportunity = period(lastMonthKey, 3);
        opportunity.setWon(1);
        opportunity.setDecided(2);
        opportunity.setAmountBase(new BigDecimal("30000.00"));
        when(dashboardMapper.countNewOpportunitiesByPeriod(anyString()))
                .thenReturn(Collections.singletonList(opportunity));

        List<CrmDashboardTrendDto> rows = crmDashboardService.trend("MONTH", 3);

        assertEquals(3, rows.size());
        assertEquals(lastMonthKey, rows.get(1).getPeriod());
        assertEquals(4L, rows.get(1).getNewCustomerCount());
        assertEquals(3L, rows.get(1).getNewOpportunityCount());
        assertEquals(0, new BigDecimal("50").compareTo(rows.get(1).getWinRate()));
        assertEquals(0, new BigDecimal("30000.00").compareTo(rows.get(1).getNewOpportunityAmountBase()));
        assertEquals(0L, rows.get(0).getNewCustomerCount());
        assertEquals(0L, rows.get(2).getNewCustomerCount());
    }

    @Test
    @DisplayName("趋势粒度非法时应报错")
    void testTrendRejectsUnknownGranularity() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmDashboardService.trend("WEEK", 6));
        assertTrue(error.getMessage().contains("granularity"));
    }

    @Test
    @DisplayName("按季统计时时间桶为 yyyy-Qn")
    void testTrendByQuarterUsesQuarterKeys() {
        when(dashboardMapper.countNewCustomersByPeriod(anyString())).thenReturn(Collections.emptyList());
        when(dashboardMapper.countNewOpportunitiesByPeriod(anyString())).thenReturn(Collections.emptyList());

        List<CrmDashboardTrendDto> rows = crmDashboardService.trend("QUARTER", 4);

        assertEquals(4, rows.size());
        assertTrue(rows.get(0).getPeriod().matches("\\d{4}-Q[1-4]"));
        assertTrue(rows.get(3).getPeriod().matches("\\d{4}-Q[1-4]"));
    }

    @Test
    @DisplayName("客户来源分布直接透出聚合结果")
    void testCustomerSource() {
        CrmDashboardCustomerSourceDto source = new CrmDashboardCustomerSourceDto();
        source.setSource("TRADE_SHOW");
        source.setCustomerCount(7);
        source.setOpportunityCount(9);
        source.setOpportunityAmountBase(new BigDecimal("120000.00"));
        when(dashboardMapper.countByCustomerSource()).thenReturn(Arrays.asList(source));

        List<CrmDashboardCustomerSourceDto> rows = crmDashboardService.customerSource();

        assertEquals(1, rows.size());
        assertEquals("TRADE_SHOW", rows.get(0).getSource());
        assertEquals(7L, rows.get(0).getCustomerCount());
    }

}
