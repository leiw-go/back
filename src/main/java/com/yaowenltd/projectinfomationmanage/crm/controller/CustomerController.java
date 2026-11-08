/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerExportDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerRequest;
import com.yaowenltd.projectinfomationmanage.crm.service.CustomerService;
import com.alibaba.excel.EasyExcel;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 客户档案 HTTP 接口.
 *
 * @since 2026-11-08
 */
@RestController
@RequestMapping("/api/crm/customers")
@Tag(name = "CRM Customer", description = "CRM customer APIs")
public class CustomerController {

    /** 导出的 Excel 内容类型. */
    private static final String XLSX_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /** 导出文件名中的时间戳格式. */
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    /** 导出的 Sheet 名（Excel 限制 31 字符内且不能含 []:*?/\）. */
    private static final String SHEET_NAME = "客户列表";

    private final CustomerService customerService;

    /**
     * 使用指定的 Service 构造 CustomerController.
     *
     * @param customerService 客户档案服务
     */
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * 分页查询客户列表.
     *
     * @param query 筛选条件
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "List customers", description = "分页查询客户列表")
    public ResponseResult<PageResponse<CustomerDto>> listCustomers(CustomerQueryRequest query) {
        return ResponseResult.success(customerService.list(query));
    }

    /**
     * 查询客户详情.
     *
     * @param id 客户 ID
     * @return 客户
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get customer", description = "查询客户详情")
    public ResponseResult<CustomerDto> getCustomer(@PathVariable String id) {
        return ResponseResult.success(customerService.getById(id));
    }

    /**
     * 新建客户.
     *
     * @param request 客户信息
     * @return 新建后的客户
     */
    @PostMapping
    @Operation(summary = "Create customer", description = "新建客户")
    public ResponseResult<CustomerDto> createCustomer(@Valid @RequestBody CustomerRequest request) {
        return ResponseResult.created(customerService.create(request));
    }

    /**
     * 更新客户.
     *
     * @param id      客户 ID
     * @param request 客户信息
     * @return 更新后的客户
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update customer", description = "更新客户")
    public ResponseResult<CustomerDto> updateCustomer(@PathVariable String id,
                                                      @Valid @RequestBody CustomerRequest request) {
        return ResponseResult.success(customerService.update(id, request));
    }

    /**
     * 逻辑删除客户.
     *
     * @param id 客户 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete customer", description = "逻辑删除客户")
    public ResponseResult<Void> deleteCustomer(@PathVariable String id) {
        customerService.delete(id);
        return ResponseResult.success();
    }

    /**
     * 转交客户负责人.
     *
     * @param id      客户 ID
     * @param request 新的负责人
     * @return 转交后的客户
     */
    @PostMapping("/{id}/transfer")
    @Operation(summary = "Transfer customer", description = "转交客户负责人")
    public ResponseResult<CustomerDto> transferCustomer(@PathVariable String id,
                                                       @RequestBody TransferRequest request) {
        return ResponseResult.success(customerService.transfer(id, request.getNewOwnerId()));
    }

    /**
     * 导出客户为 Excel.
     *
     * @param query    筛选条件
     * @param response HTTP 响应
     * @throws IOException 写出失败时抛出
     */
    @GetMapping("/export")
    @Operation(summary = "Export customers", description = "导出客户为 Excel（上限 1 万行）")
    public void exportCustomers(CustomerQueryRequest query, HttpServletResponse response) throws IOException {
        List<CustomerDto> rows = customerService.exportList(query);
        List<CustomerExportDto> exportRows = new ArrayList<>(rows.size());
        for (CustomerDto row : rows) {
            exportRows.add(toExportDto(row));
        }

        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP);
        String chineseName = "客户列表_" + timestamp + ".xlsx";
        String asciiName = "customers_" + timestamp + ".xlsx";

        response.setContentType(XLSX_CONTENT_TYPE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setHeader("Content-Disposition", "attachment; filename=\"" + asciiName
                + "\"; filename*=UTF-8''" + URLEncoder.encode(chineseName, StandardCharsets.UTF_8));

        EasyExcel.write(response.getOutputStream(), CustomerExportDto.class)
                .sheet(SHEET_NAME)
                .doWrite(exportRows);
    }

    /**
     * 查询客户下拉选项（来源渠道、客户等级）.
     *
     * @return 下拉选项
     */
    @GetMapping("/dictionary-options")
    @Operation(summary = "Customer dictionary options", description = "客户下拉选项")
    public ResponseResult<DictionaryOptions> dictionaryOptions() {
        DictionaryOptions options = new DictionaryOptions();
        options.setLevels(java.util.List.of("A", "B", "C"));
        options.setSources(java.util.List.of("WEBSITE", "TRADE_SHOW", "ALIBABA", "EMAIL", "REFERRAL", "OTHER"));
        return ResponseResult.success(options);
    }

    private CustomerExportDto toExportDto(CustomerDto dto) {
        CustomerExportDto export = new CustomerExportDto();
        export.setId(dto.getId());
        export.setCompanyName(dto.getCompanyName());
        export.setShortName(dto.getShortName());
        export.setCountryCode(dto.getCountryCode());
        export.setCity(dto.getCity());
        export.setAddress(dto.getAddress());
        export.setIndustry(dto.getIndustry());
        export.setSource(dto.getSource());
        export.setLevel(dto.getLevel());
        export.setCustomerStatus(dto.getCustomerStatus());
        export.setOwnerId(dto.getOwnerId());
        export.setRemark(dto.getRemark());
        return export;
    }

    /**
     * 转交负责人请求体.
     */
    public static class TransferRequest {

        /**
         * 新的负责人 ID.
         */
        private String newOwnerId;

        /**
         * 获取新的负责人 ID.
         *
         * @return 负责人 ID
         */
        public String getNewOwnerId() {
            return newOwnerId;
        }

        /**
         * 设置新的负责人 ID.
         *
         * @param newOwnerId 负责人 ID
         */
        public void setNewOwnerId(String newOwnerId) {
            this.newOwnerId = newOwnerId;
        }
    }

    /**
     * 客户下拉选项.
     */
    public static class DictionaryOptions {

        /**
         * 客户等级.
         */
        private java.util.List<String> levels;

        /**
         * 来源渠道.
         */
        private java.util.List<String> sources;

        /**
         * 获取客户等级.
         *
         * @return 客户等级列表
         */
        public java.util.List<String> getLevels() {
            return levels;
        }

        /**
         * 设置客户等级.
         *
         * @param levels 客户等级列表
         */
        public void setLevels(java.util.List<String> levels) {
            this.levels = levels;
        }

        /**
         * 获取来源渠道.
         *
         * @return 来源渠道列表
         */
        public java.util.List<String> getSources() {
            return sources;
        }

        /**
         * 设置来源渠道.
         *
         * @param sources 来源渠道列表
         */
        public void setSources(java.util.List<String> sources) {
            this.sources = sources;
        }
    }
}
