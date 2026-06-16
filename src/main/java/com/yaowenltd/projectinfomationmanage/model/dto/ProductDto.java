/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for product information, used for creating and updating products.
 */
@Schema(description = "产品信息")
public class ProductDto {

    @Schema(description = "产品ID")
    private String id;

    @NotBlank(message = "product name cannot be empty")
    @Schema(description = "产品名称", example = "超级大乐透")
    @Size(max = 128, message = "product name length must not exceed 128")
    private String productName;

    @NotBlank(message = "product code cannot be empty")
    @Schema(description = "产品编码", example = "DLT")
    @Size(max = 64, message = "product code length must not exceed 64")
    private String productCode;

    @Schema(description = "产品分类", example = "数字彩")
    @Size(max = 64, message = "category length must not exceed 64")
    private String category;

    @Schema(description = "产品描述", example = "体彩大乐透")
    @Size(max = 512, message = "description length must not exceed 512")
    private String description;

    @Schema(description = "价格", example = "2.00")
    private BigDecimal price;

    @Schema(description = "状态: 1=启用, 0=禁用", example = "1")
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
