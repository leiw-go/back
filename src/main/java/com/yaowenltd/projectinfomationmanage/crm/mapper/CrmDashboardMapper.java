/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardAggregateDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardCustomerSourceDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardPeriodAggregateDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmDashboardTopProductDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard.CrmReceivableAgingRowDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.math.BigDecimal;
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

    /**
     * 统计本月的成交额（订单 {@code total_amount_base} 合计，本位币 USD）.
     * <p>
     * 成交口径：订单已确认即视为成交（{@code CONFIRMED / IN_PRODUCTION / SHIPPED /
     * DELIVERED / COMPLETED}），排除已取消（{@code CANCELLED}）；按订单创建时间归月。
     * </p>
     *
     * @return 本月成交额，没有数据时为 0
     */
    BigDecimal sumMonthDealAmount();

    /**
     * 按时间桶统计成交额（订单 {@code total_amount_base} 合计，本位币 USD）.
     * <p>
     * 成交口径与 {@link #sumMonthDealAmount()} 一致；返回行只填 {@code period} 与 {@code amount_base}。
     * </p>
     *
     * @param granularity MONTH / QUARTER
     * @return 各时间桶的成交额
     */
    List<CrmDashboardPeriodAggregateDto> sumDealAmountByPeriod(@Param("granularity") String granularity);

    /**
     * 统计逾期的未回款金额（本位币 USD）.
     * <p>
     * 逾期 = 约定收款日早于今天；只统计未收齐（已收金额小于应收金额）的应收。
     * </p>
     *
     * @return 逾期未回款金额，没有数据时为 0
     */
    BigDecimal sumOverdueReceivableAmountBase();

    /**
     * 查询全部未收齐的应收原始行（到期日 + 未回款金额，本位币 USD）.
     * <p>
     * 分桶在 Service 层做，保证桶边界只有一处定义。
     * </p>
     *
     * @return 未回款应收行
     */
    List<CrmReceivableAgingRowDto> selectOutstandingReceivables();

    /**
     * 按成交金额统计产品排行.
     * <p>
     * 排除已取消（{@code CANCELLED}）的订单；成交金额按订单的
     * {@code total_amount_base / total_amount} 汇率折算成本位币 USD。
     * </p>
     *
     * @param limit 取前 N 名
     * @return 产品排行（按成交金额倒序）
     */
    List<CrmDashboardTopProductDto> sumTopProducts(@Param("limit") int limit);

}
