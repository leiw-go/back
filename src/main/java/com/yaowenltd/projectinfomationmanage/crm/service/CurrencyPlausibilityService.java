/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CurrencyQuote;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.PlausibilityOutcome;
import java.math.BigDecimal;

/**
 * 汇率合理性校验服务.
 * <p>
 * 对应汇率专项文档 §4.2：值域、正数、日期不超前、日期不太旧、环比突变。
 *
 * @since 2026-11-08
 */
public interface CurrencyPlausibilityService {

    /**
     * 对单条汇率做合理性校验.
     *
     * @param quote        待校验的汇率
     * @param previousRate 库中该币种上一条有效汇率；没有则传 null（跳过环比校验）
     * @return 校验结论
     */
    PlausibilityOutcome check(CurrencyQuote quote, BigDecimal previousRate);

}
