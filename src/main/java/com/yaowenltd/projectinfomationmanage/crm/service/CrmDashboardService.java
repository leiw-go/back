/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardCustomerSourceDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardReceivableAgingDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardSummaryDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardTopProductDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardTrendDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityPipelineDto;
import java.util.List;

/**
 * CRM 经营看板服务.
 *
 * @since 2026-11-08
 */
public interface CrmDashboardService {

    /**
     * 顶部指标.
     *
     * @return 顶部指标
     */
    CrmDashboardSummaryDto summary();

    /**
     * 销售漏斗.
     *
     * @return 漏斗统计
     */
    CrmOpportunityPipelineDto pipeline();

    /**
     * 业绩趋势（按月或按季）.
     *
     * @param granularity MONTH 或 QUARTER；为空时按月
     * @param periods     时间桶个数；为空时取 6，最大 24
     * @return 趋势数据，按时间倒序之外按时间正序返回
     */
    List<CrmDashboardTrendDto> trend(String granularity, Integer periods);

    /**
     * 客户来源分布.
     *
     * @return 各来源的统计
     */
    List<CrmDashboardCustomerSourceDto> customerSource();

    /**
     * 应收账龄.
     * <p>
     * 只统计未收齐的应收，按到期日与今天的天数差分桶（0-30 / 31-60 / 61-90 / 90+），
     * 金额为本位币 USD 口径。
     * </p>
     *
     * @return 账龄分桶结果
     */
    CrmDashboardReceivableAgingDto receivableAging();

    /**
     * 产品成交排行.
     *
     * @param limit 取前 N 名；为空时取 10，取值范围 1 ~ 50，越界抛 {@code IllegalArgumentException}
     * @return 按成交金额倒序的产品列表
     */
    List<CrmDashboardTopProductDto> topProducts(Integer limit);

}
