/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmProductMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmProductPriceMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductPriceDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductPriceRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmProductEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmProductPriceEntity;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.CrmProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CrmProductService} 的单元测试。
 * <p>
 * 契约来自需求文档 §3.6、§5.3.5、§5.3.6：
 * 产品编码全局唯一；缺成本价的产品不允许有"负成本"之类的脏数据；
 * 数值字段不允许为负；多币种参考价走"整批替换"语义，且同一
 * (币种, 阶梯起点) 不允许重复（否则会撞唯一索引）；
 * 参考价只是参考，**不是报价取价来源**。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class CrmProductServiceTests {

    @Mock
    private CrmProductMapper productMapper;

    @Mock
    private CrmProductPriceMapper productPriceMapper;

    private CrmProductService crmProductService;

    @BeforeEach
    void setUp() {
        crmProductService = new CrmProductServiceImpl(productMapper, productPriceMapper);
    }

    private static CrmProductRequest request() {
        CrmProductRequest request = new CrmProductRequest();
        request.setProductCode("P-001");
        request.setProductName("不锈钢水壶");
        request.setCostPrice(new BigDecimal("10.500000"));
        request.setCostCurrency("USD");
        request.setMoq(100);
        return request;
    }

    private static CrmProductEntity existing() {
        CrmProductEntity entity = new CrmProductEntity();
        entity.setId("P1");
        entity.setProductCode("P-001");
        entity.setProductName("不锈钢水壶");
        entity.setUnit("PCS");
        entity.setCostCurrency("USD");
        entity.setDefaultCurrency("USD");
        entity.setProductStatus(1);
        entity.setDeleted(0L);
        return entity;
    }

    private static CrmProductPriceEntity priceEntity(String currency, int minQuantity) {
        CrmProductPriceEntity entity = new CrmProductPriceEntity();
        entity.setId("PP-" + currency + "-" + minQuantity);
        entity.setProductId("P1");
        entity.setCurrencyCode(currency);
        entity.setPrice(new BigDecimal("12.500000"));
        entity.setMinQuantity(minQuantity);
        entity.setDeleted(0L);
        return entity;
    }

    @Test
    @DisplayName("新建产品应补齐默认值并落库")
    void testCreateFillsDefaults() {
        when(productMapper.selectByCode(eq("P-001"), isNull())).thenReturn(null);

        CrmProductDto dto = crmProductService.create(request());

        ArgumentCaptor<CrmProductEntity> captor = ArgumentCaptor.forClass(CrmProductEntity.class);
        verify(productMapper).insert(captor.capture());
        CrmProductEntity saved = captor.getValue();
        assertNotNull(saved.getId());
        assertEquals("PCS", saved.getUnit());
        assertEquals("USD", saved.getCostCurrency());
        assertEquals("USD", saved.getDefaultCurrency());
        assertEquals(1, saved.getProductStatus());
        assertEquals(0L, saved.getDeleted());
        assertEquals("P-001", dto.getProductCode());
    }

    @Test
    @DisplayName("新建产品时产品编码不能为空")
    void testCreateRejectsBlankCode() {
        CrmProductRequest request = request();
        request.setProductCode("  ");

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> crmProductService.create(request));
        assertTrue(error.getMessage().contains("productCode"));
        verify(productMapper, never()).insert(any());
    }

    @Test
    @DisplayName("新建产品时产品名称不能为空")
    void testCreateRejectsBlankName() {
        CrmProductRequest request = request();
        request.setProductName(null);

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> crmProductService.create(request));
        assertTrue(error.getMessage().contains("productName"));
    }

    @Test
    @DisplayName("产品编码重复时不允许新建")
    void testCreateRejectsDuplicatedCode() {
        when(productMapper.selectByCode(eq("P-001"), isNull())).thenReturn(existing());

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> crmProductService.create(request()));
        assertTrue(error.getMessage().contains("already exists"));
        verify(productMapper, never()).insert(any());
    }

    @Test
    @DisplayName("成本价为负时不允许新建")
    void testCreateRejectsNegativeCostPrice() {
        CrmProductRequest request = request();
        request.setCostPrice(new BigDecimal("-0.01"));

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> crmProductService.create(request));
        assertTrue(error.getMessage().contains("costPrice"));
    }

    @Test
    @DisplayName("重量、体积、默认售价为负或起订量为负时不允许新建")
    void testCreateRejectsNegativeNumbers() {
        CrmProductRequest negativeWeight = request();
        negativeWeight.setWeightKg(new BigDecimal("-1"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> crmProductService.create(negativeWeight)).getMessage().contains("weightKg"));

        CrmProductRequest negativeVolume = request();
        negativeVolume.setVolumeCbm(new BigDecimal("-1"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> crmProductService.create(negativeVolume)).getMessage().contains("volumeCbm"));

        CrmProductRequest negativePrice = request();
        negativePrice.setDefaultPrice(new BigDecimal("-1"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> crmProductService.create(negativePrice)).getMessage().contains("defaultPrice"));

        CrmProductRequest negativeMoq = request();
        negativeMoq.setMoq(-1);
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> crmProductService.create(negativeMoq)).getMessage().contains("moq"));
    }

    @Test
    @DisplayName("更新不存在的产品应报错")
    void testUpdateRejectsMissingProduct() {
        when(productMapper.selectById("P1")).thenReturn(null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmProductService.update("P1", request()));
        assertTrue(error.getMessage().contains("not found"));
    }

    @Test
    @DisplayName("更新时排除自身后编码不冲突则允许保存")
    void testUpdateAllowsSameCodeOnSelf() {
        when(productMapper.selectById("P1")).thenReturn(existing());
        when(productMapper.selectByCode(eq("P-001"), eq("P1"))).thenReturn(null);

        CrmProductRequest request = request();
        request.setProductName("不锈钢水壶 2L");

        CrmProductDto dto = crmProductService.update("P1", request);

        verify(productMapper).update(any());
        assertEquals("不锈钢水壶 2L", dto.getProductName());
    }

    @Test
    @DisplayName("更新时编码被别的产品占用应报错")
    void testUpdateRejectsDuplicatedCode() {
        when(productMapper.selectById("P1")).thenReturn(existing());
        when(productMapper.selectByCode(eq("P-001"), eq("P1"))).thenReturn(existing());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmProductService.update("P1", request()));
        assertTrue(error.getMessage().contains("already exists"));
        verify(productMapper, never()).update(any());
    }

    @Test
    @DisplayName("查询详情应带上各币种参考价")
    void testGetByIdIncludesPrices() {
        when(productMapper.selectById("P1")).thenReturn(existing());
        when(productPriceMapper.selectByProductId("P1"))
                .thenReturn(Arrays.asList(priceEntity("EUR", 0), priceEntity("USD", 0)));

        CrmProductDto dto = crmProductService.getById("P1");

        assertEquals(2, dto.getPrices().size());
        assertEquals("EUR", dto.getPrices().get(0).getCurrencyCode());
    }

    @Test
    @DisplayName("查询不存在的产品详情应报错")
    void testGetByIdRejectsMissingProduct() {
        when(productMapper.selectById("P1")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> crmProductService.getById("P1"));
    }

    @Test
    @DisplayName("删除产品应先清理参考价再逻辑删除主体")
    void testDeleteRemovesPricesFirst() {
        when(productMapper.selectById("P1")).thenReturn(existing());

        crmProductService.delete("P1");

        InOrder order = inOrder(productPriceMapper, productMapper);
        order.verify(productPriceMapper).deleteByProductId("P1");
        order.verify(productMapper).softDelete(eq("P1"), anyLong());
    }

    @Test
    @DisplayName("删除不存在的产品应报错")
    void testDeleteRejectsMissingProduct() {
        when(productMapper.selectById("P1")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> crmProductService.delete("P1"));
        verify(productMapper, never()).softDelete(anyString(), anyLong());
    }

    @Test
    @DisplayName("批量替换参考价应先删后插并回填产品 ID")
    void testReplacePricesDeletesThenInserts() {
        when(productMapper.selectById("P1")).thenReturn(existing());

        CrmProductPriceRequest first = new CrmProductPriceRequest();
        first.setCurrencyCode("EUR");
        first.setPrice(new BigDecimal("11.000000"));
        first.setMinQuantity(500);

        List<CrmProductPriceDto> rows = crmProductService.replacePrices("P1", Collections.singletonList(first));

        verify(productPriceMapper).deleteByProductId("P1");
        ArgumentCaptor<CrmProductPriceEntity> captor =
                ArgumentCaptor.forClass(CrmProductPriceEntity.class);
        verify(productPriceMapper).insert(captor.capture());
        assertEquals("P1", captor.getValue().getProductId());
        assertEquals("EUR", captor.getValue().getCurrencyCode());
        assertEquals(500, captor.getValue().getMinQuantity());
        assertEquals(1, rows.size());
        assertEquals("EUR", rows.get(0).getCurrencyCode());
    }

    @Test
    @DisplayName("批量替换参考价时币种与阶梯起点重复应报错")
    void testReplacePricesRejectsDuplicatedItem() {
        when(productMapper.selectById("P1")).thenReturn(existing());

        CrmProductPriceRequest first = new CrmProductPriceRequest();
        first.setCurrencyCode("EUR");
        first.setPrice(new BigDecimal("11.000000"));
        first.setMinQuantity(0);

        CrmProductPriceRequest second = new CrmProductPriceRequest();
        second.setCurrencyCode("EUR");
        second.setPrice(new BigDecimal("12.000000"));
        second.setMinQuantity(0);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmProductService.replacePrices("P1", Arrays.asList(first, second)));
        assertTrue(error.getMessage().contains("duplicated"));
        verify(productPriceMapper, never()).insert(any());
    }

    @Test
    @DisplayName("批量替换参考价时币种不能为空、价格不能为负")
    void testReplacePricesValidatesItems() {
        when(productMapper.selectById("P1")).thenReturn(existing());

        CrmProductPriceRequest blankCurrency = new CrmProductPriceRequest();
        blankCurrency.setPrice(new BigDecimal("11.000000"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> crmProductService.replacePrices("P1", Collections.singletonList(blankCurrency)))
                .getMessage().contains("currencyCode"));

        CrmProductPriceRequest negativePrice = new CrmProductPriceRequest();
        negativePrice.setCurrencyCode("EUR");
        negativePrice.setPrice(new BigDecimal("-1"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> crmProductService.replacePrices("P1", Collections.singletonList(negativePrice)))
                .getMessage().contains("price"));
    }

    @Test
    @DisplayName("批量替换参考价传空列表表示清空")
    void testReplacePricesWithEmptyListClears() {
        when(productMapper.selectById("P1")).thenReturn(existing());

        List<CrmProductPriceDto> rows = crmProductService.replacePrices("P1", new ArrayList<>());

        verify(productPriceMapper).deleteByProductId("P1");
        verify(productPriceMapper, never()).insert(any());
        assertTrue(rows.isEmpty());
    }

    @Test
    @DisplayName("批量替换参考价时产品必须存在")
    void testReplacePricesRejectsMissingProduct() {
        when(productMapper.selectById("P1")).thenReturn(null);

        assertThrows(IllegalArgumentException.class,
                () -> crmProductService.replacePrices("P1", new ArrayList<>()));
    }

    @Test
    @DisplayName("分页查询应把结果映射成 DTO")
    void testListMapsRows() {
        when(productMapper.selectPage(any())).thenReturn(Collections.singletonList(existing()));

        CrmProductQueryRequest query = new CrmProductQueryRequest();
        query.setPage(1);
        query.setSize(10);

        List<CrmProductDto> rows = crmProductService.list(query).getData();
        assertEquals(1, rows.size());
        assertEquals("P-001", rows.get(0).getProductCode());
        assertNull(rows.get(0).getPrices());
    }

    @Test
    @DisplayName("分页查询参数超出上限时拒绝")
    void testListRejectsTooDeepPaging() {
        CrmProductQueryRequest query = new CrmProductQueryRequest();
        query.setPage(1000);
        query.setSize(200);

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> crmProductService.list(query));
        assertTrue(error.getMessage().contains("page * size"));
    }

}
