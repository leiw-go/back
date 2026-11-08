/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductPriceDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductPriceRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductRequest;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmProductService;
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
import java.util.List;

/**
 * CRM 产品目录 HTTP 接口.
 *
 * @since 2026-11-08
 */
@RestController
@RequestMapping("/api/crm/products")
@Tag(name = "CRM Product", description = "CRM product catalog APIs")
public class CrmProductController {

    private final CrmProductService crmProductService;

    /**
     * 使用指定的 Service 构造 CrmProductController.
     *
     * @param crmProductService 产品目录服务
     */
    public CrmProductController(CrmProductService crmProductService) {
        this.crmProductService = crmProductService;
    }

    /**
     * 分页查询产品列表.
     *
     * @param query 筛选条件
     * @return 分页结果
     */
    @GetMapping
    @Operation(summary = "List products", description = "分页查询产品列表")
    public ResponseResult<PageResponse<CrmProductDto>> listProducts(CrmProductQueryRequest query) {
        return ResponseResult.success(crmProductService.list(query));
    }

    /**
     * 查询产品详情（含各币种参考价）.
     *
     * @param id 产品 ID
     * @return 产品
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get product", description = "查询产品详情，含各币种参考价")
    public ResponseResult<CrmProductDto> getProduct(@PathVariable String id) {
        return ResponseResult.success(crmProductService.getById(id));
    }

    /**
     * 新建产品.
     *
     * @param request 产品信息
     * @return 新建后的产品
     */
    @PostMapping
    @Operation(summary = "Create product", description = "新建产品")
    public ResponseResult<CrmProductDto> createProduct(@RequestBody CrmProductRequest request) {
        return ResponseResult.created(crmProductService.create(request));
    }

    /**
     * 更新产品.
     *
     * @param id      产品 ID
     * @param request 产品信息
     * @return 更新后的产品
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "更新产品")
    public ResponseResult<CrmProductDto> updateProduct(@PathVariable String id,
                                                       @RequestBody CrmProductRequest request) {
        return ResponseResult.success(crmProductService.update(id, request));
    }

    /**
     * 逻辑删除产品.
     *
     * @param id 产品 ID
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "逻辑删除产品")
    public ResponseResult<Void> deleteProduct(@PathVariable String id) {
        crmProductService.delete(id);
        return ResponseResult.success();
    }

    /**
     * 查询产品的多币种参考价.
     *
     * @param id 产品 ID
     * @return 参考价列表
     */
    @GetMapping("/{id}/prices")
    @Operation(summary = "List product prices", description = "查询产品的多币种参考价")
    public ResponseResult<List<CrmProductPriceDto>> listPrices(@PathVariable String id) {
        return ResponseResult.success(crmProductService.listPrices(id));
    }

    /**
     * 批量替换产品的多币种参考价.
     *
     * @param id     产品 ID
     * @param prices 参考价列表，传空数组表示清空
     * @return 替换后的参考价列表
     */
    @PutMapping("/{id}/prices")
    @Operation(summary = "Replace product prices", description = "批量维护多币种参考价（整批覆盖）")
    public ResponseResult<List<CrmProductPriceDto>> replacePrices(@PathVariable String id,
                                                                   @RequestBody List<CrmProductPriceRequest> prices) {
        return ResponseResult.success(crmProductService.replacePrices(id, prices));
    }

}
