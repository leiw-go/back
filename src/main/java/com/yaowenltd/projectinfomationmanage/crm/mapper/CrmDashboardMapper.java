/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardAggregateDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardCustomerSourceDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardPeriodAggregateDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * CRM 经营看板的聚合查询 Mapper 接口.
 *
 * @since 2026-11-08
 */
@Mapper
public interface CrmDashboardMapper {

    /**
     * 统计活跃客户数（未删除且客户状态不是 LOST）.
     *
     * @return 计数
     */
    long countActiveCustomers();

    /**
     * 统计进行中的商机数与金额（阶段不是 WON / LOST）.
     *
     * @return 计数与金额合计
     */
    CrmDashboardAggregateDto sumOpenOpportunities();

    /**
     * 按时间桶统计新增客户数.
     *
     * @param granularity MONTH / QUARTER
     * @return 各时间桶的计数
     */
    List<CrmDashboardPeriodAggregateDto> countNewCustomersByPeriod(@Param("granularity") String granularity);

    /**
     * 按时间桶统计新增商机数、赢单数、已决数与金额.
     *
     * @param granularity MONTH / QUARTER
     * @return 各时间桶的聚合结果
     */
    List<CrmDashboardPeriodAggregateDto> countNewOpportunitiesByPeriod(@Param("granularity") String granularity);

    /**
     * 按来源渠道统计客户数与商机数、金额.
     *
     * @return 各来源的统计
     */
    List<CrmDashboardCustomerSourceDto> countByCustomerSource();

}
