/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityStageCountDto;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmOpportunityEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * CRM 商机数据库操作的 Mapper 接口.
 *
 * @since 2026-11-08
 */
@Mapper
public interface CrmOpportunityMapper {

    /**
     * 按主键查询未删除的商机.
     *
     * @param id 商机 ID
     * @return 商机；不存在返回 null
     */
    CrmOpportunityEntity selectById(@Param("id") String id);

    /**
     * 统计某客户下的商机数量.
     *
     * @param customerId 客户 ID
     * @return 商机数量
     */
    long countByCustomer(@Param("customerId") String customerId);

    /**
     * 新增商机.
     *
     * @param opportunity 商机实体
     * @return 受影响的行数
     */
    int insert(CrmOpportunityEntity opportunity);

    /**
     * 更新商机.
     *
     * @param opportunity 商机实体
     * @return 受影响的行数
     */
    int update(CrmOpportunityEntity opportunity);

    /**
     * 逻辑删除商机.
     *
     * @param id        商机 ID
     * @param deletedAt 删除时刻的毫秒时间戳
     * @return 受影响的行数
     */
    int softDelete(@Param("id") String id, @Param("deletedAt") Long deletedAt);

    /**
     * 分页查询商机.
     *
     * @param query 筛选条件
     * @return 商机列表
     */
    List<CrmOpportunityEntity> selectPage(CrmOpportunityQueryRequest query);

    /**
     * 按阶段聚合商机（漏斗统计用）.
     *
     * @return 各阶段的商机数与本位币金额合计
     */
    List<CrmOpportunityStageCountDto> countByStage();

}
