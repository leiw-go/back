/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.enums;

/**
 * 汇率交叉比对结果。<p>枚举值以稳定的英文码存储，可读文案走字典。
 *
 * @since 2026-11-08
 */
public enum CurrencyCheckResult {

    /**
     * MATCH 一致.
     */
    MATCH,

    /**
     * WARN 警告但入库.
     */
    WARN,

    /**
     * REJECT 拒绝入库.
     */
    REJECT,

    /**
     * SKIP 跳过比对.
     */
    SKIP,

    /**
     * SINGLE 单源未交叉验证.
     */
    SINGLE

}
