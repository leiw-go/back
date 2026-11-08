/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation;

import lombok.Data;

/**
 * CRM 报价单拒绝请求.
 *
 * @since 2026-11-08
 */
@Data
public class CrmQuotationRejectRequest {

    /**
     * 拒绝原因（必填）
     */
    private String rejectReason;

}
