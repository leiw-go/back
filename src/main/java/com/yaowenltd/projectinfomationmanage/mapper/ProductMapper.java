/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.mapper;

import com.yaowenltd.projectinfomationmanage.model.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Mapper interface for Product database operations.
 */
@Mapper
public interface ProductMapper {

    /**
     * Inserts a new product.
     *
     * @param product the product to insert
     * @return the number of affected rows
     */
    int insertProduct(Product product);

    /**
     * Updates an existing product.
     *
     * @param product the product with updated information
     * @return the number of affected rows
     */
    int updateProduct(Product product);

    /**
     * Deletes a product by ID.
     *
     * @param id the product ID
     * @return the number of affected rows
     */
    int deleteProductById(@Param("id") String id);

    /**
     * Finds a product by ID.
     *
     * @param id the product ID
     * @return the product, or null if not found
     */
    Product findProductById(@Param("id") String id);

    /**
     * Returns all products.
     *
     * @return list of all products
     */
    List<Product> findAllProducts();
}
