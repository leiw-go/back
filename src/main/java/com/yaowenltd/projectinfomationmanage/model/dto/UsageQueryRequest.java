/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.model.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 用量聚合查询请求参数（管理端 {@code GET /design/api/llm/usage}）.
 *
 * @since 2026-08-04
 */
@Data
public class UsageQueryRequest {

    /** 调用方 user_id（ADMIN 可指定，普通用户被强制覆盖为当前用户） */
    private String userId;

    /** 上游 provider 名过滤 */
    private String provider;

    /** model 名过滤 */
    private String model;

    /** 时间区间起始（含），可空 */
    private LocalDate startDate;

    /** 时间区间结束（含），可空 */
    private LocalDate endDate;

    /** 聚合维度：{@code user} / {@code provider} / {@code model} / {@code day}，默认 {@code user} */
    private String groupBy = "user";
}