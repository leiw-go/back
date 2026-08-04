/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.mapper.llm;

import com.yaowenltd.projectinfomationmanage.model.entity.LlmProvider;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 上游 LLM provider 配置 Mapper，对应表 {@code t_llm_provider}.
 *
 * @since 2026-08-04
 */
@Mapper
public interface LlmProviderMapper {

    /**
     * 按主键查询.
     *
     * @param id 主键
     * @return provider（不存在时 empty）
     */
    Optional<LlmProvider> findProviderById(@Param("id") String id);

    /**
     * 按 name 查询（唯一索引）.
     *
     * @param name provider 名
     * @return provider
     */
    Optional<LlmProvider> findProviderByName(@Param("name") String name);

    /**
     * 查所有启用的 provider，按 priority 升序.
     *
     * @return provider 列表
     */
    List<LlmProvider> findEnabledProviders();

    /**
     * 查所有 provider（管理端列表用）.
     *
     * @return provider 列表
     */
    List<LlmProvider> findAllProviders();

    /**
     * 新增.
     *
     * @param provider provider 实体
     * @return 影响行数
     */
    int insertProvider(LlmProvider provider);

    /**
     * 按主键更新（api_key 单独走 updateApiKey，不在通用 update 里覆盖以免误清空）.
     *
     * @param provider provider 实体
     * @return 影响行数
     */
    int updateProvider(LlmProvider provider);

    /**
     * 单独更新 api_key 密文列.
     *
     * @param id           主键
     * @param ciphertext   新密文
     * @return 影响行数
     */
    int updateApiKey(@Param("id") String id, @Param("ciphertext") byte[] ciphertext);

    /**
     * 按主键删除.
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteProviderById(@Param("id") String id);
}