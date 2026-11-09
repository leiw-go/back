/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableAgingDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableWriteOffRequest;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmReceivableService;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRM 应收 HTTP 接口.
 *
 * @since 2026-11-08
 */
@RestController
@RequestMapping("/api/crm/receivables")
@Tag(name = "CRM Receivable", description = "CRM receivable APIs")
public class CrmReceivableController {

    private final CrmReceivableService crmReceivableService;

    /**
     * 使用指定的 Service 构造 CrmReceivableController.
     *
     * @param crmReceivableService 应收服务
     */
    public CrmReceivableController(CrmReceivableService crmReceivableService) {
        this.crmReceivableService = crmReceivableService;
    }

    /**
     * 账龄分析.
     *
     * @return 账龄分析
     */
    @GetMapping("/aging")
    @Operation(summary = "Receivable aging", description = "应收账龄分析：0-30 / 31-60 / 61-90 / 90+ 天")
    public ResponseResult<CrmReceivableAgingDto> aging() {
        return ResponseResult.success(crmReceivableService.aging());
    }

    /**
     * 分页查询应收列表.
     *
     * @param query 筛选条件
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "List receivables", description = "分页查询应收列表")
    public ResponseResult<PageResponse<CrmReceivableDto>> listReceivables(CrmReceivableQueryRequest query) {
        return ResponseResult.success(crmReceivableService.list(query));
    }

    /**
     * 登记收款（核销）.
     *
     * @param id      应收 ID
     * @param request 核销信息
     * @return 核销后的应收
     */
    @PostMapping("/{id}/writeoff")
    @Operation(summary = "Write off receivable", description = "登记收款并核销应收")
    public ResponseResult<CrmReceivableDto> writeOff(@PathVariable String id,
                                                      @RequestBody CrmReceivableWriteOffRequest request) {
        return ResponseResult.success(crmReceivableService.writeOff(id, request));
    }

}
