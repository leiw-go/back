/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.country.CrmCountryInfoDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.country.CrmCountryRefDto;
import com.yaowenltd.projectinfomationmanage.crm.service.CountryReferenceService;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * CRM 国家参考 HTTP 接口.
 * <p>
 * 输入国家 → 返回该国的**时区、当前时间、主要货币、对美元与人民币的汇率**。
 *
 * @since 2026-11-09
 */
@RestController
@RequestMapping("/api/crm/countries")
@Tag(name = "CRM Country", description = "CRM country reference APIs")
public class CrmCountryController {

    private final CountryReferenceService countryReferenceService;

    /**
     * 使用指定依赖构造 CrmCountryController.
     *
     * @param countryReferenceService 国家参考服务
     */
    public CrmCountryController(CountryReferenceService countryReferenceService) {
        this.countryReferenceService = countryReferenceService;
    }

    /**
     * 国家列表（可按国家码 / 英文名 / 中文名搜索）.
     *
     * @param keyword 关键词；为空表示全部
     * @param page    页码，从 1 开始
     * @param size    每页条数
     * @return 国家列表
     */
    @GetMapping
    @Operation(summary = "List countries", description = "国家列表，可按国家码 / 英文名 / 中文名搜索")
    public ResponseResult<PageResponse<CrmCountryRefDto>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ResponseResult.success(countryReferenceService.list(keyword, page, size));
    }

    /**
     * 单个国家的完整信息.
     *
     * @param country 国家码、英文名或中文名（支持模糊）
     * @return 时区、当前时间、主要货币与对美元 / 人民币的汇率
     */
    @GetMapping("/info")
    @Operation(summary = "Get country info",
            description = "查询国家信息：时区、当前时间、主要货币、对美元与人民币的汇率")
    public ResponseResult<CrmCountryInfoDto> info(@RequestParam String country) {
        return ResponseResult.success(countryReferenceService.getInfo(country));
    }

}
