/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.enums;

/**
 * 商机阶段（销售漏斗）。<p>枚举值以稳定的英文码存储，可读文案走字典。
 *
 * @since 2026-11-08
 */
public enum OpportunityStage {

    /**
     * INITIAL_CONTACT 初步接触.
     */
    INITIAL_CONTACT,

    /**
     * QUALIFIED 需求确认.
     */
    QUALIFIED,

    /**
     * QUOTED 已报价.
     */
    QUOTED,

    /**
     * NEGOTIATION 商务谈判.
     */
    NEGOTIATION,

    /**
     * WON 赢单.
     */
    WON,

    /**
     * LOST 丢单.
     */
    LOST

}
