/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityPipelineDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityStageRequest;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmOpportunityService;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRM 商机 HTTP 接口.
 *
 * @since 2026-11-08
 */
@RestController
@RequestMapping("/api/crm/opportunities")
@Tag(name = "CRM Opportunity", description = "CRM opportunity APIs")
public class CrmOpportunityController {

    private final CrmOpportunityService crmOpportunityService;

    /**
     * 使用指定的 Service 构造 CrmOpportunityController.
     *
     * @param crmOpportunityService 商机服务
     */
    public CrmOpportunityController(CrmOpportunityService crmOpportunityService) {
        this.crmOpportunityService = crmOpportunityService;
    }

    /**
     * 分页查询商机列表.
     *
     * @param query 筛选条件
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "List opportunities", description = "分页查询商机列表")
    public ResponseResult<PageResponse<CrmOpportunityDto>> listOpportunities(CrmOpportunityQueryRequest query) {
        return ResponseResult.success(crmOpportunityService.list(query));
    }

    /**
     * 销售漏斗统计.
     *
     * @return 漏斗统计
     */
    @GetMapping("/pipeline")
    @Operation(summary = "Opportunity pipeline", description = "销售漏斗统计（按当前 stage 快照）")
    public ResponseResult<CrmOpportunityPipelineDto> pipeline() {
        return ResponseResult.success(crmOpportunityService.pipeline());
    }

    /**
     * 查询商机详情.
     *
     * @param id 商机 ID
     * @return 商机
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get opportunity", description = "查询商机详情")
    public ResponseResult<CrmOpportunityDto> getOpportunity(@PathVariable String id) {
        return ResponseResult.success(crmOpportunityService.getById(id));
    }

    /**
     * 新建商机.
     *
     * @param request 商机信息
     * @return 新建后的商机
     */
    @PostMapping
    @Operation(summary = "Create opportunity", description = "新建商机")
    public ResponseResult<CrmOpportunityDto> createOpportunity(@RequestBody CrmOpportunityRequest request) {
        return ResponseResult.created(crmOpportunityService.create(request));
    }

    /**
     * 更新商机（含推进阶段）.
     *
     * @param id      商机 ID
     * @param request 商机信息
     * @return 更新后的商机
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update opportunity", description = "更新商机（含推进阶段）")
    public ResponseResult<CrmOpportunityDto> updateOpportunity(@PathVariable String id,
                                                              @RequestBody CrmOpportunityRequest request) {
        return ResponseResult.success(crmOpportunityService.update(id, request));
    }

    /**
     * 逻辑删除商机.
     *
     * @param id 商机 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete opportunity", description = "逻辑删除商机")
    public ResponseResult<Void> deleteOpportunity(@PathVariable String id) {
        crmOpportunityService.delete(id);
        return ResponseResult.success();
    }

    /**
     * 推进商机阶段.
     *
     * @param id      商机 ID
     * @param request 目标阶段与丢单原因
     * @return 更新后的商机
     */
    @PostMapping("/{id}/stage")
    @Operation(summary = "Change opportunity stage", description = "推进商机阶段，置 LOST 必须填 lostReason")
    public ResponseResult<CrmOpportunityDto> changeStage(@PathVariable String id,
                                                         @RequestBody CrmOpportunityStageRequest request) {
        return ResponseResult.success(crmOpportunityService.changeStage(id, request));
    }

}
