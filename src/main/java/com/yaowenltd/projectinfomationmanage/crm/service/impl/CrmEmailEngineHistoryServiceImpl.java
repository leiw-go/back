/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmEmailGenerationMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailEngineRewriteRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailGenerationDto;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmEmailGenerationEntity;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmEmailEngineHistoryService;
import com.yaowenltd.projectinfomationmanage.crm.service.email.CrmEmailEngineGenerated;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * CRM 邮件生成历史服务实现.
 *
 * @since 2026-11-18
 */
@Service
public class CrmEmailEngineHistoryServiceImpl implements CrmEmailEngineHistoryService {

    /** 默认每页条数. */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 每页最大条数. */
    private static final int MAX_PAGE_SIZE = 100;

    /** 最大翻页偏移，防止深翻把库拖死. */
    private static final int MAX_PAGE_OFFSET = 10000;

    private final CrmEmailGenerationMapper generationMapper;

    /**
     * 使用指定的依赖构造 CrmEmailEngineHistoryServiceImpl.
     *
     * @param generationMapper 生成历史 Mapper
     */
    public CrmEmailEngineHistoryServiceImpl(CrmEmailGenerationMapper generationMapper) {
        this.generationMapper = generationMapper;
    }

    @Override
    public void save(CrmEmailEngineGenerated generated,
                     CrmEmailEngineRewriteRequest request,
                     String ownerId) {
        if (generated == null) {
            return;
        }
        CrmEmailGenerationEntity entity = new CrmEmailGenerationEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setRequestId(generated.requestId());
        entity.setSubject(generated.subject() == null ? "" : generated.subject());
        entity.setContent(generated.body() == null ? "" : generated.body());
        entity.setLanguage(generated.language());
        entity.setTone(generated.tone());
        entity.setScene(generated.scene());
        entity.setModel(generated.model() == null ? "" : generated.model());
        if (request != null) {
            entity.setSourceContent(request.getContent());
            entity.setCustomInstruction(request.getCustomInstruction());
            entity.setCustomerName(request.getCustomerName());
            entity.setSenderName(request.getSenderName());
            entity.setSenderCompany(request.getSenderCompany());
            entity.setSenderTitle(request.getSenderTitle());
        }
        entity.setElapsedMs(generated.elapsedMs());
        entity.setPromptTokens(generated.promptTokens());
        entity.setCompletionTokens(generated.completionTokens());
        entity.setTotalTokens(generated.totalTokens());
        entity.setOwnerId(ownerId == null ? "" : ownerId);
        entity.setDeleted(0L);
        generationMapper.insert(entity);
    }

    @Override
    public PageResponse<CrmEmailGenerationDto> list(int page, int size) {
        int actualPage = page < 1 ? 1 : page;
        int actualSize = size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        if ((long) actualPage * actualSize > MAX_PAGE_OFFSET) {
            throw new IllegalArgumentException("page * size must not exceed " + MAX_PAGE_OFFSET);
        }
        PageHelper.startPage(actualPage, actualSize);
        try {
            PageInfo<CrmEmailGenerationEntity> info = new PageInfo<>(generationMapper.selectPage());
            List<CrmEmailGenerationDto> rows = new ArrayList<>(info.getList().size());
            for (CrmEmailGenerationEntity entity : info.getList()) {
                rows.add(toDto(entity));
            }
            PageResponse<CrmEmailGenerationDto> response = new PageResponse<>();
            response.setData(rows);
            response.setTotal(info.getTotal());
            response.setPage(info.getPageNum());
            response.setSize(info.getPageSize());
            return response;
        } finally {
            PageHelper.clearPage();
        }
    }

    @Override
    public CrmEmailGenerationDto getById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }
        CrmEmailGenerationEntity entity = generationMapper.selectById(id);
        return entity == null ? null : toDto(entity);
    }

    @Override
    public boolean delete(String id) {
        if (id == null || id.trim().isEmpty()) {
            return false;
        }
        return generationMapper.softDelete(id, System.currentTimeMillis()) > 0;
    }

    /**
     * 实体转 DTO.
     *
     * @param entity 实体
     * @return DTO
     */
    private CrmEmailGenerationDto toDto(CrmEmailGenerationEntity entity) {
        CrmEmailGenerationDto dto = new CrmEmailGenerationDto();
        dto.setId(entity.getId());
        dto.setRequestId(entity.getRequestId());
        dto.setSubject(entity.getSubject());
        dto.setContent(entity.getContent());
        dto.setLanguage(entity.getLanguage());
        dto.setTone(entity.getTone());
        dto.setScene(entity.getScene());
        dto.setModel(entity.getModel());
        dto.setSourceContent(entity.getSourceContent());
        dto.setCustomInstruction(entity.getCustomInstruction());
        dto.setCustomerName(entity.getCustomerName());
        dto.setSenderName(entity.getSenderName());
        dto.setSenderCompany(entity.getSenderCompany());
        dto.setSenderTitle(entity.getSenderTitle());
        dto.setElapsedMs(entity.getElapsedMs());
        dto.setPromptTokens(entity.getPromptTokens());
        dto.setCompletionTokens(entity.getCompletionTokens());
        dto.setTotalTokens(entity.getTotalTokens());
        dto.setOwnerId(entity.getOwnerId());
        dto.setCreateTime(entity.getCreateTime());
        return dto;
    }
}
