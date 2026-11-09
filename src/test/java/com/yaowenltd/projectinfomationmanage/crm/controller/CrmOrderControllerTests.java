/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.GlobalExceptionHandler;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmOrderDto;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmOrderService;
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
 * {@link CrmOrderController} 的单元测试.
 *
 * @since 2026-11-08
 */
@ExtendWith(MockitoExtension.class)
class CrmOrderControllerTests {

    @Mock
    private CrmOrderService crmOrderService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CrmOrderController(crmOrderService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static CrmOrderDto dto() {
        CrmOrderDto dto = new CrmOrderDto();
        dto.setId("O1");
        dto.setOrderNo("ORD20261108000001");
        dto.setCustomerId("C1");
        dto.setCurrencyCode("USD");
        dto.setOrderStatus("CONFIRMED");
        dto.setTotalAmount(new BigDecimal("1260.00"));
        return dto;
    }

    @Test
    @DisplayName("查询订单列表应返回统一响应体")
    void testListOrders() throws Exception {
        PageResponse<CrmOrderDto> page = new PageResponse<>();
        page.setData(Collections.singletonList(dto()));
        page.setTotal(1);
        page.setPage(1);
        page.setSize(20);
        when(crmOrderService.list(any())).thenReturn(page);

        mockMvc.perform(get("/api/crm/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.data[0].orderNo").value("ORD20261108000001"));
    }

    @Test
    @DisplayName("查询订单详情应返回统一响应体")
    void testGetOrder() throws Exception {
        when(crmOrderService.getById("O1")).thenReturn(dto());

        mockMvc.perform(get("/api/crm/orders/O1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.totalAmount").value(1260.00));
    }

    @Test
    @DisplayName("新建订单应返回 201")
    void testCreateOrder() throws Exception {
        when(crmOrderService.create(any())).thenReturn(dto());

        mockMvc.perform(post("/api/crm/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"C1\",\"currencyCode\":\"USD\",\"incoterm\":\"FOB\","
                                + "\"ownerId\":\"U1\",\"items\":[{\"productId\":\"P1\",\"quantity\":100,"
                                + "\"unitPrice\":12.6,\"amount\":1260.00}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.id").value("O1"));
    }

    @Test
    @DisplayName("更新订单应返回统一响应体")
    void testUpdateOrder() throws Exception {
        when(crmOrderService.update(eq("O1"), any())).thenReturn(dto());

        mockMvc.perform(put("/api/crm/orders/O1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":\"C1\",\"currencyCode\":\"USD\",\"ownerId\":\"U1\","
                                + "\"items\":[{\"productId\":\"P1\",\"quantity\":100,\"amount\":1260.00}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("删除订单应返回统一响应体")
    void testDeleteOrder() throws Exception {
        mockMvc.perform(delete("/api/crm/orders/O1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(crmOrderService).delete("O1");
    }

    @Test
    @DisplayName("登记发货应返回统一响应体")
    void testShipOrder() throws Exception {
        when(crmOrderService.ship(eq("O1"), any())).thenReturn(dto());

        mockMvc.perform(post("/api/crm/orders/O1/ship")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shippingType\":\"SEA\",\"shippingNo\":\"BL-001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("登记收款应返回统一响应体")
    void testReceiveOrder() throws Exception {
        when(crmOrderService.receive(eq("O1"), any())).thenReturn(dto());

        mockMvc.perform(post("/api/crm/orders/O1/receive")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receivableId\":\"RV1\",\"amount\":500.00,\"paymentMethod\":\"TT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("业务异常应被全局异常处理器转成 400")
    void testBusinessExceptionIsMappedToBadRequest() throws Exception {
        when(crmOrderService.getById(anyString()))
                .thenThrow(new IllegalArgumentException("order not found: O1"));

        mockMvc.perform(get("/api/crm/orders/O1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("order not found: O1"));
    }

}
