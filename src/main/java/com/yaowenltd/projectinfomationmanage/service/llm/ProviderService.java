/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.llm;

import com.yaowenltd.projectinfomationmanage.mapper.llm.LlmProviderMapper;
import com.yaowenltd.projectinfomationmanage.model.dto.LlmProviderDto;
import com.yaowenltd.projectinfomationmanage.model.entity.LlmProvider;
import com.yaowenltd.projectinfomationmanage.service.llm.crypto.AesGcmEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Provider 管理端 CRUD 服务 —— 写入时对 api_key 做 AES-GCM 加密，写完后失效缓存.
 *
 * @since 2026-08-04
 */
@Service
public class ProviderService {

    private final LlmProviderMapper mapper;
    private final AesGcmEncryptor encryptor;
    private final ProviderRepository repository;

    public ProviderService(LlmProviderMapper mapper,
                           AesGcmEncryptor encryptor,
                           ProviderRepository repository) {
        this.mapper = mapper;
        this.encryptor = encryptor;
        this.repository = repository;
    }

    /**
     * 查询全部 provider（响应里不返回 api_key 密文）.
     *
     * @return provider 列表（已脱敏）
     */
    public List<LlmProviderDto> findAll() {
        return mapper.findAllProviders().stream()
                .map(this::toMaskedDto)
                .toList();
    }

    /**
     * 创建 provider；api_key 明文通过 DTO 传入，加密后落库.
     *
     * @param dto 入参（必须含 name / baseUrl / apiKey）
     * @return 创建后的 DTO（不含 apiKey 明文）
     */
    @Transactional
    public LlmProviderDto create(LlmProviderDto dto) {
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("provider name 必填");
        }
        if (dto.getBaseUrl() == null || dto.getBaseUrl().isBlank()) {
            throw new IllegalArgumentException("provider baseUrl 必填");
        }
        if (dto.getApiKey() == null || dto.getApiKey().isBlank()) {
            throw new IllegalArgumentException("provider apiKey 必填");
        }

        LlmProvider entity = new LlmProvider();
        entity.setId(UUID.randomUUID().toString());
        entity.setName(dto.getName());
        entity.setBaseUrl(dto.getBaseUrl());
        entity.setApiKeyCiphertext(encryptor.encrypt(dto.getApiKey()));
        entity.setEnabled(dto.getEnabled() == null ? 1 : dto.getEnabled());
        entity.setPriority(dto.getPriority() == null ? 100 : dto.getPriority());
        entity.setTimeoutMsOverride(dto.getTimeoutMsOverride());

        mapper.insertProvider(entity);
        repository.invalidate(entity.getName());

        return toMaskedDto(entity);
    }

    /**
     * 更新 provider（apiKey 为空则不修改密文列）.
     *
     * @param dto 入参（必须含 id）
     * @return 更新后的 DTO（不含 apiKey 明文）
     */
    @Transactional
    public LlmProviderDto update(LlmProviderDto dto) {
        if (dto.getId() == null || dto.getId().isBlank()) {
            throw new IllegalArgumentException("provider id 必填");
        }
        LlmProvider existing = mapper.findProviderById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("provider 不存在: " + dto.getId()));

        existing.setName(dto.getName() == null ? existing.getName() : dto.getName());
        existing.setBaseUrl(dto.getBaseUrl() == null ? existing.getBaseUrl() : dto.getBaseUrl());
        existing.setEnabled(dto.getEnabled() == null ? existing.getEnabled() : dto.getEnabled());
        existing.setPriority(dto.getPriority() == null ? existing.getPriority() : dto.getPriority());
        existing.setTimeoutMsOverride(dto.getTimeoutMsOverride());

        mapper.updateProvider(existing);

        // 单独更新 api_key 密文（如果有新值）
        if (dto.getApiKey() != null && !dto.getApiKey().isBlank()) {
            mapper.updateApiKey(existing.getId(), encryptor.encrypt(dto.getApiKey()));
        }

        repository.invalidate(existing.getName());
        return toMaskedDto(existing);
    }

    /**
     * 删除 provider.
     *
     * @param id 主键
     */
    @Transactional
    public void delete(String id) {
        LlmProvider existing = mapper.findProviderById(id)
                .orElseThrow(() -> new IllegalArgumentException("provider 不存在: " + id));
        mapper.deleteProviderById(id);
        repository.invalidate(existing.getName());
    }

    /**
     * 把 entity 转成不包含密文列的 DTO（response 脱敏）.
     *
     * @param entity provider 实体
     * @return DTO（apiKey 字段为 null）
     */
    private LlmProviderDto toMaskedDto(LlmProvider entity) {
        LlmProviderDto dto = new LlmProviderDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setBaseUrl(entity.getBaseUrl());
        dto.setApiKey(null); // 不在 response 里回传密文，也不回传明文
        dto.setEnabled(entity.getEnabled());
        dto.setPriority(entity.getPriority());
        dto.setTimeoutMsOverride(entity.getTimeoutMsOverride());
        return dto;
    }
}