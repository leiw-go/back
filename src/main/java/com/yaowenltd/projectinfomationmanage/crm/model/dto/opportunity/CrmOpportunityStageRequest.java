/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity;

import lombok.Data;

/**
 * CRM 商机阶段推进请求.
 *
 * @since 2026-11-08
 */
@Data
public class CrmOpportunityStageRequest {

    /**
     * 目标阶段
     */
    private String stage;

    /**
     * 丢单原因；目标阶段为 LOST 时必填
     */
    private String lostReason;

}
