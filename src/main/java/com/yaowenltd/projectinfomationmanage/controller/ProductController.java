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
 * Controller for product information configuration operations.
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Product Management", description = "Product information configuration APIs")
public class ProductController {

    private final ProductService productService;

    /**
     * Constructs a ProductController with the given ProductService.
     *
     * @param productService the product service
     */
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Creates a new product.
     *
     * @param productDto the product data
     * @return the created product
     */
    @PostMapping
    @Operation(summary = "Create product", description = "Create a new product")
    public ResponseResult<ProductDto> createProduct(@Valid @RequestBody ProductDto productDto) {
        ProductDto created = productService.createProduct(productDto);
        return ResponseResult.created(created);
    }

    /**
     * Updates an existing product.
     *
     * @param id         the product ID
     * @param productDto the updated product data
     * @return the updated product
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Update an existing product")
    public ResponseResult<ProductDto> updateProduct(@PathVariable String id, @Valid @RequestBody ProductDto productDto) {
        productDto.setId(id);
        ProductDto updated = productService.updateProduct(productDto);
        return ResponseResult.success(updated);
    }

    /**
     * Deletes a product by ID.
     *
     * @param id the product ID
     * @return success response
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Delete a product by ID")
    public ResponseResult<Void> deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return ResponseResult.success();
    }

    /**
     * Finds a product by ID.
     *
     * @param id the product ID
     * @return the product data
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Get product details by ID")
    public ResponseResult<ProductDto> getProductById(@PathVariable String id) {
        ProductDto productDto = productService.findProductById(id);
        return ResponseResult.success(productDto);
    }

    /**
     * Returns all products.
     *
     * @return list of all products
     */
    @GetMapping
    @Operation(summary = "List all products", description = "Get all products")
    public ResponseResult<List<ProductDto>> getAllProducts() {
        List<ProductDto> products = productService.findAllProducts();
        return ResponseResult.success(products);
    }
}
