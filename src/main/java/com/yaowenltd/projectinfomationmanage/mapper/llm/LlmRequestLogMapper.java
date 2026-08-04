/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.mapper.llm;

import com.yaowenltd.projectinfomationmanage.model.dto.UsageAggregateDto;
import com.yaowenltd.projectinfomationmanage.model.entity.LlmRequestLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * LLM 调用记录 Mapper，对应表 {@code t_llm_request_log}.
 *
 * @since 2026-08-04
 */
@Mapper
public interface LlmRequestLogMapper {

    /**
     * 新增一条调用记录.
     *
     * @param log 记录实体
     * @return 影响行数
     */
    int insertRequestLog(LlmRequestLog log);

    /**
     * 用量聚合查询.
     *
     * @param userId    user_id 过滤（NULL 表示不过滤 —— 仅 ADMIN 允许）
     * @param provider  provider 过滤（NULL 表示不过滤）
     * @param model     model 过滤（NULL 表示不过滤）
     * @param startTime 时间区间起始（NULL 表示无下限）
     * @param endTime   时间区间结束（NULL 表示无上限）
     * @param groupBy   聚合维度：{@code user} / {@code provider} / {@code model} / {@code day}
     * @return 聚合结果列表
     */
    List<UsageAggregateDto> aggregateUsage(@Param("userId") String userId,
                                           @Param("provider") String provider,
                                           @Param("model") String model,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime,
                                           @Param("groupBy") String groupBy);
}