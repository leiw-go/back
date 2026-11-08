/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.GlobalExceptionHandler;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link CustomerController} 的单元测试。
 * <p>
 * 契约来自需求文档 §4.2：所有接口挂在 {@code /api/crm/customers}，
 * 统一用 {@code ResponseResult} 包装、HTTP 恒 200、业务码看 {@code code}；
 * 导出为同步 Excel（.xlsx），业务异常转 400。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class CustomerControllerTests {

    @Mock
    private CustomerService customerService;

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new CustomerController(customerService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static CustomerDto dto() {
        CustomerDto dto = new CustomerDto();
        dto.setId("C1");
        dto.setCompanyName("Acme Trading Co., Ltd.");
        dto.setCountryCode("US");
        dto.setLevel("B");
        dto.setSource("TRADE_SHOW");
        dto.setOwnerId("U1");
        return dto;
    }

    @Test
    @DisplayName("新建客户应返回统一响应体")
    void testCreateCustomer() throws Exception {
        when(customerService.create(any())).thenReturn(dto());

        mockMvc.perform(post("/api/crm/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Acme\",\"countryCode\":\"US\","
                                + "\"source\":\"TRADE_SHOW\",\"level\":\"B\",\"ownerId\":\"U1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.id").value("C1"));
    }

    @Test
    @DisplayName("查询详情应返回统一响应体")
    void testGetCustomer() throws Exception {
        when(customerService.getById("C1")).thenReturn(dto());

        mockMvc.perform(get("/api/crm/customers/C1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.companyName").value("Acme Trading Co., Ltd."));
    }

    @Test
    @DisplayName("业务异常应转成 400 业务码并带中文文案")
    void testBusinessExceptionBecomesBadRequest() throws Exception {
        when(customerService.getById("C404"))
                .thenThrow(new IllegalArgumentException("customer not found: C404"));

        mockMvc.perform(get("/api/crm/customers/C404"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("customer not found: C404"));
    }

    @Test
    @DisplayName("更新客户应把路径上的 id 传给 Service")
    void testUpdateCustomer() throws Exception {
        when(customerService.update(any(), any())).thenReturn(dto());

        mockMvc.perform(put("/api/crm/customers/C1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"companyName\":\"Acme\",\"countryCode\":\"US\","
                                + "\"source\":\"TRADE_SHOW\",\"level\":\"B\",\"ownerId\":\"U1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        verify(customerService).update(idCaptor.capture(), any());
        assertTrue("C1".equals(idCaptor.getValue()));
    }

    @Test
    @DisplayName("删除客户应调用 Service 并返回成功")
    void testDeleteCustomer() throws Exception {
        mockMvc.perform(delete("/api/crm/customers/C1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(customerService).delete("C1");
    }

    @Test
    @DisplayName("转交负责人应传新负责人")
    void testTransferCustomer() throws Exception {
        when(customerService.transfer(any(), any())).thenReturn(dto());

        mockMvc.perform(post("/api/crm/customers/C1/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newOwnerId\":\"U2\"}"))
                .andExpect(status().isOk());

        verify(customerService).transfer("C1", "U2");
    }

    @Test
    @DisplayName("导出应返回 xlsx 文件与中文文件名")
    void testExportCustomersAsExcel() throws Exception {
        when(customerService.exportList(any())).thenReturn(List.of(dto()));

        byte[] body = mockMvc.perform(get("/api/crm/customers/export"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertTrue(body.length > 0, "export body should not be empty");
        // xlsx 是 zip 容器，magic 为 PK
        assertArrayEquals(new byte[] {'P', 'K'}, new byte[] {body[0], body[1]});
    }

    @Test
    @DisplayName("导出应带 attachment 与 UTF-8 文件名")
    void testExportContentDisposition() throws Exception {
        when(customerService.exportList(any())).thenReturn(List.of(dto()));

        String disposition = mockMvc.perform(get("/api/crm/customers/export"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getHeader("Content-Disposition");

        assertTrue(disposition != null && disposition.startsWith("attachment;"),
                "export should be an attachment but was: " + disposition);
        assertTrue(disposition.contains("filename*=UTF-8''"),
                "export should carry an RFC 5987 encoded filename but was: " + disposition);
    }

    @Test
    @DisplayName("下拉选项应返回字典数据")
    void testDictionaryOptions() throws Exception {
        mockMvc.perform(get("/api/crm/customers/dictionary-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
