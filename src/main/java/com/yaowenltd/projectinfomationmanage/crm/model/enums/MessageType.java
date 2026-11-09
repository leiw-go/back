/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.enums;

/**
 * 站内信消息类型（字典 {@code message_type}）.
 * <p>级别只影响站内呈现：{@code NORMAL} 进列表，{@code IMPORTANT} 置顶加粗，
 * {@code URGENT} 置顶加粗并弹窗。站内信**不做任何外推**。
 *
 * @since 2026-11-08
 */
public enum MessageType {

    /**
     * 报价被拒绝.
     */
    QUOTATION_REJECTED(MessageLevel.IMPORTANT),

    /**
     * 报价被确认.
     */
    QUOTATION_CONFIRMED(MessageLevel.IMPORTANT),

    /**
     * 报价即将过期.
     */
    QUOTATION_EXPIRING(MessageLevel.NORMAL),

    /**
     * 订单已确认.
     */
    ORDER_CONFIRMED(MessageLevel.IMPORTANT),

    /**
     * 订单已发货.
     */
    ORDER_SHIPPED(MessageLevel.NORMAL),

    /**
     * 应收逾期.
     */
    RECEIVABLE_OVERDUE(MessageLevel.URGENT),

    /**
     * 询盘超时未报价.
     */
    INQUIRY_TIMEOUT(MessageLevel.NORMAL),

    /**
     * 客户转交.
     */
    CUSTOMER_ASSIGNED(MessageLevel.NORMAL),

    /**
     * 商机阶段变更.
     */
    OPPORTUNITY_STAGE(MessageLevel.NORMAL),

    /**
     * 汇率同步失败.
     */
    RATE_SYNC_FAILED(MessageLevel.URGENT),

    /**
     * 汇率币种熔断.
     */
    RATE_CURRENCY_SUSPENDED(MessageLevel.IMPORTANT),

    /**
     * 人工消息.
     */
    MANUAL(MessageLevel.NORMAL);

    /** 该类型对应的默认级别. */
    private final MessageLevel defaultLevel;

    MessageType(MessageLevel defaultLevel) {
        this.defaultLevel = defaultLevel;
    }

    /**
     * 获取默认级别.
     *
     * @return 消息级别
     */
    public MessageLevel getDefaultLevel() {
        return defaultLevel;
    }

    /**
     * 按英文码解析消息类型，非法码返回 null（不抛异常，避免业务事件被类型问题打断）.
     *
     * @param code 消息类型英文码
     * @return 消息类型；未知返回 null
     */
    public static MessageType fromCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        for (MessageType type : values()) {
            if (type.name().equals(code.trim().toUpperCase())) {
                return type;
            }
        }
        return null;
    }

}
