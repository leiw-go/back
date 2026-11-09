/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardCustomerSourceDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardReceivableAgingDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardSummaryDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardTopProductDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardTrendDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityPipelineDto;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * CRM 经营看板 HTTP 接口.
 *
 * @since 2026-11-08
 */
@RestController
@RequestMapping("/api/crm/dashboard")
@Tag(name = "CRM Dashboard", description = "CRM dashboard APIs")
public class CrmDashboardController {

    private final CrmDashboardService crmDashboardService;

    /**
     * 使用指定的 Service 构造 CrmDashboardController.
     *
     * @param crmDashboardService 看板服务
     */
    public CrmDashboardController(CrmDashboardService crmDashboardService) {
        this.crmDashboardService = crmDashboardService;
    }

    /**
     * 顶部指标.
     *
     * @return 顶部指标
     */
    @GetMapping("/summary")
    @Operation(summary = "Dashboard summary", description = "顶部指标")
    public ResponseResult<CrmDashboardSummaryDto> summary() {
        return ResponseResult.success(crmDashboardService.summary());
    }

    /**
     * 销售漏斗.
     *
     * @return 漏斗统计
     */
    @GetMapping("/pipeline")
    @Operation(summary = "Dashboard pipeline", description = "销售漏斗")
    public ResponseResult<CrmOpportunityPipelineDto> pipeline() {
        return ResponseResult.success(crmDashboardService.pipeline());
    }

    /**
     * 业绩趋势.
     *
     * @param granularity MONTH 或 QUARTER；为空时按月
     * @param periods     时间桶个数；为空时取 6
     * @return 趋势数据
     */
    @GetMapping("/trend")
    @Operation(summary = "Dashboard trend", description = "业绩趋势（按月 / 按季）")
    public ResponseResult<List<CrmDashboardTrendDto>> trend(
            @RequestParam(required = false) String granularity,
            @RequestParam(required = false) Integer periods) {
        return ResponseResult.success(crmDashboardService.trend(granularity, periods));
    }

    /**
     * 客户来源分布.
     *
     * @return 各来源的统计
     */
    @GetMapping("/customer-source")
    @Operation(summary = "Dashboard customer source", description = "客户来源分布")
    public ResponseResult<List<CrmDashboardCustomerSourceDto>> customerSource() {
        return ResponseResult.success(crmDashboardService.customerSource());
    }

    /**
     * 应收账龄.
     *
     * @return 账龄分桶（0-30 / 31-60 / 61-90 / 90+ 天）
     */
    @GetMapping("/receivable-aging")
    @Operation(summary = "Dashboard receivable aging", description = "应收账龄（未回款金额按到期日分桶）")
    public ResponseResult<CrmDashboardReceivableAgingDto> receivableAging() {
        return ResponseResult.success(crmDashboardService.receivableAging());
    }

    /**
     * 产品成交排行.
     *
     * @param limit 取前 N 名；为空时取 10，取值范围 1 ~ 50，越界报 400
     * @return 按成交金额倒序的产品列表
     */
    @GetMapping("/top-products")
    @Operation(summary = "Dashboard top products", description = "产品成交排行（按成交金额）")
    public ResponseResult<List<CrmDashboardTopProductDto>> topProducts(
            @RequestParam(required = false) Integer limit) {
        return ResponseResult.success(crmDashboardService.topProducts(limit));
    }

}
