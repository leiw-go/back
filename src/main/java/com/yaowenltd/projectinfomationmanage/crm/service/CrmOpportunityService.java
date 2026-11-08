/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityPipelineDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityStageRequest;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;

/**
 * CRM 商机服务.
 *
 * @since 2026-11-08
 */
public interface CrmOpportunityService {

    /**
     * 新建商机.
     *
     * @param request 商机信息
     * @return 新建后的商机
     */
    CrmOpportunityDto create(CrmOpportunityRequest request);

    /**
     * 更新商机（含推进阶段）.
     *
     * @param id      商机 ID
     * @param request 商机信息
     * @return 更新后的商机
     */
    CrmOpportunityDto update(String id, CrmOpportunityRequest request);

    /**
     * 查询商机详情.
     *
     * @param id 商机 ID
     * @return 商机
     */
    CrmOpportunityDto getById(String id);

    /**
     * 逻辑删除商机.
     *
     * @param id 商机 ID
     */
    void delete(String id);

    /**
     * 分页查询商机.
     *
     * @param query 筛选条件
     * @return 分页结果
     */
    PageResponse<CrmOpportunityDto> list(CrmOpportunityQueryRequest query);

    /**
     * 推进商机阶段.
     *
     * @param id      商机 ID
     * @param request 目标阶段与丢单原因
     * @return 更新后的商机
     */
    CrmOpportunityDto changeStage(String id, CrmOpportunityStageRequest request);

    /**
     * 销售漏斗统计（按当前 stage 快照）.
     *
     * @return 漏斗统计
     */
    CrmOpportunityPipelineDto pipeline();

}
