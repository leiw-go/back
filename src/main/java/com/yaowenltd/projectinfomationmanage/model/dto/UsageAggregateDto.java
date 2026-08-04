/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.model.dto;

import lombok.Data;

/**
 * 用量聚合查询响应.
 *
 * @since 2026-08-04
 */
@Data
public class UsageAggregateDto {

    /** 维度：user_id / provider / model 等 */
    private String dimension;

    /** 维度值 */
    private String dimensionValue;

    /** 调用总次数 */
    private long totalCalls;

    /** prompt token 总量 */
    private long totalPromptTokens;

    /** completion token 总量 */
    private long totalCompletionTokens;

    /** token 总和 */
    private long totalTokens;

    /** 失败调用数（status >= 400） */
    private long failedCalls;
}