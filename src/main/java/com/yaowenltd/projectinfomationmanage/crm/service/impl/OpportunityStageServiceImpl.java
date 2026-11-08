/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.yaowenltd.projectinfomationmanage.crm.model.enums.OpportunityStage;
import com.yaowenltd.projectinfomationmanage.crm.service.OpportunityStageService;
import org.springframework.stereotype.Service;

/**
 * 商机阶段机实现.
 *
 * @since 2026-11-08
 */
@Service
public class OpportunityStageServiceImpl implements OpportunityStageService {


    /**
     * 判断两个商机阶段之间是否允许流转.
     */
    @Override
    public boolean canTransit(OpportunityStage from, OpportunityStage to) {
        throw new UnsupportedOperationException("TDD: 待实现");
    }

    /**
     * 校验商机阶段流转是否合法，非法时抛出业务异常.
     */
    @Override
    public void assertCanTransit(OpportunityStage from, OpportunityStage to, String lostReason) {
        throw new UnsupportedOperationException("TDD: 待实现");
    }
}
