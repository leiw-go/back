/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * CRM 询盘实体，对应表 t_inquiry.
 *
 * @since 2026-11-08
 */
@Data
public class CrmInquiryEntity {

    /**
     * 主键
     */
    private String id;

    /**
     * 客户 → t_customer.id
     */
    private String customerId;

    /**
     * 联系人 → t_customer_contact.id
     */
    private String contactId;

    /**
     * 询盘编号
     */
    private String inquiryNo;

    /**
     * 来源渠道
     */
    private String source;

    /**
     * 主题
     */
    private String subject;

    /**
     * 客户目标单价（仅参考，不参与定价）
     */
    private BigDecimal targetPrice;

    /**
     * 币种
     */
    private String currencyCode;

    /**
     * 数量
     */
    private BigDecimal quantity;

    /**
     * 单位
     */
    private String unit;

    /**
     * 期望交期
     */
    private LocalDate expectDeliveryDate;

    /**
     * 状态 NEW/PROCESSING/QUOTED/WON/LOST
     */
    private String inquiryStatus;

    /**
     * 原始需求内容（可外文）
     */
    private String content;

    /**
     * 负责人 → t_user.id
     */
    private String ownerId;

    /**
     * 首次回复时间（报价单创建时回填）
     */
    private LocalDateTime firstReplyAt;

    /**
     * 创建时间（用于"超时未报价"判定）
     */
    private LocalDateTime createTime;

    /**
     * 逻辑删除：0 未删除；删除时写入毫秒时间戳
     */
    private Long deleted;

}
