/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 报价单头实体，对应表 t_quotation.
 *
 * @since 2026-11-08
 */
@Data
public class CrmQuotationEntity {

    /**
     * 主键
     */
    private String id;

    /**
     * 报价单号
     */
    private String quotationNo;

    /**
     * 客户 → t_customer.id
     */
    private String customerId;

    /**
     * 来源询盘 → t_inquiry.id
     */
    private String inquiryId;

    /**
     * 关联商机 → t_opportunity.id
     */
    private String opportunityId;

    /**
     * 版本号
     */
    private Integer versionNo;

    /**
     * 币种（一单一币种）
     */
    private String currencyCode;

    /**
     * 贸易术语 EXW/FOB/CFR/CIF/DDP
     */
    private String incoterm;

    /**
     * 加价率 %
     */
    private BigDecimal markupRate;

    /**
     * 折扣率 %
     */
    private BigDecimal discountRate;

    /**
     * 报价总额（原币，服务端算）
     */
    private BigDecimal totalAmount;

    /**
     * 成本汇总（原币）
     */
    private BigDecimal totalCost;

    /**
     * 毛利额
     */
    private BigDecimal profitAmount;

    /**
     * 毛利率 %
     */
    private BigDecimal profitRate;

    /**
     * 本位币（USD）快照
     */
    private BigDecimal totalAmountBase;

    /**
     * 有效期至
     */
    private LocalDate validUntil;

    /**
     * 状态 DRAFT/SENT/CONFIRMED/REJECTED/EXPIRED
     */
    private String quotationStatus;

    /**
     * 拒绝原因
     */
    private String rejectReason;

    /**
     * 负责人 → t_user.id
     */
    private String ownerId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 逻辑删除：0 未删除；删除时写入毫秒时间戳
     */
    private Long deleted;

}
