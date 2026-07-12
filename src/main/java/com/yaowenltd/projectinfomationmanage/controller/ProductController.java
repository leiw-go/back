/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.model.dto.ProductDto;
import com.yaowenltd.projectinfomationmanage.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 负责产品信息配置相关操作的 HTTP 接口.
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Management", description = "Product information configuration APIs")
public class ProductController {

    private final ProductService productService;

    /**
     * 使用给定的 ProductService 构造 ProductController.
     *
     * @param productService 产品服务
     */
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 创建一个新产品.
     *
     * @param productDto 产品数据
     * @return 已创建的产品
     */
    @PostMapping
    @Operation(summary = "Create product", description = "Create a new product")
    public ResponseResult<ProductDto> createProduct(@Valid @RequestBody ProductDto productDto) {
        ProductDto created = productService.createProduct(productDto);
        return ResponseResult.created(created);
    }

    /**
     * 更新已存在的产品.
     *
     * @param id         产品 ID
     * @param productDto 更新后的产品数据
     * @return 更新后的产品
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Update an existing product")
    public ResponseResult<ProductDto> updateProduct(@PathVariable String id, @Valid @RequestBody ProductDto productDto) {
        productDto.setId(id);
        ProductDto updated = productService.updateProduct(productDto);
        return ResponseResult.success(updated);
    }

    /**
     * 根据 ID 删除产品.
     *
     * @param id 产品 ID
     * @return 成功响应
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Delete a product by ID")
    public ResponseResult<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseResult.success();
    }

    /**
     * 根据 ID 查找产品.
     *
     * @param id 产品 ID
     * @return 产品数据
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Get product details by ID")
    public ResponseResult<ProductDto> getProductById(@PathVariable String id) {
        ProductDto productDto = productService.findProductById(id);
        return ResponseResult.success(productDto);
    }

    /**
     * 返回所有产品.
     *
     * @return 全部产品列表
     */
    @GetMapping
    @Operation(summary = "List all products", description = "Get all products")
    public ResponseResult<List<ProductDto>> getAllProducts() {
        List<ProductDto> products = productService.findAllProducts();
        return ResponseResult.success(products);
    }
}
