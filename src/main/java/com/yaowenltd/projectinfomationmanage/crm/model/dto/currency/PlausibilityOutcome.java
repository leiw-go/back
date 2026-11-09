/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.currency;

import lombok.Data;
import java.math.BigDecimal;

/**
 * 合理性校验结论.
 * <p>
 * 对应汇率专项文档 §4.2：合理性校验**不依赖第二源**，
 * 是交叉比对之外的第二道闸门（用于抓"两个源一起错"）。
 *
 * @since 2026-11-08
 */
@Data
public class PlausibilityOutcome {

    /** 是否通过全部校验. */
    private boolean passed;

    /** 是否只是"环比突变"这类需要人工复核的情况（入库但标记 WARN）. */
    private boolean warn;

    /** 失败原因码；通过时为空. */
    private String reasonCode;

    /** 环比变动幅度（%）；未做环比校验时为空. */
    private BigDecimal dailyChange;

    /**
     * 构造一个通过的结论.
     *
     * @return 通过的结论
     */
    public static PlausibilityOutcome pass() {
        PlausibilityOutcome outcome = new PlausibilityOutcome();
        outcome.setPassed(true);
        return outcome;
    }

    /**
     * 构造一个不通过的结论.
     *
     * @param reasonCode 失败原因码
     * @return 不通过的结论
     */
    public static PlausibilityOutcome fail(String reasonCode) {
        PlausibilityOutcome outcome = new PlausibilityOutcome();
        outcome.setPassed(false);
        outcome.setReasonCode(reasonCode);
        return outcome;
    }

    /**
     * 构造一个"通过但需人工复核"的结论（入库并标记 WARN）.
     *
     * @param reasonCode   原因码
     * @param dailyChange  环比变动幅度（%）
     * @return 警告结论
     */
    public static PlausibilityOutcome warn(String reasonCode, BigDecimal dailyChange) {
        PlausibilityOutcome outcome = new PlausibilityOutcome();
        outcome.setPassed(true);
        outcome.setWarn(true);
        outcome.setReasonCode(reasonCode);
        outcome.setDailyChange(dailyChange);
        return outcome;
    }

}
