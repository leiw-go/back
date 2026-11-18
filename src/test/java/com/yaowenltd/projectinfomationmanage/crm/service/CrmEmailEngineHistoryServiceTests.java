/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmEmailGenerationMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailEngineRewriteRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailGenerationDto;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmEmailGenerationEntity;
import com.yaowenltd.projectinfomationmanage.crm.service.email.CrmEmailEngineGenerated;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.CrmEmailEngineHistoryServiceImpl;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CrmEmailEngineHistoryService} 的单元测试.
 *
 * @since 2026-11-18
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("邮件生成历史")
class CrmEmailEngineHistoryServiceTests {

    @Mock
    private CrmEmailGenerationMapper generationMapper;

    private CrmEmailEngineHistoryServiceImpl service() {
        return new CrmEmailEngineHistoryServiceImpl(generationMapper);
    }

    private CrmEmailEngineGenerated generated() {
        return new CrmEmailEngineGenerated(
                "req-1", "LED Strip Quotation", "Dear Sir, could you please help me?",
                "minimax-cn:MiniMax-M3.1-Flash", "en", "new_customer", "quotation",
                1234L, 100, 200, 300);
    }

    @Test
    @DisplayName("保存时把生成产物与请求上下文一起落库")
    void saveStoresProductAndContext() {
        CrmEmailEngineRewriteRequest request = new CrmEmailEngineRewriteRequest();
        request.setContent("draft body");
        request.setCustomInstruction("强调交期");
        request.setCustomerName("Acme");
        request.setSenderName("leon");
        request.setSenderTitle("销售业务员");

        service().save(generated(), request, "u-1");

        ArgumentCaptor<CrmEmailGenerationEntity> captor =
                ArgumentCaptor.forClass(CrmEmailGenerationEntity.class);
        verify(generationMapper).insert(captor.capture());

        CrmEmailGenerationEntity entity = captor.getValue();
        assertEquals("LED Strip Quotation", entity.getSubject());
        assertEquals("Dear Sir, could you please help me?", entity.getContent());
        assertEquals("en", entity.getLanguage());
        assertEquals("new_customer", entity.getTone());
        assertEquals("quotation", entity.getScene());
        assertEquals("draft body", entity.getSourceContent());
        assertEquals("强调交期", entity.getCustomInstruction());
        assertEquals("Acme", entity.getCustomerName());
        assertEquals("leon", entity.getSenderName());
        assertEquals("销售业务员", entity.getSenderTitle());
        assertEquals(1234L, entity.getElapsedMs());
        assertEquals(100, entity.getPromptTokens());
        assertEquals(200, entity.getCompletionTokens());
        assertEquals(300, entity.getTotalTokens());
        assertEquals("u-1", entity.getOwnerId());
        assertEquals(0L, entity.getDeleted());
        assertNotNull(entity.getId());
    }

    @Test
    @DisplayName("保存时没有草稿也能落库（直接生成的场景）")
    void saveWithoutDraft() {
        CrmEmailEngineRewriteRequest request = new CrmEmailEngineRewriteRequest();
        request.setContent(null);
        request.setCustomInstruction("直接生成");

        service().save(generated(), request, "u-1");

        ArgumentCaptor<CrmEmailGenerationEntity> captor =
                ArgumentCaptor.forClass(CrmEmailGenerationEntity.class);
        verify(generationMapper).insert(captor.capture());
        assertNull(captor.getValue().getSourceContent());
        assertEquals("直接生成", captor.getValue().getCustomInstruction());
    }

    @Test
    @DisplayName("列表按时间倒排返回")
    void listReturnsRows() {
        CrmEmailGenerationEntity entity = new CrmEmailGenerationEntity();
        entity.setId("g-1");
        entity.setSubject("Hello");
        entity.setContent("Body");
        entity.setLanguage("en");
        entity.setCreateTime(LocalDateTime.now());
        when(generationMapper.selectPage()).thenReturn(List.of(entity));

        PageResponse<CrmEmailGenerationDto> response = service().list(1, 20);

        assertEquals(1, response.getData().size());
        assertEquals("Hello", response.getData().get(0).getSubject());
        assertEquals(1, response.getPage());
        // 没连真实库，PageHelper 不生效 —— 分页元信息以实际返回条数为准
        assertEquals(1, response.getSize());
    }

    @Test
    @DisplayName("按 ID 查详情，查不到返回 null")
    void getById() {
        when(generationMapper.selectById("g-1")).thenReturn(null);
        assertNull(service().getById("g-1"));
        assertNull(service().getById(""));

        CrmEmailGenerationEntity entity = new CrmEmailGenerationEntity();
        entity.setId("g-1");
        entity.setSubject("Hello");
        when(generationMapper.selectById("g-1")).thenReturn(entity);
        assertEquals("Hello", service().getById("g-1").getSubject());
    }

    @Test
    @DisplayName("删除走逻辑删除")
    void deleteSoftDeletes() {
        when(generationMapper.softDelete(eq("g-1"), any())).thenReturn(1);
        assertTrue(service().delete("g-1"));

        when(generationMapper.softDelete(eq("g-2"), any())).thenReturn(0);
        assertFalse(service().delete("g-2"));

        assertFalse(service().delete(""));
    }
}
