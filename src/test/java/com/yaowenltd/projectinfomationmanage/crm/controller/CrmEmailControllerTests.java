/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.GlobalExceptionHandler;
import com.yaowenltd.projectinfomationmanage.crm.common.CurrentUserResolver;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailCampaignDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailLogDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailPreviewDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailTemplateDto;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmEmailService;
import com.yaowenltd.projectinfomationmanage.mapper.UserMapper;
import com.yaowenltd.projectinfomationmanage.model.entity.User;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link CrmEmailController} 的单元测试.
 *
 * @since 2026-11-08
 */
@ExtendWith(MockitoExtension.class)
class CrmEmailControllerTests {

    @Mock
    private CrmEmailService crmEmailService;

    @Mock
    private UserMapper userMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId("U1");
        user.setUsername("admin");
        lenient().when(userMapper.selectByUsername(anyString())).thenReturn(user);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CrmEmailController(crmEmailService, new CurrentUserResolver(userMapper)))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static CrmEmailTemplateDto template() {
        CrmEmailTemplateDto dto = new CrmEmailTemplateDto();
        dto.setId("T1");
        dto.setName("开发信");
        dto.setSubject("Hello");
        dto.setTemplateType("开发信");
        return dto;
    }

    @Test
    @DisplayName("模板列表应返回统一响应体")
    void testListTemplates() throws Exception {
        when(crmEmailService.listTemplates()).thenReturn(Collections.singletonList(template()));

        mockMvc.perform(get("/api/crm/email/templates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].name").value("开发信"));
    }

    @Test
    @DisplayName("新建模板返回统一响应体")
    void testSaveTemplate() throws Exception {
        when(crmEmailService.saveTemplate(any())).thenReturn(template());

        mockMvc.perform(post("/api/crm/email/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"开发信\",\"subject\":\"Hello\",\"content\":\"Dear\","
                                + "\"templateType\":\"开发信\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value("T1"));
    }

    @Test
    @DisplayName("删除模板")
    void testDeleteTemplate() throws Exception {
        mockMvc.perform(delete("/api/crm/email/templates/T1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("预览渲染返回渲染结果")
    void testPreview() throws Exception {
        CrmEmailPreviewDto dto = new CrmEmailPreviewDto();
        dto.setTemplateId("T1");
        dto.setSubject("Hello ACME");
        dto.setContent("Dear ACME");
        when(crmEmailService.preview(eq("T1"), any())).thenReturn(dto);

        mockMvc.perform(post("/api/crm/email/templates/T1/preview")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customer.company_name\":\"ACME\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subject").value("Hello ACME"));
    }

    @Test
    @DisplayName("群发任务列表")
    void testListCampaigns() throws Exception {
        CrmEmailCampaignDto dto = new CrmEmailCampaignDto();
        dto.setId("C1");
        dto.setName("批量开发信");
        dto.setCampaignStatus("DRAFT");
        when(crmEmailService.listCampaigns(any(), any())).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/crm/email/campaigns")
                        .requestAttr(CurrentUserResolver.AUTH_ATTR_USERNAME, "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].campaignStatus").value("DRAFT"));
    }

    @Test
    @DisplayName("创建群发任务返回 201")
    void testCreateCampaign() throws Exception {
        CrmEmailCampaignDto dto = new CrmEmailCampaignDto();
        dto.setId("C1");
        dto.setCampaignStatus("DRAFT");
        when(crmEmailService.createCampaign(any(), anyString())).thenReturn(dto);

        mockMvc.perform(post("/api/crm/email/campaigns")
                        .requestAttr(CurrentUserResolver.AUTH_ATTR_USERNAME, "admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"批量开发信\",\"templateId\":\"T1\","
                                + "\"targetType\":\"TAG\",\"targetValue\":\"TAG1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.id").value("C1"));
    }

    @Test
    @DisplayName("执行发送")
    void testSendCampaign() throws Exception {
        CrmEmailCampaignDto dto = new CrmEmailCampaignDto();
        dto.setId("C1");
        dto.setCampaignStatus("SENT");
        dto.setSuccessCount(3);
        when(crmEmailService.send("C1")).thenReturn(dto);

        mockMvc.perform(post("/api/crm/email/campaigns/C1/send"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(3));
    }

    @Test
    @DisplayName("发送明细列表")
    void testListLogs() throws Exception {
        CrmEmailLogDto dto = new CrmEmailLogDto();
        dto.setId("L1");
        dto.setEmail("buyer@acme.com");
        dto.setSendStatus("SUCCESS");
        when(crmEmailService.listLogs("C1")).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/crm/email/campaigns/C1/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].email").value("buyer@acme.com"));
    }

}
