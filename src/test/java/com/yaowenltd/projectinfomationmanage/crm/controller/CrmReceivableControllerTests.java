/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.GlobalExceptionHandler;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableAgingDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableDto;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmReceivableService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link CrmReceivableController} 的单元测试.
 *
 * @since 2026-11-08
 */
@ExtendWith(MockitoExtension.class)
class CrmReceivableControllerTests {

    @Mock
    private CrmReceivableService crmReceivableService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CrmReceivableController(crmReceivableService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static CrmReceivableDto dto() {
        CrmReceivableDto dto = new CrmReceivableDto();
        dto.setId("RV1");
        dto.setOrderId("O1");
        dto.setCurrencyCode("USD");
        dto.setReceivableType("FULL");
        dto.setReceivableAmount(new BigDecimal("1260.00"));
        dto.setReceivedAmount(BigDecimal.ZERO);
        dto.setReceivableStatus("UNPAID");
        return dto;
    }

    @Test
    @DisplayName("查询应收列表应返回统一响应体")
    void testListReceivables() throws Exception {
        PageResponse<CrmReceivableDto> page = new PageResponse<>();
        page.setData(Collections.singletonList(dto()));
        page.setTotal(1);
        page.setPage(1);
        page.setSize(20);
        when(crmReceivableService.list(any())).thenReturn(page);

        mockMvc.perform(get("/api/crm/receivables"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.data[0].receivableType").value("FULL"));
    }

    @Test
    @DisplayName("账龄分析应返回统一响应体")
    void testAging() throws Exception {
        CrmReceivableAgingDto aging = new CrmReceivableAgingDto();
        aging.setBuckets(new java.util.ArrayList<>());
        when(crmReceivableService.aging()).thenReturn(aging);

        mockMvc.perform(get("/api/crm/receivables/aging"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.buckets").isEmpty());
    }

    @Test
    @DisplayName("登记收款应返回统一响应体")
    void testWriteOff() throws Exception {
        when(crmReceivableService.writeOff(eq("RV1"), any())).thenReturn(dto());

        mockMvc.perform(post("/api/crm/receivables/RV1/writeoff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":500.00,\"paymentMethod\":\"TT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("RV1"));
    }

}
