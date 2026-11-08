/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.GlobalExceptionHandler;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.inquiry.CrmInquiryDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationDto;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmInquiryService;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link CrmInquiryController} 的单元测试.
 *
 * @since 2026-11-08
 */
@ExtendWith(MockitoExtension.class)
class CrmInquiryControllerTests {

    @Mock
    private CrmInquiryService crmInquiryService;

    @Mock
    private CrmQuotationService crmQuotationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new CrmInquiryController(crmInquiryService, crmQuotationService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static CrmInquiryDto dto() {
        CrmInquiryDto dto = new CrmInquiryDto();
        dto.setId("I1");
        dto.setCustomerId("C1");
        dto.setInquiryNo("INQ20261108000001");
        dto.setSource("EMAIL");
        dto.setInquiryStatus("NEW");
        dto.setCurrencyCode("USD");
        return dto;
    }

    @Test
    @DisplayName("查询询盘列表应返回统一响应体")
    void testListInquiries() throws Exception {
        PageResponse<CrmInquiryDto> page = new PageResponse<>();
        page.setData(Collections.singletonList(dto()));
        page.setTotal(1);
        page.setPage(1);
        page.setSize(20);
        when(crmInquiryService.list(any())).thenReturn(page);

        mockMvc.perform(get("/api/crm/inquiries"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.data[0].inquiryNo").value("INQ20261108000001"));
    }

    @Test
    @DisplayName("查询询盘详情应返回统一响应体")
    void testGetInquiry() throws Exception {
        when(crmInquiryService.getById("I1")).thenReturn(dto());

        mockMvc.perform(get("/api/crm/inquiries/I1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value("EMAIL"));
    }

    @Test
    @DisplayName("新建询盘应返回 201")
    void testCreateInquiry() throws Exception {
        when(crmInquiryService.create(any())).thenReturn(dto());

        mockMvc.perform(post("/api/crm/inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"C1\",\"source\":\"EMAIL\",\"ownerId\":\"U1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.id").value("I1"));
    }

    @Test
    @DisplayName("更新询盘应返回统一响应体")
    void testUpdateInquiry() throws Exception {
        when(crmInquiryService.update(eq("I1"), any())).thenReturn(dto());

        mockMvc.perform(put("/api/crm/inquiries/I1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"C1\",\"source\":\"EMAIL\",\"ownerId\":\"U1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("基于询盘建报价单应返回 201")
    void testCreateQuotationFromInquiry() throws Exception {
        CrmQuotationDto quotation = new CrmQuotationDto();
        quotation.setId("Q1");
        quotation.setInquiryId("I1");
        quotation.setVersionNo(1);
        quotation.setQuotationStatus("DRAFT");
        when(crmQuotationService.createFromInquiry(eq("I1"), any())).thenReturn(quotation);

        mockMvc.perform(post("/api/crm/inquiries/I1/quotations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currencyCode\":\"USD\",\"incoterm\":\"FOB\","
                                + "\"items\":[{\"productId\":\"P1\",\"quantity\":100}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.versionNo").value(1));
        verify(crmQuotationService).createFromInquiry(eq("I1"), any());
    }

    @Test
    @DisplayName("业务异常应被全局异常处理器转成 400")
    void testBusinessExceptionIsMappedToBadRequest() throws Exception {
        when(crmInquiryService.getById("I1"))
                .thenThrow(new IllegalArgumentException("inquiry not found: I1"));

        mockMvc.perform(get("/api/crm/inquiries/I1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("inquiry not found: I1"));
    }

}
