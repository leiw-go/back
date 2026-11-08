/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationRejectRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationRequest;
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
 * CRM 报价单 HTTP 接口.
 *
 * @since 2026-11-08
 */
@RestController
@RequestMapping("/api/crm/quotations")
@Tag(name = "CRM Quotation", description = "CRM quotation APIs")
public class CrmQuotationController {

    private final CrmQuotationService crmQuotationService;

    /**
     * 使用指定的 Service 构造 CrmQuotationController.
     *
     * @param crmQuotationService 报价单服务
     */
    public CrmQuotationController(CrmQuotationService crmQuotationService) {
        this.crmQuotationService = crmQuotationService;
    }

    /**
     * 分页查询报价单列表.
     *
     * @param query 筛选条件
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "List quotations", description = "分页查询报价单列表")
    public ResponseResult<PageResponse<CrmQuotationDto>> listQuotations(CrmQuotationQueryRequest query) {
        return ResponseResult.success(crmQuotationService.list(query));
    }

    /**
     * 查询报价单详情（明细 + 金额汇总 + 利润）.
     *
     * @param id 报价单 ID
     * @return 报价单
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get quotation", description = "查询报价单详情，含明细、金额汇总与利润")
    public ResponseResult<CrmQuotationDto> getQuotation(@PathVariable String id) {
        return ResponseResult.success(crmQuotationService.getById(id));
    }

    /**
     * 新建报价单（服务端自动算价）.
     *
     * @param request 报价单信息
     * @return 新建后的报价单
     */
    @PostMapping
    @Operation(summary = "Create quotation", description = "新建报价单，单价与金额由服务端计算")
    public ResponseResult<CrmQuotationDto> createQuotation(@RequestBody CrmQuotationRequest request) {
        return ResponseResult.created(crmQuotationService.create(request));
    }

    /**
     * 更新报价单（仅 DRAFT 可改）.
     *
     * @param id      报价单 ID
     * @param request 报价单信息
     * @return 更新后的报价单
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update quotation", description = "更新报价单（仅 DRAFT 可改），改完服务端重算")
    public ResponseResult<CrmQuotationDto> updateQuotation(@PathVariable String id,
                                                           @RequestBody CrmQuotationRequest request) {
        return ResponseResult.success(crmQuotationService.update(id, request));
    }

    /**
     * 试算：只算不落库.
     *
     * @param request 报价单信息
     * @return 算价结果
     */
    @PostMapping("/calculate")
    @Operation(summary = "Calculate quotation", description = "试算：只算不落库，返回每行单价 / 金额与单头汇总")
    public ResponseResult<CrmQuotationDto> calculate(@RequestBody CrmQuotationRequest request) {
        return ResponseResult.success(crmQuotationService.calculate(request));
    }

    /**
     * 按当前加价率 / 折扣重算整单.
     *
     * @param id 报价单 ID
     * @return 重算后的报价单
     */
    @PostMapping("/{id}/recalculate")
    @Operation(summary = "Recalculate quotation", description = "按当前加价率 / 折扣重算整单（覆盖非手工改价的行）")
    public ResponseResult<CrmQuotationDto> recalculate(@PathVariable String id) {
        return ResponseResult.success(crmQuotationService.recalculate(id));
    }

    /**
     * 提交发送：DRAFT → SENT.
     *
     * @param id 报价单 ID
     * @return 更新后的报价单
     */
    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit quotation", description = "提交发送（DRAFT → SENT）")
    public ResponseResult<CrmQuotationDto> submit(@PathVariable String id) {
        return ResponseResult.success(crmQuotationService.submit(id));
    }

    /**
     * 客户确认：SENT → CONFIRMED.
     *
     * @param id 报价单 ID
     * @return 更新后的报价单
     */
    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm quotation", description = "客户确认（SENT → CONFIRMED）")
    public ResponseResult<CrmQuotationDto> confirm(@PathVariable String id) {
        return ResponseResult.success(crmQuotationService.confirm(id));
    }

    /**
     * 客户拒绝：SENT → REJECTED，必须填原因.
     *
     * @param id      报价单 ID
     * @param request 拒绝原因
     * @return 更新后的报价单
     */
    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject quotation", description = "客户拒绝（SENT → REJECTED），必须填原因")
    public ResponseResult<CrmQuotationDto> reject(@PathVariable String id,
                                                   @RequestBody CrmQuotationRejectRequest request) {
        String reason = request == null ? null : request.getRejectReason();
        return ResponseResult.success(crmQuotationService.reject(id, reason));
    }

}
