/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * CRM 报价单入参（新建 / 更新 / 基于询盘建单）.
 *
 * @since 2026-11-08
 */
@Data
public class CrmQuotationRequest {

    /**
     * 客户 ID；基于询盘建单时可不传，由询盘带出
     */
    private String customerId;

    /**
     * 来源询盘 ID
     */
    private String inquiryId;

    /**
     * 关联商机 ID
     */
    private String opportunityId;

    /**
     * 币种（一单一币种）；为空时默认 USD
     */
    private String currencyCode;

    /**
     * 贸易术语 EXW/FOB/CFR/CIF/DDP
     */
    private String incoterm;

    /**
     * 加价率 %（单头默认，明细可覆盖），0~1000
     */
    private BigDecimal markupRate;

    /**
     * 折扣率 %（单头默认，明细可覆盖），0~100
     */
    private BigDecimal discountRate;

    /**
     * 有效期至
     */
    private LocalDate validUntil;

    /**
     * 负责人 ID
     */
    private String ownerId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 报价明细
     */
    private List<CrmQuotationItemRequest> items = new ArrayList<>();

}
