/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.inquiry.CrmInquiryDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.inquiry.CrmInquiryQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.inquiry.CrmInquiryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationRequest;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmInquiryService;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmQuotationService;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRM 询盘 HTTP 接口.
 *
 * @since 2026-11-08
 */
@RestController
@RequestMapping("/api/crm/inquiries")
@Tag(name = "CRM Inquiry", description = "CRM inquiry APIs")
public class CrmInquiryController {

    private final CrmInquiryService crmInquiryService;
    private final CrmQuotationService crmQuotationService;

    /**
     * 使用指定的 Service 构造 CrmInquiryController.
     *
     * @param crmInquiryService   询盘服务
     * @param crmQuotationService 报价单服务（基于询盘建单）
     */
    public CrmInquiryController(CrmInquiryService crmInquiryService,
                                CrmQuotationService crmQuotationService) {
        this.crmInquiryService = crmInquiryService;
        this.crmQuotationService = crmQuotationService;
    }

    /**
     * 分页查询询盘列表.
     *
     * @param query 筛选条件
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "List inquiries", description = "分页查询询盘列表，可按状态 / 客户 / 负责人 / 超时未报价筛选")
    public ResponseResult<PageResponse<CrmInquiryDto>> listInquiries(CrmInquiryQueryRequest query) {
        return ResponseResult.success(crmInquiryService.list(query));
    }

    /**
     * 查询询盘详情（含关联报价单）.
     *
     * @param id 询盘 ID
     * @return 询盘
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get inquiry", description = "查询询盘详情，含关联报价单")
    public ResponseResult<CrmInquiryDto> getInquiry(@PathVariable String id) {
        return ResponseResult.success(crmInquiryService.getById(id));
    }

    /**
     * 新建询盘.
     *
     * @param request 询盘信息
     * @return 新建后的询盘
     */
    @PostMapping
    @Operation(summary = "Create inquiry", description = "新建询盘")
    public ResponseResult<CrmInquiryDto> createInquiry(@RequestBody CrmInquiryRequest request) {
        return ResponseResult.created(crmInquiryService.create(request));
    }

    /**
     * 更新询盘.
     *
     * @param id      询盘 ID
     * @param request 询盘信息
     * @return 更新后的询盘
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update inquiry", description = "更新询盘")
    public ResponseResult<CrmInquiryDto> updateInquiry(@PathVariable String id,
                                                        @RequestBody CrmInquiryRequest request) {
        return ResponseResult.success(crmInquiryService.update(id, request));
    }

    /**
     * 基于询盘创建报价单（同询盘 + 同币种下版本号递增）.
     *
     * @param id      询盘 ID
     * @param request 报价单信息
     * @return 新建后的报价单
     */
    @PostMapping("/{id}/quotations")
    @Operation(summary = "Create quotation from inquiry", description = "基于询盘创建报价单（新版本）")
    public ResponseResult<CrmQuotationDto> createQuotation(@PathVariable String id,
                                                           @RequestBody CrmQuotationRequest request) {
        return ResponseResult.created(crmQuotationService.createFromInquiry(id, request));
    }

}
