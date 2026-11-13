/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.GlobalExceptionHandler;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.country.CrmCountryInfoDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.country.CrmCountryRefDto;
import com.yaowenltd.projectinfomationmanage.crm.service.CountryReferenceService;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link CrmCountryController} 的单元测试.
 *
 * @since 2026-11-09
 */
@ExtendWith(MockitoExtension.class)
class CrmCountryControllerTests {

    @Mock
    private CountryReferenceService countryReferenceService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CrmCountryController(countryReferenceService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static CrmCountryRefDto ref() {
        CrmCountryRefDto dto = new CrmCountryRefDto();
        dto.setCountryCode("JP");
        dto.setCountryName("Japan");
        dto.setCountryNameZh("日本");
        dto.setTimezone("Asia/Tokyo");
        dto.setCurrencyCode("JPY");
        dto.setIsCommon(1);
        return dto;
    }

    @Test
    @DisplayName("国家列表应返回统一响应体")
    void testList() throws Exception {
        PageResponse<CrmCountryRefDto> page = new PageResponse<>();
        page.setData(Collections.singletonList(ref()));
        page.setTotal(1);
        page.setPage(1);
        page.setSize(20);
        when(countryReferenceService.list(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/crm/countries").param("keyword", "日本"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data[0].countryCode").value("JP"))
                .andExpect(jsonPath("$.data.data[0].timezone").value("Asia/Tokyo"));
    }

    @Test
    @DisplayName("国家信息返回时区、当前时间、货币与对美元人民币的汇率")
    void testInfo() throws Exception {
        CrmCountryInfoDto dto = new CrmCountryInfoDto();
        dto.setCountryCode("JP");
        dto.setCountryName("Japan");
        dto.setCountryNameZh("日本");
        dto.setTimezone("Asia/Tokyo");
        dto.setCurrentTime("2026-11-09T09:30:00+09:00");
        dto.setCurrentTimeText("2026-11-09 09:30:00");
        dto.setUtcOffset("+09:00");
        dto.setHoursFromBeijing(new BigDecimal("1.00"));
        dto.setBeijingOffsetText("比北京时间早 1 小时");
        dto.setCurrencyCode("JPY");
        dto.setCurrencyName("Japanese yen");
        dto.setCurrencySymbol("¥");
        dto.setRateDate("2026-11-08");
        dto.setRateToUsd(new BigDecimal("0.00666667"));
        dto.setRateToCny(new BigDecimal("0.04666667"));
        dto.setRateFromUsd(new BigDecimal("150.00000000"));
        dto.setRateFromCny(new BigDecimal("21.42857143"));
        dto.setRateMissing(false);
        when(countryReferenceService.getInfo("日本")).thenReturn(dto);

        mockMvc.perform(get("/api/crm/countries/info").param("country", "日本"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.countryCode").value("JP"))
                .andExpect(jsonPath("$.data.timezone").value("Asia/Tokyo"))
                .andExpect(jsonPath("$.data.currentTimeText").value("2026-11-09 09:30:00"))
                .andExpect(jsonPath("$.data.currencyCode").value("JPY"))
                .andExpect(jsonPath("$.data.rateToUsd").value(0.00666667))
                .andExpect(jsonPath("$.data.rateToCny").value(0.04666667))
                .andExpect(jsonPath("$.data.rateMissing").value(false));
    }

    @Test
    @DisplayName("查不到国家时返回 400")
    void testCountryNotFound() throws Exception {
        when(countryReferenceService.getInfo(anyString()))
                .thenThrow(new IllegalArgumentException("country not found: xx"));

        mockMvc.perform(get("/api/crm/countries/info").param("country", "xx"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("country not found: xx"));
    }

}
