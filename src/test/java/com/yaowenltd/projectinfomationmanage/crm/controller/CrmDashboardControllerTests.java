/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.GlobalExceptionHandler;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardCustomerSourceDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardSummaryDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardTrendDto;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link CrmDashboardController} 的单元测试。
 * <p>
 * 契约来自需求文档 §4.11：4 个端点，响应体统一用 {@code ResponseResult} 包装。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class CrmDashboardControllerTests {

    @Mock
    private CrmDashboardService crmDashboardService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CrmDashboardController(crmDashboardService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("顶部指标应返回统一响应体")
    void testSummary() throws Exception {
        CrmDashboardSummaryDto summary = new CrmDashboardSummaryDto();
        summary.setActiveCustomerCount(12);
        summary.setOpenOpportunityCount(5);
        summary.setOpenOpportunityAmountBase(new BigDecimal("80000.00"));
        when(crmDashboardService.summary()).thenReturn(summary);

        mockMvc.perform(get("/api/crm/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.activeCustomerCount").value(12))
                .andExpect(jsonPath("$.data.openOpportunityCount").value(5));
    }

    @Test
    @DisplayName("销售漏斗应返回统一响应体")
    void testPipeline() throws Exception {
        when(crmDashboardService.pipeline()).thenReturn(new com.yaowenltd.projectinfomationmanage.crm.model.dto
                .opportunity.CrmOpportunityPipelineDto());

        mockMvc.perform(get("/api/crm/dashboard/pipeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.winRate").value(0));
    }

    @Test
    @DisplayName("业绩趋势应返回统一响应体")
    void testTrend() throws Exception {
        CrmDashboardTrendDto trend = new CrmDashboardTrendDto();
        trend.setPeriod("2026-10");
        trend.setNewCustomerCount(4);
        when(crmDashboardService.trend(eq("MONTH"), eq(6))).thenReturn(Collections.singletonList(trend));

        mockMvc.perform(get("/api/crm/dashboard/trend").param("granularity", "MONTH").param("periods", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].period").value("2026-10"))
                .andExpect(jsonPath("$.data[0].newCustomerCount").value(4));
    }

    @Test
    @DisplayName("客户来源分布应返回统一响应体")
    void testCustomerSource() throws Exception {
        CrmDashboardCustomerSourceDto source = new CrmDashboardCustomerSourceDto();
        source.setSource("TRADE_SHOW");
        source.setCustomerCount(7);
        when(crmDashboardService.customerSource()).thenReturn(Collections.singletonList(source));

        mockMvc.perform(get("/api/crm/dashboard/customer-source"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].source").value("TRADE_SHOW"));
    }

    @Test
    @DisplayName("粒度参数缺省时应走服务层默认值")
    void testTrendWithoutParams() throws Exception {
        when(crmDashboardService.trend(any(), any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/crm/dashboard/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());
    }

}
