/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.yaowenltd.projectinfomationmanage.crm.model.enums.OpportunityStage;
import com.yaowenltd.projectinfomationmanage.crm.service.OpportunityStageService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 商机阶段机实现.
 * <p>按需求文档 §3.3：INITIAL_CONTACT → QUALIFIED → QUOTED → NEGOTIATION → WON 为标准漏斗，
 * 逐级推进不可跳级；任意阶段都可以转 LOST，但必须填丢单原因；WON 与 LOST 为终态。
 * </p>
 *
 * @since 2026-11-08
 */
@Service
public class OpportunityStageServiceImpl implements OpportunityStageService {

    private static final Map<OpportunityStage, Set<OpportunityStage>> TRANSITIONS = buildTransitions();

    private static Map<OpportunityStage, Set<OpportunityStage>> buildTransitions() {
        Map<OpportunityStage, Set<OpportunityStage>> map = new EnumMap<>(OpportunityStage.class);
        map.put(OpportunityStage.INITIAL_CONTACT,
                EnumSet.of(OpportunityStage.QUALIFIED, OpportunityStage.LOST));
        map.put(OpportunityStage.QUALIFIED, EnumSet.of(OpportunityStage.QUOTED, OpportunityStage.LOST));
        map.put(OpportunityStage.QUOTED, EnumSet.of(OpportunityStage.NEGOTIATION, OpportunityStage.LOST));
        map.put(OpportunityStage.NEGOTIATION, EnumSet.of(OpportunityStage.WON, OpportunityStage.LOST));
        map.put(OpportunityStage.WON, Collections.emptySet());
        map.put(OpportunityStage.LOST, Collections.emptySet());
        return Collections.unmodifiableMap(map);
    }

    @Override
    public boolean canTransit(OpportunityStage from, OpportunityStage to) {
        if (from == null || to == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(from, Collections.emptySet()).contains(to);
    }

    @Override
    public void assertCanTransit(OpportunityStage from, OpportunityStage to, String lostReason) {
        if (!canTransit(from, to)) {
            throw new IllegalArgumentException(
                    "illegal opportunity stage transition: " + from + " -> " + to);
        }
        if (to == OpportunityStage.LOST && (lostReason == null || lostReason.trim().isEmpty())) {
            throw new IllegalArgumentException("lostReason is required when opportunity is lost");
        }
    }
}
