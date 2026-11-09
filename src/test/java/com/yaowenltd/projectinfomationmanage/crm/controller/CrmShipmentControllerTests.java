/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.GlobalExceptionHandler;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmShipmentDto;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmShipmentService;
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
 * {@link CrmShipmentController} 的单元测试.
 *
 * @since 2026-11-08
 */
@ExtendWith(MockitoExtension.class)
class CrmShipmentControllerTests {

    @Mock
    private CrmShipmentService crmShipmentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CrmShipmentController(crmShipmentService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static CrmShipmentDto dto() {
        CrmShipmentDto dto = new CrmShipmentDto();
        dto.setId("SH1");
        dto.setOrderId("O1");
        dto.setShippingType("SEA");
        dto.setShippingNo("BL-001");
        dto.setShipmentStatus("SHIPPED");
        return dto;
    }

    @Test
    @DisplayName("查询发货单列表应返回统一响应体")
    void testListShipments() throws Exception {
        PageResponse<CrmShipmentDto> page = new PageResponse<>();
        page.setData(Collections.singletonList(dto()));
        page.setTotal(1);
        page.setPage(1);
        page.setSize(20);
        when(crmShipmentService.list(any())).thenReturn(page);

        mockMvc.perform(get("/api/crm/shipments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.data[0].shippingNo").value("BL-001"));
    }

    @Test
    @DisplayName("新建发货单应返回 201")
    void testCreateShipment() throws Exception {
        when(crmShipmentService.create(any())).thenReturn(dto());

        mockMvc.perform(post("/api/crm/shipments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"O1\",\"shippingType\":\"SEA\",\"shippingNo\":\"BL-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.id").value("SH1"));
    }

    @Test
    @DisplayName("更新发货单应返回统一响应体")
    void testUpdateShipment() throws Exception {
        when(crmShipmentService.update(eq("SH1"), any())).thenReturn(dto());

        mockMvc.perform(put("/api/crm/shipments/SH1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"O1\",\"shippingType\":\"SEA\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(crmShipmentService).update(eq("SH1"), any());
    }

}
