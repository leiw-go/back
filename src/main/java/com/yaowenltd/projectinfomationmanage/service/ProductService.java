/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service;

import com.yaowenltd.projectinfomationmanage.model.dto.ProductDto;

import java.util.List;

/**
 * Service interface for product management operations.
 */
public interface ProductService {

    /**
     * Creates a new product.
     *
     * @param productDto the product data
     * @return the created product
     */
    ProductDto createProduct(ProductDto productDto);

    /**
     * Updates an existing product.
     *
     * @param productDto the product data with updates
     * @return the updated product
     */
    ProductDto updateProduct(ProductDto productDto);

    /**
     * Deletes a product by ID.
     *
     * @param id the product ID
     */
    void deleteProduct(String id);

    /**
     * Finds a product by ID.
     *
     * @param id the product ID
     * @return the product data
     */
    ProductDto findProductById(String id);

    /**
     * Returns all products.
     *
     * @return list of all products
     */
    List<ProductDto> findAllProducts();
}
