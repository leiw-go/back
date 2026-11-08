/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.yaowenltd.projectinfomationmanage.crm.model.enums.QuotationStatus;
import com.yaowenltd.projectinfomationmanage.crm.service.QuotationStatusService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * 报价单状态机实现.
 * <p>按需求文档 §3.5：DRAFT → SENT → CONFIRMED / REJECTED；只有 DRAFT 可改；
 * 超过有效期的 DRAFT / SENT 视为 EXPIRED。
 * </p>
 *
 * @since 2026-11-08
 */
@Service
public class QuotationStatusServiceImpl implements QuotationStatusService {

    private static final Map<QuotationStatus, Set<QuotationStatus>> TRANSITIONS = buildTransitions();

    private static Map<QuotationStatus, Set<QuotationStatus>> buildTransitions() {
        Map<QuotationStatus, Set<QuotationStatus>> map = new EnumMap<>(QuotationStatus.class);
        map.put(QuotationStatus.DRAFT, EnumSet.of(QuotationStatus.SENT));
        map.put(QuotationStatus.SENT, EnumSet.of(QuotationStatus.CONFIRMED, QuotationStatus.REJECTED));
        map.put(QuotationStatus.CONFIRMED, Collections.emptySet());
        map.put(QuotationStatus.REJECTED, Collections.emptySet());
        map.put(QuotationStatus.EXPIRED, Collections.emptySet());
        return Collections.unmodifiableMap(map);
    }

    @Override
    public boolean canTransit(QuotationStatus from, QuotationStatus to) {
        if (from == null || to == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(from, Collections.emptySet()).contains(to);
    }

    @Override
    public void assertCanTransit(QuotationStatus from, QuotationStatus to, String rejectReason) {
        if (!canTransit(from, to)) {
            throw new IllegalArgumentException(
                    "illegal quotation status transition: " + from + " -> " + to);
        }
        if (to == QuotationStatus.REJECTED && (rejectReason == null || rejectReason.trim().isEmpty())) {
            throw new IllegalArgumentException("rejectReason is required when quotation is rejected");
        }
    }

    @Override
    public boolean isExpired(QuotationStatus status, LocalDate validUntil, LocalDate today) {
        if (status == null || validUntil == null || today == null) {
            return false;
        }
        if (status != QuotationStatus.DRAFT && status != QuotationStatus.SENT) {
            return false;
        }
        return today.isAfter(validUntil);
    }
}
