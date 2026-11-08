/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing.QuotationPricingRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing.QuotationPricingResult;

/**
 * 报价自动算价引擎.<p>按需求文档 §3.5.1：行单价 = 成本单价 × (1 + 加价率) × (1 - 折扣率)，金额统一舍入到 2 位。
 *
 * @since 2026-11-08
 */
public interface QuotationPricingService {

    /**
     * 按成本价与加价率计算报价明细及单头汇总.
     *
     * @param request 算价请求
     * @return 算价结果
     */
    QuotationPricingResult calculate(QuotationPricingRequest request);
}
