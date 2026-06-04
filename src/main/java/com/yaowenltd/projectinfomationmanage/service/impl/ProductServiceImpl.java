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
 * Implementation of ProductService for product management.
 */
@Service
public class ProductServiceImpl implements ProductService {

    private final ProductMapper productMapper;

    /**
     * Constructs a ProductServiceImpl with required dependencies.
     *
     * @param productMapper the product mapper
     */
    public ProductServiceImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    /**
     * Creates a new product.
     *
     * @param productDto the product data
     * @return the created product
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
     * Updates an existing product.
     *
     * @param productDto the product data with updates
     * @return the updated product
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
     * Deletes a product by ID.
     *
     * @param id the product ID
     */
    @Override
    public void deleteProduct(String id) {
        productMapper.deleteProductById(id);
    }

    /**
     * Finds a product by ID.
     *
     * @param id the product ID
     * @return the product data
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
     * Returns all products.
     *
     * @return list of all products
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
