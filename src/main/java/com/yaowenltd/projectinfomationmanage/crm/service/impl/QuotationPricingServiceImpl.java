/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing.QuotationPricingRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing.QuotationPricingResult;
import com.yaowenltd.projectinfomationmanage.crm.service.QuotationPricingService;
import org.springframework.stereotype.Service;

/**
 * 报价自动算价引擎实现.
 *
 * @since 2026-11-08
 */
@Service
public class QuotationPricingServiceImpl implements QuotationPricingService {


    /**
     * 按成本价与加价率计算报价明细及单头汇总.
     */
    @Override
    public QuotationPricingResult calculate(QuotationPricingRequest request) {
        throw new UnsupportedOperationException("TDD: 待实现");
    }
}
