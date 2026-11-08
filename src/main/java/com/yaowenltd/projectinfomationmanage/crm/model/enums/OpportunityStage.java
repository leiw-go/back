/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.enums;

/**
 * 商机阶段（销售漏斗）。<p>枚举值以稳定的英文码存储，可读文案走字典
 * （字典 {@code opportunity_stage} 与本枚举保持一致，代码侧以本枚举为准）。
 *
 * @since 2026-11-08
 */
public enum OpportunityStage {

    /**
     * INITIAL_CONTACT 初步接触.
     */
    INITIAL_CONTACT("初步接触", 10),

    /**
     * QUALIFIED 需求确认.
     */
    QUALIFIED("需求确认", 30),

    /**
     * QUOTED 已报价.
     */
    QUOTED("已报价", 50),

    /**
     * NEGOTIATION 商务谈判.
     */
    NEGOTIATION("商务谈判", 80),

    /**
     * WON 赢单.
     */
    WON("赢单", 100),

    /**
     * LOST 丢单.
     */
    LOST("丢单", 0);

    /** 阶段中文名，与字典 opportunity_stage 的 item_name 一致. */
    private final String displayName;

    /** 该阶段的默认赢率（%），新建商机未指定赢率时使用. */
    private final int defaultWinRate;

    OpportunityStage(String displayName, int defaultWinRate) {
        this.displayName = displayName;
        this.defaultWinRate = defaultWinRate;
    }

    /**
     * 获取阶段中文名.
     *
     * @return 中文名
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 获取该阶段的默认赢率.
     *
     * @return 赢率百分比（0-100）
     */
    public int getDefaultWinRate() {
        return defaultWinRate;
    }

    /**
     * 按英文码解析阶段，非法码直接抛业务异常.
     *
     * @param code 阶段英文码
     * @return 阶段枚举
     */
    public static OpportunityStage fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("opportunity stage must not be blank");
        }
        for (OpportunityStage stage : values()) {
            if (stage.name().equals(code.trim().toUpperCase())) {
                return stage;
            }
        }
        throw new IllegalArgumentException("unknown opportunity stage: " + code);
    }

}
