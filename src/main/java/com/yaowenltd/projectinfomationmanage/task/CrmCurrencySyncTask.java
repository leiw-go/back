/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.task;

import com.yaowenltd.projectinfomationmanage.crm.config.CrmCurrencyProperties;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencySyncStatusDto;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmCurrencyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * CRM 汇率同步定时任务.
 * <p>
 * 每天 07:00（{@code Asia/Shanghai}）跑一次：拉两个免费源 → 交叉比对 → 合理性校验 → 入库。
 * <p>
 * ⚠️ **时区**：容器默认 UTC，cron 必须显式指定 {@code zone = "Asia/Shanghai"}，
 * 否则会变成北京时间 15:00 才跑（与部署要求一致：容器 env 设置 {@code TZ=Asia/Shanghai}）。
 * <p>
 * 部署到多副本时需要加分布式锁，保证同一批数据不被并发 upsert。
 *
 * @since 2026-11-08
 */
@Component
@ConditionalOnProperty(prefix = "crm.currency.sync", name = "enabled", havingValue = "true",
        matchIfMissing = true)
public class CrmCurrencySyncTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrmCurrencySyncTask.class);

    private final CrmCurrencyService crmCurrencyService;
    private final CrmCurrencyProperties properties;

    /**
     * 使用指定依赖构造 CrmCurrencySyncTask.
     *
     * @param crmCurrencyService 汇率服务
     * @param properties         汇率配置
     */
    public CrmCurrencySyncTask(CrmCurrencyService crmCurrencyService, CrmCurrencyProperties properties) {
        this.crmCurrencyService = crmCurrencyService;
        this.properties = properties;
    }

    /**
     * 每日汇率同步.
     */
    @Scheduled(cron = "${crm.currency.sync.cron:0 0 7 * * ?}", zone = "Asia/Shanghai")
    public void syncDaily() {
        if (!properties.getSync().isEnabled()) {
            LOGGER.info("currency sync skipped: crm.currency.sync.enabled = false");
            return;
        }
        LOGGER.info("currency sync task started");
        try {
            int currencies = crmCurrencyService.syncCurrencyDictionary();
            LOGGER.info("currency dictionary synced: {} currencies", currencies);
            CrmCurrencySyncStatusDto status = crmCurrencyService.sync();
            LOGGER.info("currency sync finished: status={}, saved={}, rejected={}, skipped={}",
                    status == null ? "null" : status.getSyncStatus(),
                    status == null ? 0 : status.getSavedCount(),
                    status == null ? 0 : status.getRejectedCount(),
                    status == null ? 0 : status.getSkippedCount());
        } catch (Exception e) {
            LOGGER.error("currency sync task failed: {}", e.getMessage(), e);
        }
    }

}
