/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.GlobalExceptionHandler;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationDto;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmQuotationService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link CrmQuotationController} 的单元测试.
 *
 * @since 2026-11-08
 */
@ExtendWith(MockitoExtension.class)
class CrmQuotationControllerTests {

    @Mock
    private CrmQuotationService crmQuotationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CrmQuotationController(crmQuotationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static CrmQuotationDto dto(String status) {
        CrmQuotationDto dto = new CrmQuotationDto();
        dto.setId("Q1");
        dto.setQuotationNo("QT20261108000001");
        dto.setCustomerId("C1");
        dto.setVersionNo(1);
        dto.setCurrencyCode("USD");
        dto.setIncoterm("FOB");
        dto.setQuotationStatus(status);
        dto.setTotalAmount(new BigDecimal("1260.00"));
        dto.setProfitAmount(new BigDecimal("210.00"));
        dto.setProfitRate(new BigDecimal("16.67"));
        return dto;
    }

    @Test
    @DisplayName("查询报价单列表应返回统一响应体")
    void testListQuotations() throws Exception {
        PageResponse<CrmQuotationDto> page = new PageResponse<>();
        page.setData(Collections.singletonList(dto("DRAFT")));
        page.setTotal(1);
        page.setPage(1);
        page.setSize(20);
        when(crmQuotationService.list(any())).thenReturn(page);

        mockMvc.perform(get("/api/crm/quotations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.data[0].quotationNo").value("QT20261108000001"));
    }

    @Test
    @DisplayName("查询报价单详情应返回统一响应体")
    void testGetQuotation() throws Exception {
        when(crmQuotationService.getById("Q1")).thenReturn(dto("DRAFT"));

        mockMvc.perform(get("/api/crm/quotations/Q1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(1260.00))
                .andExpect(jsonPath("$.data.profitRate").value(16.67));
    }

    @Test
    @DisplayName("新建报价单应返回 201")
    void testCreateQuotation() throws Exception {
        when(crmQuotationService.create(any())).thenReturn(dto("DRAFT"));

        mockMvc.perform(post("/api/crm/quotations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"C1\",\"currencyCode\":\"USD\",\"incoterm\":\"FOB\","
                                + "\"markupRate\":20,\"items\":[{\"productId\":\"P1\",\"quantity\":100}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.id").value("Q1"));
    }

    @Test
    @DisplayName("更新报价单应返回统一响应体")
    void testUpdateQuotation() throws Exception {
        when(crmQuotationService.update(eq("Q1"), any())).thenReturn(dto("DRAFT"));

        mockMvc.perform(put("/api/crm/quotations/Q1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"C1\",\"currencyCode\":\"USD\",\"incoterm\":\"FOB\","
                                + "\"items\":[{\"productId\":\"P1\",\"quantity\":200}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("试算应返回统一响应体")
    void testCalculate() throws Exception {
        when(crmQuotationService.calculate(any())).thenReturn(dto("DRAFT"));

        mockMvc.perform(post("/api/crm/quotations/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"C1\",\"currencyCode\":\"USD\",\"incoterm\":\"FOB\","
                                + "\"markupRate\":20,\"items\":[{\"productId\":\"P1\",\"quantity\":100}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalAmount").value(1260.00));
    }

    @Test
    @DisplayName("重算应返回统一响应体")
    void testRecalculate() throws Exception {
        when(crmQuotationService.recalculate("Q1")).thenReturn(dto("DRAFT"));

        mockMvc.perform(post("/api/crm/quotations/Q1/recalculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("提交发送应返回统一响应体")
    void testSubmit() throws Exception {
        when(crmQuotationService.submit("Q1")).thenReturn(dto("SENT"));

        mockMvc.perform(post("/api/crm/quotations/Q1/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quotationStatus").value("SENT"));
    }

    @Test
    @DisplayName("客户确认应返回统一响应体")
    void testConfirm() throws Exception {
        when(crmQuotationService.confirm("Q1")).thenReturn(dto("CONFIRMED"));

        mockMvc.perform(post("/api/crm/quotations/Q1/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quotationStatus").value("CONFIRMED"));
    }

    @Test
    @DisplayName("客户拒绝应返回统一响应体")
    void testReject() throws Exception {
        when(crmQuotationService.reject(eq("Q1"), anyString())).thenReturn(dto("REJECTED"));

        mockMvc.perform(post("/api/crm/quotations/Q1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rejectReason\":\"价格谈不拢\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quotationStatus").value("REJECTED"));
    }

    @Test
    @DisplayName("业务异常应被全局异常处理器转成 400")
    void testBusinessExceptionIsMappedToBadRequest() throws Exception {
        when(crmQuotationService.getById(anyString()))
                .thenThrow(new IllegalArgumentException("quotation not found: Q1"));

        mockMvc.perform(get("/api/crm/quotations/Q1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("quotation not found: Q1"));
    }

}
