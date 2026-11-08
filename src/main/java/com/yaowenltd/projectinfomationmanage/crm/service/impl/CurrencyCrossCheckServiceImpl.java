/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrossCheckRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrossCheckOutcome;
import com.yaowenltd.projectinfomationmanage.crm.service.CurrencyCrossCheckService;
import org.springframework.stereotype.Service;

/**
 * 两源汇率交叉比对与熔断判定实现.
 *
 * @since 2026-11-08
 */
@Service
public class CurrencyCrossCheckServiceImpl implements CurrencyCrossCheckService {


    /**
     * 对两源汇率做交叉比对.
     */
    @Override
    public CrossCheckOutcome check(CrossCheckRequest request) {
        throw new UnsupportedOperationException("TDD: 待实现");
    }

    /**
     * 判断某币种是否应被熔断.
     */
    @Override
    public boolean shouldSuspend(int consecutiveBreachDays, int suspectDays) {
        throw new UnsupportedOperationException("TDD: 待实现");
    }

    /**
     * 判断某币种是否应解除熔断.
     */
    @Override
    public boolean shouldRecover(int consecutiveOkDays, int recoverDays) {
        throw new UnsupportedOperationException("TDD: 待实现");
    }
}
