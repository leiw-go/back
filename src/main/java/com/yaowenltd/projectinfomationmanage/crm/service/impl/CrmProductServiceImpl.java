/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmProductMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmProductPriceMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductPriceDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductPriceRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmProductEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmProductPriceEntity;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmProductService;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * CRM 产品目录服务实现.
 *
 * @since 2026-11-08
 */
@Service
public class CrmProductServiceImpl implements CrmProductService {

    /** 列表默认每页条数. */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 列表最大每页条数. */
    private static final int MAX_PAGE_SIZE = 200;

    /** 单次查询允许的最大偏移量（防止深翻页拖垮数据库）. */
    private static final int MAX_PAGE_OFFSET = 10000;

    /** 默认单位. */
    private static final String DEFAULT_UNIT = "PCS";

    /** 默认币种. */
    private static final String DEFAULT_CURRENCY = "USD";

    /** 默认状态：在售. */
    private static final int DEFAULT_STATUS = 1;

    private final CrmProductMapper productMapper;
    private final CrmProductPriceMapper productPriceMapper;

    /**
     * 使用指定的 Mapper 构造 CrmProductServiceImpl.
     *
     * @param productMapper      产品 Mapper
     * @param productPriceMapper 产品参考价 Mapper
     */
    public CrmProductServiceImpl(CrmProductMapper productMapper,
                                 CrmProductPriceMapper productPriceMapper) {
        this.productMapper = productMapper;
        this.productPriceMapper = productPriceMapper;
    }

    @Override
    public CrmProductDto create(CrmProductRequest request) {
        validate(request);
        ensureCodeNotDuplicate(request.getProductCode(), null);

        CrmProductEntity entity = new CrmProductEntity();
        entity.setId(UUID.randomUUID().toString());
        applyRequest(entity, request);
        entity.setDeleted(0L);
        productMapper.insert(entity);
        return toDto(entity);
    }

    @Override
    public CrmProductDto update(String id, CrmProductRequest request) {
        validate(request);
        CrmProductEntity existing = requireProduct(id);
        ensureCodeNotDuplicate(request.getProductCode(), id);

        applyRequest(existing, request);
        productMapper.update(existing);
        return toDto(existing);
    }

    @Override
    public CrmProductDto getById(String id) {
        CrmProductEntity entity = requireProduct(id);
        CrmProductDto dto = toDto(entity);
        dto.setPrices(listPrices(id));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String id) {
        requireProduct(id);
        // 参考价不被其他表引用，删产品时一并清掉
        productPriceMapper.deleteByProductId(id);
        productMapper.softDelete(id, System.currentTimeMillis());
    }

    @Override
    public PageResponse<CrmProductDto> list(CrmProductQueryRequest query) {
        CrmProductQueryRequest actual = query == null ? new CrmProductQueryRequest() : query;
        int page = actual.getPage() == null || actual.getPage() < 1 ? 1 : actual.getPage();
        int size = actual.getSize() == null || actual.getSize() < 1
                ? DEFAULT_PAGE_SIZE : Math.min(actual.getSize(), MAX_PAGE_SIZE);
        if ((long) page * size > MAX_PAGE_OFFSET) {
            throw new IllegalArgumentException("page * size must not exceed " + MAX_PAGE_OFFSET);
        }
        PageHelper.startPage(page, size);
        try {
            PageInfo<CrmProductEntity> info = new PageInfo<>(productMapper.selectPage(actual));

            List<CrmProductDto> rows = new ArrayList<>(info.getList().size());
            for (CrmProductEntity entity : info.getList()) {
                rows.add(toDto(entity));
            }
            PageResponse<CrmProductDto> response = new PageResponse<>();
            response.setData(rows);
            response.setTotal(info.getTotal());
            response.setPage(info.getPageNum());
            response.setSize(info.getPageSize());
            return response;
        } finally {
            PageHelper.clearPage();
        }
    }

    @Override
    public List<CrmProductPriceDto> listPrices(String productId) {
        requireProduct(productId);
        List<CrmProductPriceEntity> entities = productPriceMapper.selectByProductId(productId);
        List<CrmProductPriceDto> rows = new ArrayList<>(entities.size());
        for (CrmProductPriceEntity entity : entities) {
            rows.add(toPriceDto(entity));
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<CrmProductPriceDto> replacePrices(String productId, List<CrmProductPriceRequest> prices) {
        requireProduct(productId);
        List<CrmProductPriceRequest> actual = prices == null ? new ArrayList<>() : prices;
        validatePrices(actual);

        productPriceMapper.deleteByProductId(productId);
        List<CrmProductPriceDto> rows = new ArrayList<>(actual.size());
        for (CrmProductPriceRequest request : actual) {
            CrmProductPriceEntity entity = new CrmProductPriceEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setProductId(productId);
            entity.setCurrencyCode(request.getCurrencyCode());
            entity.setPrice(request.getPrice());
            entity.setMinQuantity(request.getMinQuantity() == null ? 0 : request.getMinQuantity());
            entity.setValidFrom(request.getValidFrom());
            entity.setValidTo(request.getValidTo());
            entity.setDeleted(0L);
            productPriceMapper.insert(entity);
            rows.add(toPriceDto(entity));
        }
        return rows;
    }

    private void validate(CrmProductRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("product request must not be null");
        }
        requireText(request.getProductCode(), "productCode");
        requireText(request.getProductName(), "productName");
        requireNonNegative(request.getCostPrice(), "costPrice");
        requireNonNegative(request.getDefaultPrice(), "defaultPrice");
        requireNonNegative(request.getWeightKg(), "weightKg");
        requireNonNegative(request.getVolumeCbm(), "volumeCbm");
        if (request.getMoq() != null && request.getMoq() < 0) {
            throw new IllegalArgumentException("moq must not be negative");
        }
    }

    private void validatePrices(List<CrmProductPriceRequest> prices) {
        Set<String> seen = new HashSet<>();
        for (CrmProductPriceRequest price : prices) {
            if (price == null) {
                throw new IllegalArgumentException("price item must not be null");
            }
            requireText(price.getCurrencyCode(), "currencyCode");
            if (price.getPrice() == null) {
                throw new IllegalArgumentException("price must not be null");
            }
            requireNonNegative(price.getPrice(), "price");
            if (price.getMinQuantity() != null && price.getMinQuantity() < 0) {
                throw new IllegalArgumentException("minQuantity must not be negative");
            }
            int minQuantity = price.getMinQuantity() == null ? 0 : price.getMinQuantity();
            String key = price.getCurrencyCode() + "#" + minQuantity;
            if (!seen.add(key)) {
                throw new IllegalArgumentException(
                        "duplicated price item: " + price.getCurrencyCode() + " / " + minQuantity);
            }
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    private void requireNonNegative(BigDecimal value, String field) {
        if (value != null && value.signum() < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
    }

    private void ensureCodeNotDuplicate(String productCode, String excludeId) {
        CrmProductEntity existing = productMapper.selectByCode(productCode.trim(), excludeId);
        if (existing != null) {
            throw new IllegalArgumentException("product code already exists: " + productCode.trim());
        }
    }

    private CrmProductEntity requireProduct(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("product id must not be blank");
        }
        CrmProductEntity entity = productMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("product not found: " + id);
        }
        return entity;
    }

    private void applyRequest(CrmProductEntity entity, CrmProductRequest request) {
        entity.setProductCode(request.getProductCode().trim());
        entity.setProductName(request.getProductName().trim());
        entity.setCategory(request.getCategory());
        entity.setSpecification(request.getSpecification());
        entity.setUnit(blankToDefault(request.getUnit(), DEFAULT_UNIT));
        entity.setWeightKg(request.getWeightKg());
        entity.setVolumeCbm(request.getVolumeCbm());
        entity.setCostPrice(request.getCostPrice());
        entity.setCostCurrency(blankToDefault(request.getCostCurrency(), DEFAULT_CURRENCY));
        entity.setDefaultPrice(request.getDefaultPrice());
        entity.setDefaultCurrency(blankToDefault(request.getDefaultCurrency(), DEFAULT_CURRENCY));
        entity.setMoq(request.getMoq());
        entity.setHsCode(request.getHsCode());
        entity.setOrigin(request.getOrigin());
        entity.setProductStatus(request.getProductStatus() == null ? DEFAULT_STATUS : request.getProductStatus());
        entity.setRemark(request.getRemark());
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private CrmProductDto toDto(CrmProductEntity entity) {
        CrmProductDto dto = new CrmProductDto();
        dto.setId(entity.getId());
        dto.setProductCode(entity.getProductCode());
        dto.setProductName(entity.getProductName());
        dto.setCategory(entity.getCategory());
        dto.setSpecification(entity.getSpecification());
        dto.setUnit(entity.getUnit());
        dto.setWeightKg(entity.getWeightKg());
        dto.setVolumeCbm(entity.getVolumeCbm());
        dto.setCostPrice(entity.getCostPrice());
        dto.setCostCurrency(entity.getCostCurrency());
        dto.setDefaultPrice(entity.getDefaultPrice());
        dto.setDefaultCurrency(entity.getDefaultCurrency());
        dto.setMoq(entity.getMoq());
        dto.setHsCode(entity.getHsCode());
        dto.setOrigin(entity.getOrigin());
        dto.setProductStatus(entity.getProductStatus());
        dto.setRemark(entity.getRemark());
        return dto;
    }

    private CrmProductPriceDto toPriceDto(CrmProductPriceEntity entity) {
        CrmProductPriceDto dto = new CrmProductPriceDto();
        dto.setId(entity.getId());
        dto.setProductId(entity.getProductId());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setPrice(entity.getPrice());
        dto.setMinQuantity(entity.getMinQuantity());
        dto.setValidFrom(entity.getValidFrom());
        dto.setValidTo(entity.getValidTo());
        return dto;
    }

}
