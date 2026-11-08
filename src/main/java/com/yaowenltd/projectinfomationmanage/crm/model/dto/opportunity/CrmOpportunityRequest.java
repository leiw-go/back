/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 商机入参（新建 / 更新）.
 *
 * @since 2026-11-08
 */
@Data
public class CrmOpportunityRequest {

    /**
     * 客户 ID
     */
    private String customerId;

    /**
     * 商机名称
     */
    private String name;

    /**
     * 阶段；为空时新建默认 INITIAL_CONTACT
     */
    private String stage;

    /**
     * 预计金额（原币）
     */
    private BigDecimal amount;

    /**
     * 原币币种；为空时默认 USD
     */
    private String currencyCode;

    /**
     * 赢率 0-100；为空时取阶段默认赢率
     */
    private Integer winRate;

    /**
     * 预计成交日
     */
    private LocalDate expectCloseDate;

    /**
     * 竞争对手
     */
    private String competitor;

    /**
     * 丢单原因；阶段转 LOST 时必填
     */
    private String lostReason;

    /**
     * 负责人 ID
     */
    private String ownerId;

    /**
     * 备注
     */
    private String remark;

}
