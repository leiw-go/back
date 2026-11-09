/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmEmailCampaignMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmEmailLogMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmEmailTemplateMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerContactMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailCampaignDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailCampaignRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailPreviewDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailTemplateDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailTemplateRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmEmailCampaignEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmEmailLogEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmEmailTemplateEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CustomerContactEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CustomerEntity;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.CrmEmailServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CrmEmailService} 的单元测试.
 * <p>
 * 契约来自需求文档 §3.9：模板 CRUD、群发范围解析、发送记录逐收件人落库。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CrmEmailServiceTests {

    @Mock
    private CrmEmailTemplateMapper templateMapper;

    @Mock
    private CrmEmailCampaignMapper campaignMapper;

    @Mock
    private CrmEmailLogMapper logMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private CustomerContactMapper customerContactMapper;

    private CrmEmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new CrmEmailServiceImpl(templateMapper, campaignMapper, logMapper,
                customerMapper, customerContactMapper);
    }

    private static CrmEmailTemplateEntity template() {
        CrmEmailTemplateEntity entity = new CrmEmailTemplateEntity();
        entity.setId("T1");
        entity.setName("开发信");
        entity.setSubject("Hello {{customer.company_name}}");
        entity.setContent("Dear {{customer.company_name}}, {{product.name}}");
        entity.setLanguage("en");
        entity.setTemplateType("开发信");
        entity.setDeleted(0L);
        return entity;
    }

    private static CustomerEntity customer(String id, String companyName) {
        CustomerEntity entity = new CustomerEntity();
        entity.setId(id);
        entity.setCompanyName(companyName);
        entity.setOwnerId("U1");
        entity.setDeleted(0L);
        return entity;
    }

    private static CustomerContactEntity contact(String id, String customerId, String email) {
        CustomerContactEntity entity = new CustomerContactEntity();
        entity.setId(id);
        entity.setCustomerId(customerId);
        entity.setEmail(email);
        entity.setDeleted(0L);
        return entity;
    }

    @Test
    @DisplayName("新建模板走 insert")
    void testSaveTemplateCreates() {
        CrmEmailTemplateRequest request = new CrmEmailTemplateRequest();
        request.setName("开发信");
        request.setSubject("Hello");
        request.setContent("Dear");
        request.setTemplateType("开发信");

        CrmEmailTemplateDto dto = emailService.saveTemplate(request);

        assertEquals("开发信", dto.getName());
        verify(templateMapper).insert(any(CrmEmailTemplateEntity.class));
    }

    @Test
    @DisplayName("带 id 的模板走 update")
    void testSaveTemplateUpdates() {
        CrmEmailTemplateRequest request = new CrmEmailTemplateRequest();
        request.setId("T1");
        request.setName("开发信");
        request.setSubject("Hello v2");
        request.setContent("Dear");
        request.setTemplateType("开发信");
        when(templateMapper.selectById("T1")).thenReturn(template());

        emailService.saveTemplate(request);

        verify(templateMapper).update(any(CrmEmailTemplateEntity.class));
        verify(templateMapper, never()).insert(any(CrmEmailTemplateEntity.class));
    }

    @Test
    @DisplayName("模板必填项缺失时抛业务异常")
    void testSaveTemplateValidates() {
        CrmEmailTemplateRequest request = new CrmEmailTemplateRequest();
        request.setName("开发信");

        assertThrows(IllegalArgumentException.class, () -> emailService.saveTemplate(request));
    }

    @Test
    @DisplayName("预览渲染替换占位符并标出未解析的占位符")
    void testPreviewRendersPlaceholders() {
        when(templateMapper.selectById("T1")).thenReturn(template());
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("customer.company_name", "ACME");

        CrmEmailPreviewDto dto = emailService.preview("T1", placeholders);

        assertEquals("Hello ACME", dto.getSubject());
        assertTrue(dto.getContent().contains("Dear ACME"));
        assertTrue(dto.getContent().contains("{{product.name}}"));
        assertTrue(dto.getUnresolvedPlaceholders().contains("product.name"));
    }

    @Test
    @DisplayName("删除模板是逻辑删除")
    void testDeleteTemplate() {
        when(templateMapper.selectById("T1")).thenReturn(template());

        emailService.deleteTemplate("T1");

        verify(templateMapper).softDelete(eq("T1"), any(Long.class));
    }

    @Test
    @DisplayName("创建群发任务校验范围类型")
    void testCreateCampaignValidatesTargetType() {
        CrmEmailCampaignRequest request = new CrmEmailCampaignRequest();
        request.setName("批量开发信");
        request.setTemplateId("T1");
        request.setTargetType("ALL");
        request.setTargetValue("X");
        when(templateMapper.selectById("T1")).thenReturn(template());

        assertThrows(IllegalArgumentException.class, () -> emailService.createCampaign(request, "U1"));
    }

    @Test
    @DisplayName("创建群发任务落库为 DRAFT")
    void testCreateCampaign() {
        CrmEmailCampaignRequest request = new CrmEmailCampaignRequest();
        request.setName("批量开发信");
        request.setTemplateId("T1");
        request.setTargetType("TAG");
        request.setTargetValue("TAG1");
        when(templateMapper.selectById("T1")).thenReturn(template());

        CrmEmailCampaignDto dto = emailService.createCampaign(request, "U1");

        assertEquals("DRAFT", dto.getCampaignStatus());
        assertEquals("TAG", dto.getTargetType());
        verify(campaignMapper).insert(any(CrmEmailCampaignEntity.class));
    }

    @Test
    @DisplayName("发送群发：每个有邮箱的联系人写一条发送记录")
    void testSendWritesOneLogPerContact() {
        CrmEmailCampaignEntity campaign = new CrmEmailCampaignEntity();
        campaign.setId("C1");
        campaign.setTemplateId("T1");
        campaign.setTargetType("CUSTOMER");
        campaign.setTargetValue("CU1");
        campaign.setCampaignStatus("DRAFT");
        when(campaignMapper.selectById("C1")).thenReturn(campaign);
        when(templateMapper.selectById("T1")).thenReturn(template());
        when(customerMapper.selectById("CU1")).thenReturn(customer("CU1", "ACME"));
        when(customerContactMapper.selectByCustomerId("CU1")).thenReturn(List.of(
                contact("CT1", "CU1", "buyer@acme.com"),
                contact("CT2", "CU1", null),
                contact("CT3", "CU1", "buyer@acme.com")));

        CrmEmailCampaignDto dto = emailService.send("C1");

        ArgumentCaptor<List<CrmEmailLogEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(logMapper).insertBatch(captor.capture());
        List<CrmEmailLogEntity> logs = captor.getValue();
        assertEquals(1, logs.size(), "没有邮箱的联系人跳过，重复邮箱去重");
        assertEquals("buyer@acme.com", logs.get(0).getEmail());
        assertEquals("SENT", dto.getCampaignStatus());
        assertEquals(1, dto.getSuccessCount());
    }

    @Test
    @DisplayName("已发送的群发任务不能重复发送")
    void testSendRejectsAlreadySent() {
        CrmEmailCampaignEntity campaign = new CrmEmailCampaignEntity();
        campaign.setId("C1");
        campaign.setTemplateId("T1");
        campaign.setCampaignStatus("SENT");
        when(campaignMapper.selectById("C1")).thenReturn(campaign);

        assertThrows(IllegalArgumentException.class, () -> emailService.send("C1"));
    }

    @Test
    @DisplayName("发送记录可按群发任务查询")
    void testListLogs() {
        CrmEmailCampaignEntity campaign = new CrmEmailCampaignEntity();
        campaign.setId("C1");
        when(campaignMapper.selectById("C1")).thenReturn(campaign);
        CrmEmailLogEntity log = new CrmEmailLogEntity();
        log.setId("L1");
        log.setCampaignId("C1");
        log.setEmail("buyer@acme.com");
        log.setSendStatus("SUCCESS");
        when(logMapper.selectByCampaignId("C1")).thenReturn(Collections.singletonList(log));

        assertEquals(1, emailService.listLogs("C1").size());
    }

}
