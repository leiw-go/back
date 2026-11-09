/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencyConvertDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencyDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencyRateCheckDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencyRateDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencyRateRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencySyncStatusDto;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmCurrencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * CRM 汇率 HTTP 接口.
 * <p>
 * 端点清单见 {@code docs/crm-currency-rate-design.md} §6。
 * 所有读接口只读本地表，不实时调用外部汇率接口。
 *
 * @since 2026-11-08
 */
@RestController
@RequestMapping("/api/crm/currencies")
@Tag(name = "CRM Currency", description = "CRM currency rate APIs")
public class CrmCurrencyController {

    private final CrmCurrencyService crmCurrencyService;

    /**
     * 使用指定依赖构造 CrmCurrencyController.
     *
     * @param crmCurrencyService 汇率服务
     */
    public CrmCurrencyController(CrmCurrencyService crmCurrencyService) {
        this.crmCurrencyService = crmCurrencyService;
    }

    /**
     * 币种列表（含熔断状态）.
     *
     * @return 币种列表
     */
    @GetMapping
    @Operation(summary = "List currencies", description = "币种列表，含熔断状态")
    public ResponseResult<List<CrmCurrencyDto>> listCurrencies() {
        return ResponseResult.success(crmCurrencyService.listCurrencies());
    }

    /**
     * 汇率列表（只读本地表）.
     *
     * @param baseCurrency  基准币；为空表示不限
     * @param quoteCurrency 目标币；为空表示不限
     * @param date          汇率日期；为空表示不限
     * @param source        来源；为空表示不限
     * @return 汇率列表
     */
    @GetMapping("/rates")
    @Operation(summary = "List currency rates", description = "汇率列表，只读本地表")
    public ResponseResult<List<CrmCurrencyRateDto>> listRates(
            @RequestParam(required = false) String baseCurrency,
            @RequestParam(required = false) String quoteCurrency,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String source) {
        return ResponseResult.success(crmCurrencyService.listRates(baseCurrency, quoteCurrency, date, source));
    }

    /**
     * 手工维护汇率.
     *
     * @param request 汇率内容
     * @return 保存后的汇率
     */
    @PutMapping("/rates")
    @Operation(summary = "Save manual currency rate", description = "手工维护汇率，同步任务不覆盖 MANUAL 行")
    public ResponseResult<CrmCurrencyRateDto> saveManualRate(@RequestBody CrmCurrencyRateRequest request) {
        return ResponseResult.success(crmCurrencyService.saveManualRate(request));
    }

    /**
     * 金额折算.
     *
     * @param amount 金额
     * @param from   源币
     * @param to     目标币
     * @param date   折算日期；为空表示今天
     * @return 折算结果
     */
    @GetMapping("/convert")
    @Operation(summary = "Convert amount", description = "金额折算，交叉汇率走 USD 三角折算")
    public ResponseResult<CrmCurrencyConvertDto> convert(
            @RequestParam BigDecimal amount,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseResult.success(crmCurrencyService.convert(amount, from, to, date));
    }

    /**
     * 手动触发汇率同步.
     *
     * @return 本次同步批次摘要
     */
    @PostMapping("/sync")
    @Operation(summary = "Trigger currency sync", description = "手动触发汇率同步：拉两源、交叉比对、校验后入库")
    public ResponseResult<CrmCurrencySyncStatusDto> sync() {
        return ResponseResult.success(crmCurrencyService.sync());
    }

    /**
     * 最近一次同步批次摘要.
     *
     * @return 批次摘要；没有同步记录时返回 null
     */
    @GetMapping("/sync-status")
    @Operation(summary = "Get currency sync status", description = "最近一次汇率同步批次摘要")
    public ResponseResult<CrmCurrencySyncStatusDto> syncStatus() {
        return ResponseResult.success(crmCurrencyService.syncStatus());
    }

    /**
     * 交叉比对明细.
     *
     * @param quoteCurrency 目标币；为空表示不限
     * @param checkResult   比对结果；为空表示不限
     * @return 比对明细列表
     */
    @GetMapping("/rates/verification")
    @Operation(summary = "List currency verification details",
            description = "交叉比对明细：两源原值、偏差、比对结果与拒绝原因")
    public ResponseResult<List<CrmCurrencyRateCheckDto>> verification(
            @RequestParam(required = false) String quoteCurrency,
            @RequestParam(required = false) String checkResult) {
        return ResponseResult.success(crmCurrencyService.verification(quoteCurrency, checkResult));
    }

}
