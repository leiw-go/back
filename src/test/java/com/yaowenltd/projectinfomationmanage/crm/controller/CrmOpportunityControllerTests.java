/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.GlobalExceptionHandler;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityPipelineDto;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmOpportunityService;
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
 * {@link CrmOpportunityController} 的单元测试。
 * <p>
 * 契约来自需求文档 §4.4：响应体统一用 {@code ResponseResult} 包装，
 * 新建返回 201，其余返回 200。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class CrmOpportunityControllerTests {

    @Mock
    private CrmOpportunityService crmOpportunityService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CrmOpportunityController(crmOpportunityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static CrmOpportunityDto dto() {
        CrmOpportunityDto dto = new CrmOpportunityDto();
        dto.setId("O1");
        dto.setCustomerId("C1");
        dto.setName("2026 展会订单");
        dto.setStage("INITIAL_CONTACT");
        dto.setAmount(new BigDecimal("50000.00"));
        dto.setCurrencyCode("USD");
        dto.setAmountBase(new BigDecimal("50000.00"));
        dto.setWinRate(10);
        dto.setOwnerId("U1");
        return dto;
    }

    @Test
    @DisplayName("查询商机列表应返回统一响应体")
    void testListOpportunities() throws Exception {
        PageResponse<CrmOpportunityDto> page = new PageResponse<>();
        page.setData(Collections.singletonList(dto()));
        page.setTotal(1);
        page.setPage(1);
        page.setSize(20);
        when(crmOpportunityService.list(any())).thenReturn(page);

        mockMvc.perform(get("/api/crm/opportunities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.data[0].stage").value("INITIAL_CONTACT"));
    }

    @Test
    @DisplayName("查询商机详情应返回统一响应体")
    void testGetOpportunity() throws Exception {
        when(crmOpportunityService.getById("O1")).thenReturn(dto());

        mockMvc.perform(get("/api/crm/opportunities/O1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("2026 展会订单"));
    }

    @Test
    @DisplayName("新建商机应返回 201")
    void testCreateOpportunity() throws Exception {
        when(crmOpportunityService.create(any())).thenReturn(dto());

        mockMvc.perform(post("/api/crm/opportunities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"C1\",\"name\":\"2026 展会订单\",\"amount\":50000.00,"
                                + "\"currencyCode\":\"USD\",\"ownerId\":\"U1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.id").value("O1"));
    }

    @Test
    @DisplayName("更新商机应返回统一响应体")
    void testUpdateOpportunity() throws Exception {
        when(crmOpportunityService.update(eq("O1"), any())).thenReturn(dto());

        mockMvc.perform(put("/api/crm/opportunities/O1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"C1\",\"name\":\"2026 展会订单\",\"ownerId\":\"U1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("删除商机应返回统一响应体")
    void testDeleteOpportunity() throws Exception {
        mockMvc.perform(delete("/api/crm/opportunities/O1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(crmOpportunityService).delete("O1");
    }

    @Test
    @DisplayName("推进阶段应返回统一响应体")
    void testChangeStage() throws Exception {
        CrmOpportunityDto dto = dto();
        dto.setStage("QUALIFIED");
        dto.setWinRate(30);
        when(crmOpportunityService.changeStage(eq("O1"), any())).thenReturn(dto);

        mockMvc.perform(post("/api/crm/opportunities/O1/stage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stage\":\"QUALIFIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.stage").value("QUALIFIED"));
    }

    @Test
    @DisplayName("漏斗统计应返回统一响应体")
    void testPipeline() throws Exception {
        CrmOpportunityPipelineDto pipeline = new CrmOpportunityPipelineDto();
        pipeline.setWinRate(BigDecimal.ZERO);
        when(crmOpportunityService.pipeline()).thenReturn(pipeline);

        mockMvc.perform(get("/api/crm/opportunities/pipeline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.winRate").value(0));
    }

    @Test
    @DisplayName("业务异常应被全局异常处理器转成 400")
    void testBusinessExceptionIsMappedToBadRequest() throws Exception {
        when(crmOpportunityService.getById(anyString()))
                .thenThrow(new IllegalArgumentException("opportunity not found: O1"));

        mockMvc.perform(get("/api/crm/opportunities/O1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("opportunity not found: O1"));
    }

}
