/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * CRM 商机实体，对应表 t_opportunity.
 *
 * @since 2026-11-08
 */
@Data
public class CrmOpportunityEntity {

    /**
     * 主键
     */
    private String id;

    /**
     * 所属客户 → t_customer.id
     */
    private String customerId;

    /**
     * 商机名称
     */
    private String name;

    /**
     * 阶段，字典 opportunity_stage
     */
    private String stage;

    /**
     * 预计金额（原币）
     */
    private BigDecimal amount;

    /**
     * 原币币种
     */
    private String currencyCode;

    /**
     * 本位币（USD）快照
     */
    private BigDecimal amountBase;

    /**
     * 赢率 0-100
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
     * 丢单原因
     */
    private String lostReason;

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
