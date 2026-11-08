/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.yaowenltd.projectinfomationmanage.crm.model.enums.QuotationStatus;
import java.time.LocalDate;
import com.yaowenltd.projectinfomationmanage.crm.service.QuotationStatusService;
import org.springframework.stereotype.Service;

/**
 * 报价单状态机实现.
 *
 * @since 2026-11-08
 */
@Service
public class QuotationStatusServiceImpl implements QuotationStatusService {


    /**
     * 判断两个报价单状态之间是否允许流转.
     */
    @Override
    public boolean canTransit(QuotationStatus from, QuotationStatus to) {
        throw new UnsupportedOperationException("TDD: 待实现");
    }

    /**
     * 校验报价单状态流转是否合法，非法时抛出业务异常.
     */
    @Override
    public void assertCanTransit(QuotationStatus from, QuotationStatus to, String rejectReason) {
        throw new UnsupportedOperationException("TDD: 待实现");
    }

    /**
     * 判断报价单是否已过有效期.
     */
    @Override
    public boolean isExpired(QuotationStatus status, LocalDate validUntil, LocalDate today) {
        throw new UnsupportedOperationException("TDD: 待实现");
    }
}
