/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.enums.OpportunityStage;

/**
 * 商机阶段机.<p>按需求文档 §3.3：INITIAL_CONTACT → QUALIFIED → QUOTED → NEGOTIATION → WON，任意阶段可转 LOST。
 *
 * @since 2026-11-08
 */
public interface OpportunityStageService {

    /**
     * 判断两个商机阶段之间是否允许流转.
     *
     * @param from 当前阶段
     * @param to   目标阶段
     * @return 允许返回 true
     */
    boolean canTransit(OpportunityStage from, OpportunityStage to);

    /**
     * 校验商机阶段流转是否合法，非法时抛出业务异常.
     *
     * @param from        当前阶段
     * @param to          目标阶段
     * @param lostReason  丢单原因；目标阶段为 LOST 时必填
     */
    void assertCanTransit(OpportunityStage from, OpportunityStage to, String lostReason);
}
