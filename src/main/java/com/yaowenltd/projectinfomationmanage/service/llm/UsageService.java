/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.llm;

import com.yaowenltd.projectinfomationmanage.mapper.llm.LlmRequestLogMapper;
import com.yaowenltd.projectinfomationmanage.model.dto.UsageAggregateDto;
import com.yaowenltd.projectinfomationmanage.model.dto.UsageQueryRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 用量聚合查询服务.
 *
 * @since 2026-08-04
 */
@Service
public class UsageService {

    private final LlmRequestLogMapper mapper;

    public UsageService(LlmRequestLogMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 聚合查询 —— 普通用户被强制覆盖为查询自己的用量.
     *
     * @param request      查询参数
     * @param currentUser  当前调用方（普通用户强制覆盖 userId；ADMIN 可指定任意 userId）
     * @param isAdmin      当前调用方是否是 ADMIN 角色
     * @return 聚合结果列表
     */
    public List<UsageAggregateDto> aggregate(UsageQueryRequest request,
                                             String currentUser,
                                             boolean isAdmin) {
        String effectiveUserId;
        if (isAdmin) {
            effectiveUserId = request.getUserId();
        } else {
            // 非 ADMIN 强制只看自己
            effectiveUserId = currentUser;
        }

        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        if (request.getStartDate() != null) {
            startTime = request.getStartDate().atStartOfDay();
        }
        if (request.getEndDate() != null) {
            endTime = request.getEndDate().atTime(23, 59, 59);
        }

        return mapper.aggregateUsage(
                effectiveUserId,
                request.getProvider(),
                request.getModel(),
                startTime,
                endTime,
                request.getGroupBy());
    }
}