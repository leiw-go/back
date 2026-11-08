/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.GlobalExceptionHandler;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductPriceDto;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmProductService;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link CrmProductController} 的单元测试。
 * <p>
 * 契约来自需求文档 §4.7：7 个端点，响应体统一用 {@code ResponseResult} 包装，
 * 新建返回 201，其余返回 200。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class CrmProductControllerTests {

    @Mock
    private CrmProductService crmProductService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CrmProductController(crmProductService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static CrmProductDto dto() {
        CrmProductDto dto = new CrmProductDto();
        dto.setId("P1");
        dto.setProductCode("P-001");
        dto.setProductName("不锈钢水壶");
        dto.setCostPrice(new BigDecimal("10.500000"));
        dto.setCostCurrency("USD");
        dto.setUnit("PCS");
        dto.setProductStatus(1);
        return dto;
    }

    @Test
    @DisplayName("查询产品列表应返回统一响应体")
    void testListProducts() throws Exception {
        PageResponse<CrmProductDto> page = new PageResponse<>();
        page.setData(Collections.singletonList(dto()));
        page.setTotal(1);
        page.setPage(1);
        page.setSize(20);
        when(crmProductService.list(any())).thenReturn(page);

        mockMvc.perform(get("/api/crm/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(crmProductService).list(any());
    }

    @Test
    @DisplayName("查询产品详情应返回统一响应体")
    void testGetProduct() throws Exception {
        when(crmProductService.getById("P1")).thenReturn(dto());

        mockMvc.perform(get("/api/crm/products/P1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productCode").value("P-001"))
                .andExpect(jsonPath("$.data.costCurrency").value("USD"));
    }

    @Test
    @DisplayName("新建产品应返回 201")
    void testCreateProduct() throws Exception {
        when(crmProductService.create(any())).thenReturn(dto());

        mockMvc.perform(post("/api/crm/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"P-001\",\"productName\":\"不锈钢水壶\","
                                + "\"costPrice\":10.5,\"costCurrency\":\"USD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.id").value("P1"));
    }

    @Test
    @DisplayName("更新产品应返回统一响应体")
    void testUpdateProduct() throws Exception {
        when(crmProductService.update(eq("P1"), any())).thenReturn(dto());

        mockMvc.perform(put("/api/crm/products/P1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"P-001\",\"productName\":\"不锈钢水壶\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("P1"));
    }

    @Test
    @DisplayName("删除产品应返回统一响应体")
    void testDeleteProduct() throws Exception {
        mockMvc.perform(delete("/api/crm/products/P1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(crmProductService).delete("P1");
    }

    @Test
    @DisplayName("查询产品参考价应返回统一响应体")
    void testListPrices() throws Exception {
        CrmProductPriceDto price = new CrmProductPriceDto();
        price.setId("PP1");
        price.setProductId("P1");
        price.setCurrencyCode("EUR");
        price.setPrice(new BigDecimal("11.000000"));
        price.setMinQuantity(0);
        when(crmProductService.listPrices("P1")).thenReturn(Collections.singletonList(price));

        mockMvc.perform(get("/api/crm/products/P1/prices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].currencyCode").value("EUR"));
    }

    @Test
    @DisplayName("批量替换参考价应返回统一响应体")
    void testReplacePrices() throws Exception {
        CrmProductPriceDto price = new CrmProductPriceDto();
        price.setId("PP1");
        price.setProductId("P1");
        price.setCurrencyCode("EUR");
        price.setPrice(new BigDecimal("11.000000"));
        price.setMinQuantity(0);
        when(crmProductService.replacePrices(eq("P1"), any()))
                .thenReturn(Collections.singletonList(price));

        mockMvc.perform(put("/api/crm/products/P1/prices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"currencyCode\":\"EUR\",\"price\":11.000000,\"minQuantity\":0}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].currencyCode").value("EUR"));
    }

    @Test
    @DisplayName("请求体缺失或格式错误应被转成 400 而不是 500")
    void testMalformedBodyIsMappedToBadRequest() throws Exception {
        mockMvc.perform(put("/api/crm/products/P1/prices")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("malformed request body"));
    }

    @Test
    @DisplayName("业务异常应被全局异常处理器转成 400")
    void testBusinessExceptionIsMappedToBadRequest() throws Exception {
        when(crmProductService.getById(anyString()))
                .thenThrow(new IllegalArgumentException("product not found: P1"));

        mockMvc.perform(get("/api/crm/products/P1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("product not found: P1"));
    }

}
