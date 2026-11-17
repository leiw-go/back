/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.llm;

import com.yaowenltd.projectinfomationmanage.mapper.llm.LlmProviderMapper;
import com.yaowenltd.projectinfomationmanage.model.entity.LlmProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Provider 配置的内存缓存层（read-through + TTL 30s + 写后失效）.
 * <p>
 * 设计动机：每次 LLM 请求都要查 provider 元数据（baseUrl / apiKey / 是否启用 / 超时覆盖），
 * 直接打 DB 会成热点。缓存在内存里，写操作（管理端 CRUD）显式 {@link #invalidate()} 强制刷新。
 * </p>
 *
 * @since 2026-08-04
 */
@Component
public class ProviderRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderRepository.class);

    /** 缓存有效期. */
    private static final Duration TTL = Duration.ofSeconds(30);

    private final LlmProviderMapper mapper;

    private final ReentrantLock lock = new ReentrantLock();

    /** 单条 provider 缓存（key = provider name）. */
    private final ConcurrentHashMap<String, CacheEntry<LlmProvider>> singleCache = new ConcurrentHashMap<>();

    /** 全部启用 provider 列表缓存（null 表示未加载） */
    private volatile CacheEntry<List<LlmProvider>> listCache;

    public ProviderRepository(LlmProviderMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 按 name 查询 provider（缓存优先）.
     *
     * @param name provider 名
     * @return provider（不存在或 disabled 时返回 empty）
     */
    public Optional<LlmProvider> findEnabledByName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        CacheEntry<LlmProvider> entry = singleCache.computeIfAbsent(name, k -> new CacheEntry<>());
        if (entry.isExpired()) {
            lock.lock();
            try {
                if (entry.isExpired()) {
                    Optional<LlmProvider> loaded = mapper.findProviderByName(name)
                            .filter(p -> p.getEnabled() != null && p.getEnabled() == 1);
                    entry.set(loaded.orElse(null), Instant.now());
                }
            } finally {
                lock.unlock();
            }
        }
        return Optional.ofNullable(entry.value);
    }

    /**
     * 查所有启用的 provider（缓存优先）.
     *
     * @return 启用 provider 列表（按 priority 升序）
     */
    public List<LlmProvider> findAllEnabled() {
        CacheEntry<List<LlmProvider>> entry = listCache;
        if (entry == null || entry.isExpired()) {
            lock.lock();
            try {
                if (listCache == null || listCache.isExpired()) {
                    List<LlmProvider> loaded = mapper.findEnabledProviders();
                    listCache = new CacheEntry<>();
                    listCache.set(loaded, Instant.now());
                }
            } finally {
                lock.unlock();
            }
        }
        return listCache.value == null ? List.of() : listCache.value;
    }

    /**
     * 主动失效缓存 —— 管理端 CRUD 后必须调用.
     */
    public void invalidate() {
        lock.lock();
        try {
            singleCache.clear();
            listCache = null;
            LOGGER.debug("Provider cache invalidated");
        } finally {
            lock.unlock();
        }
    }

    /**
     * 失效单条缓存 —— 单条更新时不必全清.
     *
     * @param name provider 名
     */
    public void invalidate(String name) {
        singleCache.remove(name);
        listCache = null;
    }

    /** 单条缓存 entry —— 不可变 value + 加载时间. */
    private static final class CacheEntry<V> {
        private volatile V value;
        private volatile Instant loadedAt;

        void set(V value, Instant when) {
            this.value = value;
            this.loadedAt = when;
        }

        boolean isExpired() {
            return loadedAt == null || Duration.between(loadedAt, Instant.now()).compareTo(TTL) > 0;
        }
    }
}