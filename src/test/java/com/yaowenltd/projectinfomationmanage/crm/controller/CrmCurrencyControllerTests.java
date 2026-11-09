/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.GlobalExceptionHandler;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencyConvertDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencyDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencyRateCheckDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencyRateDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencySyncStatusDto;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmCurrencyService;
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
import java.time.LocalDate;
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
 * {@link CrmCurrencyController} 的单元测试.
 *
 * @since 2026-11-08
 */
@ExtendWith(MockitoExtension.class)
class CrmCurrencyControllerTests {

    @Mock
    private CrmCurrencyService crmCurrencyService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CrmCurrencyController(crmCurrencyService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static CrmCurrencyDto currency() {
        CrmCurrencyDto dto = new CrmCurrencyDto();
        dto.setId("CU1");
        dto.setCurrencyCode("CNY");
        dto.setCurrencyName("Chinese Renminbi Yuan");
        dto.setStatus("NORMAL");
        dto.setCurrencyLevel("CORE");
        return dto;
    }

    @Test
    @DisplayName("币种列表应返回统一响应体")
    void testListCurrencies() throws Exception {
        when(crmCurrencyService.listCurrencies()).thenReturn(Collections.singletonList(currency()));

        mockMvc.perform(get("/api/crm/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].currencyCode").value("CNY"))
                .andExpect(jsonPath("$.data[0].status").value("NORMAL"));
    }

    @Test
    @DisplayName("汇率列表可按币种与日期过滤")
    void testListRates() throws Exception {
        CrmCurrencyRateDto dto = new CrmCurrencyRateDto();
        dto.setQuoteCurrency("CNY");
        dto.setRate(new BigDecimal("6.6635"));
        dto.setRateDate(LocalDate.of(2026, 11, 8));
        dto.setSource("FRANKFURTER");
        dto.setCrossStatus("MATCH");
        when(crmCurrencyService.listRates(any(), any(), any(), any()))
                .thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/crm/currencies/rates")
                        .param("quoteCurrency", "CNY")
                        .param("date", "2026-11-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].quoteCurrency").value("CNY"))
                .andExpect(jsonPath("$.data[0].crossStatus").value("MATCH"));
    }

    @Test
    @DisplayName("手工维护汇率走 PUT")
    void testSaveManualRate() throws Exception {
        CrmCurrencyRateDto dto = new CrmCurrencyRateDto();
        dto.setQuoteCurrency("CNY");
        dto.setRate(new BigDecimal("6.7"));
        dto.setSource("MANUAL");
        when(crmCurrencyService.saveManualRate(any())).thenReturn(dto);

        mockMvc.perform(put("/api/crm/currencies/rates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quoteCurrency\":\"CNY\",\"rate\":6.7,\"rateDate\":\"2026-11-08\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.source").value("MANUAL"));
    }

    @Test
    @DisplayName("金额折算返回汇率与折算值")
    void testConvert() throws Exception {
        CrmCurrencyConvertDto dto = new CrmCurrencyConvertDto();
        dto.setFromCurrency("USD");
        dto.setToCurrency("CNY");
        dto.setAmount(new BigDecimal("100"));
        dto.setRate(new BigDecimal("6.6635"));
        dto.setConvertedAmount(new BigDecimal("666.35"));
        when(crmCurrencyService.convert(eq(new BigDecimal("100")), eq("USD"), eq("CNY"), any()))
                .thenReturn(dto);

        mockMvc.perform(get("/api/crm/currencies/convert")
                        .param("amount", "100")
                        .param("from", "USD")
                        .param("to", "CNY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.toCurrency").value("CNY"))
                .andExpect(jsonPath("$.data.convertedAmount").value(666.35));
    }

    @Test
    @DisplayName("手动触发同步返回批次摘要")
    void testSync() throws Exception {
        CrmCurrencySyncStatusDto dto = new CrmCurrencySyncStatusDto();
        dto.setSyncId("SYNC1");
        dto.setSyncStatus("SUCCESS");
        dto.setSavedCount(150);
        dto.setMatchedCount(150);
        when(crmCurrencyService.sync()).thenReturn(dto);

        mockMvc.perform(post("/api/crm/currencies/sync"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.syncId").value("SYNC1"))
                .andExpect(jsonPath("$.data.savedCount").value(150));
    }

    @Test
    @DisplayName("最近一次同步状态可查询")
    void testSyncStatus() throws Exception {
        CrmCurrencySyncStatusDto dto = new CrmCurrencySyncStatusDto();
        dto.setSyncId("SYNC1");
        dto.setSyncStatus("SUCCESS");
        when(crmCurrencyService.syncStatus()).thenReturn(dto);

        mockMvc.perform(get("/api/crm/currencies/sync-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.syncStatus").value("SUCCESS"));
    }

    @Test
    @DisplayName("交叉比对明细可查询")
    void testVerification() throws Exception {
        CrmCurrencyRateCheckDto dto = new CrmCurrencyRateCheckDto();
        dto.setQuoteCurrency("CNY");
        dto.setCheckResult("MATCH");
        dto.setDeviationRate(new BigDecimal("0.408800"));
        when(crmCurrencyService.verification(anyString(), anyString()))
                .thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/crm/currencies/rates/verification")
                        .param("quoteCurrency", "CNY")
                        .param("checkResult", "MATCH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].quoteCurrency").value("CNY"))
                .andExpect(jsonPath("$.data[0].checkResult").value("MATCH"));
    }

}
