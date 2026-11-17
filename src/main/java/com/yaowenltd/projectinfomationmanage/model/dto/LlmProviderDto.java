/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.model.dto;

import lombok.Data;

/**
 * 管理端 provider CRUD 接口的入参 / 出参.
 *
 * @since 2026-08-04
 */
@Data
public class LlmProviderDto {

    /** 主键 UUID（更新时必填，创建时不填由服务端生成） */
    private String id;

    /** provider 唯一名 */
    private String name;

    /** 上游 baseUrl */
    private String baseUrl;

    /** 明文 API key —— 仅在创建 / 更新时接收；响应里不返回 */
    private String apiKey;

    /** 1: 启用, 0: 停用 */
    private Integer enabled;

    /** 路由优先级 */
    private Integer priority;

    /** 单 provider 超时覆盖（毫秒），NULL 表示走全局 */
    private Integer timeoutMsOverride;
}