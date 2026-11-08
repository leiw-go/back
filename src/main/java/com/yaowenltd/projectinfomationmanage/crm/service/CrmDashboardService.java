/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardCustomerSourceDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardSummaryDto;
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

}
