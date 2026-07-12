/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.impl;

import com.yaowenltd.projectinfomationmanage.mapper.ProductMapper;
import com.yaowenltd.projectinfomationmanage.model.dto.ProductDto;
import com.yaowenltd.projectinfomationmanage.model.entity.Product;
import com.yaowenltd.projectinfomationmanage.service.ProductService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 产品管理的 Spring 实现.
 */
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;

    /**
     * 构造 ProductServiceImpl，注入所需依赖.
     *
     * @param productMapper 产品 Mapper
     */
    public ProductServiceImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    /**
     * 创建新产品.
     *
     * @param productDto 产品数据
     * @return 已创建的产品
     */
    @Override
    public ProductDto createProduct(ProductDto productDto) {
        Product product = new Product();
        String id = UUID.randomUUID().toString();
        product.setId(id);
        product.setProductName(productDto.getProductName());
        product.setProductCode(productDto.getProductCode());
        product.setCategory(productDto.getCategory());
        product.setDescription(productDto.getDescription());
        product.setPrice(productDto.getPrice());
        product.setStatus(productDto.getStatus() != null ? productDto.getStatus() : 1);
        LocalDateTime now = LocalDateTime.now();
        product.setCreateTime(now);
        product.setUpdateTime(now);

        productMapper.insertProduct(product);

        productDto.setId(id);
        productDto.setCreateTime(now);
        productDto.setUpdateTime(now);
        return productDto;
    }

    /**
     * 更新已存在的产品.
     *
     * @param productDto 包含更新字段的产品数据
     * @return 更新后的产品
     */
    @Override
    public ProductDto updateProduct(ProductDto productDto) {
        Product existingProduct = productMapper.findProductById(productDto.getId());
        if (existingProduct == null) {
            throw new IllegalArgumentException("product not found");
        }

        Product product = new Product();
        product.setId(productDto.getId());
        product.setProductName(productDto.getProductName());
        product.setProductCode(productDto.getProductCode());
        product.setCategory(productDto.getCategory());
        product.setDescription(productDto.getDescription());
        product.setPrice(productDto.getPrice());
        product.setStatus(productDto.getStatus());

        productMapper.updateProduct(product);
        return productDto;
    }

    /**
     * 根据 ID 删除产品.
     *
     * @param id 产品 ID
     */
    @Override
    public void deleteProduct(String id) {
        productMapper.deleteProductById(id);
    }

    /**
     * 根据 ID 查找产品.
     *
     * @param id 产品 ID
     * @return 产品数据
     */
    @Override
    public ProductDto findProductById(String id) {
        Product product = productMapper.findProductById(id);
        if (product == null) {
            throw new IllegalArgumentException("product not found");
        }
        return convertToDto(product);
    }

    /**
     * 返回所有产品.
     *
     * @return 所有产品列表
     */
    @Override
    public List<ProductDto> findAllProducts() {
        List<Product> products = productMapper.findAllProducts();
        List<ProductDto> productDtos = new ArrayList<>();
        for (Product product : products) {
            productDtos.add(convertToDto(product));
        }
        return productDtos;
    }

    private ProductDto convertToDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setProductName(product.getProductName());
        dto.setProductCode(product.getProductCode());
        dto.setCategory(product.getCategory());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStatus(product.getStatus());
        dto.setCreateTime(product.getCreateTime());
        dto.setUpdateTime(product.getUpdateTime());
        return dto;
    }
}