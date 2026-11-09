/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.common;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.message.MessageDraft;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.MessageType;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 站内信文案工厂.
 * <p>
 * 按需求文档 §3.11：文案**一律中文**，由代码常量类按 {@code message_type} 分支生成，
 * 支持 {@code {占位符}} 替换；金额用原币 + 币种。
 * 站内信**不做任何外推**（不发邮件、不推飞书）。
 *
 * @since 2026-11-08
 */
public final class MessageTextFactory {

    /** 各类消息的标题模板. */
    private static final Map<MessageType, String> TITLES = new LinkedHashMap<>();

    /** 各类消息的正文模板. */
    private static final Map<MessageType, String> CONTENTS = new LinkedHashMap<>();

    static {
        TITLES.put(MessageType.QUOTATION_REJECTED, "报价被拒绝：{bizTitle}");
        CONTENTS.put(MessageType.QUOTATION_REJECTED,
                "客户拒绝了报价单 {bizTitle}，原因：{reason}。请及时跟进。");

        TITLES.put(MessageType.QUOTATION_CONFIRMED, "报价已确认：{bizTitle}");
        CONTENTS.put(MessageType.QUOTATION_CONFIRMED,
                "客户已确认报价单 {bizTitle}，金额 {amount}。可以安排生产或发货。");

        TITLES.put(MessageType.QUOTATION_EXPIRING, "报价即将过期：{bizTitle}");
        CONTENTS.put(MessageType.QUOTATION_EXPIRING,
                "报价单 {bizTitle} 将在 {validUntil} 到期，请及时跟进客户。");

        TITLES.put(MessageType.ORDER_CONFIRMED, "订单已确认：{bizTitle}");
        CONTENTS.put(MessageType.ORDER_CONFIRMED,
                "订单 {bizTitle} 已确认，金额 {amount}。请关注定金与尾款的收款节点。");

        TITLES.put(MessageType.ORDER_SHIPPED, "订单已发货：{bizTitle}");
        CONTENTS.put(MessageType.ORDER_SHIPPED,
                "订单 {bizTitle} 已发货，运单号 {shippingNo}，预计到港 {expectArriveDate}。");

        TITLES.put(MessageType.RECEIVABLE_OVERDUE, "应收逾期：{bizTitle}");
        CONTENTS.put(MessageType.RECEIVABLE_OVERDUE,
                "订单 {bizTitle} 的应收已逾期 {overdueDays} 天，未收金额 {amount}，请尽快催款。");

        TITLES.put(MessageType.INQUIRY_TIMEOUT, "询盘超时未报价：{bizTitle}");
        CONTENTS.put(MessageType.INQUIRY_TIMEOUT,
                "询盘 {bizTitle} 已超过 {days} 天仍未报价，请尽快处理。");

        TITLES.put(MessageType.CUSTOMER_ASSIGNED, "客户已转交给你：{bizTitle}");
        CONTENTS.put(MessageType.CUSTOMER_ASSIGNED,
                "客户 {bizTitle} 已转交给你，请及时跟进。");

        TITLES.put(MessageType.OPPORTUNITY_STAGE, "商机阶段变更：{bizTitle}");
        CONTENTS.put(MessageType.OPPORTUNITY_STAGE,
                "商机 {bizTitle} 的阶段变更为 {stage}。");

        TITLES.put(MessageType.RATE_SYNC_FAILED, "汇率同步失败");
        CONTENTS.put(MessageType.RATE_SYNC_FAILED,
                "汇率同步失败：{reason}。业务读写不受影响，请运维关注。");

        TITLES.put(MessageType.RATE_CURRENCY_SUSPENDED, "汇率币种已熔断：{bizTitle}");
        CONTENTS.put(MessageType.RATE_CURRENCY_SUSPENDED,
                "币种 {currencyCode} 连续 {suspectDays} 天交叉比对超标，已熔断暂停入库。");

        TITLES.put(MessageType.MANUAL, "{bizTitle}");
        CONTENTS.put(MessageType.MANUAL, "{content}");
    }

    private MessageTextFactory() {
    }

    /**
     * 组装一条待发送的站内信草稿.
     * <p>
     * 标题 / 正文按 {@code message_type} 渲染，业务主键与业务标题一并写入草稿，
     * 便于前端从列表直接跳转. {@code bizTitle} 会自动作为 {@code {bizTitle}} 占位符参与渲染
     * （调用方已显式传入同名占位符时以调用方为准）。
     * </p>
     *
     * @param type         消息类型
     * @param bizType      关联业务类型
     * @param bizId        关联业务主键
     * @param bizTitle     关联业务标题
     * @param placeholders 占位符键值对
     * @return 消息草稿
     */
    public static MessageDraft draft(MessageType type, String bizType, String bizId, String bizTitle,
                                     Map<String, String> placeholders) {
        Map<String, String> merged = new HashMap<>();
        if (placeholders != null) {
            merged.putAll(placeholders);
        }
        if (bizTitle != null) {
            merged.putIfAbsent("bizTitle", bizTitle);
        }
        MessageDraft draft = new MessageDraft();
        draft.setMessageType(type.name());
        draft.setLevel(type.getDefaultLevel().name());
        draft.setTitle(title(type, merged));
        draft.setContent(content(type, merged));
        draft.setBizType(bizType);
        draft.setBizId(bizId);
        draft.setBizTitle(bizTitle);
        return draft;
    }

    /**
     * 生成某类消息的标题.
     *
     * @param type      消息类型
     * @param placeholders 占位符键值对
     * @return 标题；类型未知时返回空串
     */
    public static String title(MessageType type, Map<String, String> placeholders) {
        String template = TITLES.get(type);
        return template == null ? "" : render(template, placeholders);
    }

    /**
     * 生成某类消息的正文.
     *
     * @param type      消息类型
     * @param placeholders 占位符键值对
     * @return 正文；类型未知时返回空串
     */
    public static String content(MessageType type, Map<String, String> placeholders) {
        String template = CONTENTS.get(type);
        return template == null ? "" : render(template, placeholders);
    }

    /**
     * 把金额渲染成"原币 + 币种"的形式.
     *
     * @param amount   金额
     * @param currency 币种
     * @return 渲染后的金额文案
     */
    public static String money(BigDecimal amount, String currency) {
        if (amount == null) {
            return "0.00";
        }
        return amount.toPlainString() + " " + (currency == null ? "" : currency);
    }

    /**
     * 替换模板里的 {占位符}.
     *
     * @param template     模板
     * @param placeholders 占位符键值对
     * @return 渲染结果
     */
    private static String render(String template, Map<String, String> placeholders) {
        if (placeholders == null || placeholders.isEmpty()) {
            return template;
        }
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}",
                    entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

}
