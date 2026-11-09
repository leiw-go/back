/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.task;

import com.yaowenltd.projectinfomationmanage.crm.common.MessageNotifier;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmInquiryMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmInquiryEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.MessageBizType;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.MessageType;
import com.yaowenltd.projectinfomationmanage.crm.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CRM 询盘超时未报价扫描定时任务.
 * <p>
 * 需求文档 §3.11：询盘超时未报价发 {@code INQUIRY_TIMEOUT}（NORMAL）站内信给询盘负责人。
 * <p>
 * **幂等**：同一询盘只发一次 —— 判据是"该询盘是否已发过 INQUIRY_TIMEOUT 系统消息"；
 * 已经报过价（名下有报价单）或已赢单 / 丢单的询盘不再提醒。
 * <p>
 * ⚠️ **时区**：cron 显式指定 {@code zone = "Asia/Shanghai"}。
 *
 * @since 2026-11-08
 */
@Component
public class CrmInquiryTimeoutTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrmInquiryTimeoutTask.class);

    private final CrmInquiryMapper inquiryMapper;
    private final MessageService messageService;
    private final MessageNotifier messageNotifier;

    /** 超时天数（超过该天数仍未报价就提醒）. */
    private final int timeoutDays;

    /**
     * 使用指定依赖构造 CrmInquiryTimeoutTask.
     *
     * @param inquiryMapper  询盘 Mapper
     * @param messageService 站内信服务
     * @param messageNotifier 站内信通知器
     * @param timeoutDays    超时天数
     */
    public CrmInquiryTimeoutTask(CrmInquiryMapper inquiryMapper,
                                 MessageService messageService,
                                 MessageNotifier messageNotifier,
                                 @Value("${crm.inquiry.timeout-days:3}") int timeoutDays) {
        this.inquiryMapper = inquiryMapper;
        this.messageService = messageService;
        this.messageNotifier = messageNotifier;
        this.timeoutDays = timeoutDays;
    }

    /**
     * 扫描超时未报价的询盘.
     */
    @Scheduled(cron = "${crm.inquiry.timeout-cron:0 0 9 * * ?}", zone = "Asia/Shanghai")
    public void scan() {
        int notified = scanTimeout(LocalDateTime.now());
        LOGGER.info("crm inquiry timeout scan finished: {} notifications sent", notified);
    }

    /**
     * 执行超时扫描（供 cron 与单元测试调用）.
     *
     * @param now 判定基准时刻
     * @return 本次发出的通知条数
     */
    public int scanTimeout(LocalDateTime now) {
        List<CrmInquiryEntity> pendingList = inquiryMapper.selectPendingQuoteList(now.minusDays(timeoutDays));
        if (pendingList == null || pendingList.isEmpty()) {
            return 0;
        }
        int notified = 0;
        for (CrmInquiryEntity inquiry : pendingList) {
            if (messageService.existsSystemMessage(MessageBizType.INQUIRY.name(),
                    inquiry.getId(), MessageType.INQUIRY_TIMEOUT.name())) {
                continue;
            }
            if (notifyOwner(inquiry)) {
                notified++;
            }
        }
        return notified;
    }

    /**
     * 给询盘负责人发超时提醒.
     *
     * @param inquiry 询盘
     * @return 是否成功发出
     */
    private boolean notifyOwner(CrmInquiryEntity inquiry) {
        String receiverId = inquiry.getOwnerId();
        if (receiverId == null || receiverId.trim().isEmpty()) {
            LOGGER.warn("询盘超时通知跳过：负责人为空, inquiryId={}", inquiry.getId());
            return false;
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("days", String.valueOf(timeoutDays));
        messageNotifier.notifyOwner(MessageType.INQUIRY_TIMEOUT, MessageBizType.INQUIRY.name(),
                inquiry.getId(), inquiry.getInquiryNo(), receiverId, placeholders);
        return true;
    }

    /**
     * 取超时天数.
     *
     * @return 超时天数
     */
    public int getTimeoutDays() {
        return timeoutDays;
    }

}
