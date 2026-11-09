/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.GlobalExceptionHandler;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerContactDto;
import com.yaowenltd.projectinfomationmanage.crm.service.CustomerContactService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link CustomerContactController} 的单元测试.
 *
 * @since 2026-11-08
 */
@ExtendWith(MockitoExtension.class)
class CustomerContactControllerTests {

    @Mock
    private CustomerContactService customerContactService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CustomerContactController(customerContactService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static CustomerContactDto contact() {
        CustomerContactDto dto = new CustomerContactDto();
        dto.setId("CT1");
        dto.setCustomerId("C1");
        dto.setName("Jack");
        dto.setEmail("buyer@acme.com");
        dto.setPhone("+8613800138000");
        dto.setIsPrimary(1);
        return dto;
    }

    @Test
    @DisplayName("客户下的联系人列表应返回统一响应体")
    void testListByCustomer() throws Exception {
        when(customerContactService.listByCustomer("C1")).thenReturn(Collections.singletonList(contact()));

        mockMvc.perform(get("/api/crm/customers/C1/contacts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Jack"))
                .andExpect(jsonPath("$.data[0].email").value("buyer@acme.com"));
    }

    @Test
    @DisplayName("新建联系人返回 201")
    void testCreate() throws Exception {
        when(customerContactService.create(eq("C1"), any())).thenReturn(contact());

        mockMvc.perform(post("/api/crm/customers/C1/contacts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jack\",\"email\":\"buyer@acme.com\","
                                + "\"phone\":\"+8613800138000\",\"isPrimary\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.id").value("CT1"));
    }

    @Test
    @DisplayName("联系人详情")
    void testGetById() throws Exception {
        when(customerContactService.getById("CT1")).thenReturn(contact());

        mockMvc.perform(get("/api/crm/contacts/CT1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("CT1"));
    }

    @Test
    @DisplayName("更新联系人")
    void testUpdate() throws Exception {
        when(customerContactService.update(eq("CT1"), any())).thenReturn(contact());

        mockMvc.perform(put("/api/crm/contacts/CT1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jack\",\"email\":\"buyer@acme.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("CT1"));
    }

    @Test
    @DisplayName("删除联系人")
    void testDelete() throws Exception {
        mockMvc.perform(delete("/api/crm/contacts/CT1"))
                .andExpect(status().isOk());
        verify(customerContactService).delete("CT1");
    }

}
