/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service;

import com.yaowenltd.projectinfomationmanage.model.dto.ProductDto;

import java.util.List;

/**
 * 服务接口，定义产品管理业务能力.
 */
public interface ProductService {

    /**
     * 创建新产品.
     *
     * @param productDto 产品数据
     * @return 已创建的产品
     */
    ProductDto createProduct(ProductDto productDto);

    /**
     * 更新已存在的产品.
     *
     * @param productDto 包含更新字段的产品数据
     * @return 更新后的产品
     */
    ProductDto updateProduct(ProductDto productDto);

    /**
     * 根据 ID 删除产品.
     *
     * @param id 产品 ID
     */
    void deleteProduct(String id);

    /**
     * 根据 ID 查找产品.
     *
     * @param id 产品 ID
     * @return 产品数据
     */
    ProductDto findProductById(String id);

    /**
     * 返回所有产品.
     *
     * @return 所有产品列表
     */
    List<ProductDto> findAllProducts();
}