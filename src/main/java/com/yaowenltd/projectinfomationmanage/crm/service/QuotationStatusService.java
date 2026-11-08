/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.enums.QuotationStatus;
import java.time.LocalDate;

/**
 * 报价单状态机.<p>按需求文档 §3.5：DRAFT → SENT → CONFIRMED / REJECTED，只有 DRAFT 可改。
 *
 * @since 2026-11-08
 */
public interface QuotationStatusService {

    /**
     * 判断两个报价单状态之间是否允许流转.
     *
     * @param from 当前状态
     * @param to   目标状态
     * @return 允许返回 true
     */
    boolean canTransit(QuotationStatus from, QuotationStatus to);

    /**
     * 校验报价单状态流转是否合法，非法时抛出业务异常.
     *
     * @param from         当前状态
     * @param to           目标状态
     * @param rejectReason 拒绝原因；目标状态为 REJECTED 时必填
     */
    void assertCanTransit(QuotationStatus from, QuotationStatus to, String rejectReason);

    /**
     * 判断报价单是否已过有效期.
     *
     * @param status     当前状态
     * @param validUntil 有效期截止日；为空表示长期有效
     * @param today      当前日期
     * @return 已过期返回 true
     */
    boolean isExpired(QuotationStatus status, LocalDate validUntil, LocalDate today);
}
