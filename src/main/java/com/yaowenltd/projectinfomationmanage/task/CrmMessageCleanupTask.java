/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.task;

import com.yaowenltd.projectinfomationmanage.crm.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * CRM 站内信清理定时任务.
 * <p>
 * 需求文档 §3.11：**已读**消息超过保留期（默认 90 天）后清理，**未读不清理**。
 * 每天凌晨跑一次即可，清理是逻辑删除（{@code t_message.deleted} 写入毫秒时间戳），
 * 不会物理删数据。
 * <p>
 * ⚠️ **时区**：cron 显式指定 {@code zone = "Asia/Shanghai"}，与容器 {@code TZ} 保持一致。
 *
 * @since 2026-11-08
 */
@Component
public class CrmMessageCleanupTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrmMessageCleanupTask.class);

    private final MessageService messageService;

    /** 已读消息保留天数. */
    private final int retentionDays;

    /**
     * 使用指定依赖构造 CrmMessageCleanupTask.
     *
     * @param messageService 站内信服务
     * @param retentionDays   已读消息保留天数
     */
    public CrmMessageCleanupTask(MessageService messageService,
                                 @Value("${crm.message.retention-days:90}") int retentionDays) {
        this.messageService = messageService;
        this.retentionDays = retentionDays;
    }

    /**
     * 清理超过保留期的已读消息.
     */
    @Scheduled(cron = "${crm.message.cleanup-cron:0 30 3 * * ?}", zone = "Asia/Shanghai")
    public void cleanup() {
        int cleaned = cleanupExpired();
        LOGGER.info("crm message cleanup finished: {} messages removed (retentionDays={})",
                cleaned, retentionDays);
    }

    /**
     * 执行清理（供 cron 与单元测试调用）.
     *
     * @return 清理掉的已读消息条数
     */
    public int cleanupExpired() {
        return messageService.cleanupReadMessages(retentionDays);
    }

    /**
     * 取已读消息保留天数.
     *
     * @return 保留天数
     */
    public int getRetentionDays() {
        return retentionDays;
    }

}
