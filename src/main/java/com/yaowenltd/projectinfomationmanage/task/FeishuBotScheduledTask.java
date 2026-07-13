/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.task;

import com.yaowenltd.projectinfomationmanage.service.FeishuBotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 飞书机器人定时推送任务.
 * <p>
 * 通过 Spring 的 {@link Scheduled} 调度器，每两分钟向飞书机器人推送一条
 * "hello world" 测试消息，用于验证 webhook 接入是否正常.
 * </p>
 *
 * <p>
 * <strong>关闭方式</strong>：将 {@code feishu.webhook.enabled} 设为 false，
 * 或通过 {@code /actuator/refresh} 重新发布 Nacos 配置后即可实时生效，
 * 无需重启应用.
 * </p>
 */
@Component
public class FeishuBotScheduledTask {

    private static final Logger LOG = LoggerFactory.getLogger(FeishuBotScheduledTask.class);

    /**
     * 推送的测试文本 —— 这里固定为 hello world，后续接入业务告警时
     * 由调用方传入动态内容即可.
     */
    private static final String TEST_MESSAGE = "hello world";

    private final FeishuBotService feishuBotService;

    /**
     * 构造 FeishuBotScheduledTask.
     *
     * @param feishuBotService 飞书机器人推送服务
     */
    public FeishuBotScheduledTask(FeishuBotService feishuBotService) {
        this.feishuBotService = feishuBotService;
    }

    /**
     * 每两分钟执行一次的 hello world 推送.
     * <p>
     * cron 表达式 {@code 0 0/2 * * * ?} 表示从每小时的 0 分开始，每 2 分钟
     * 触发一次（00、02、04 ... 58）.
     * </p>
     */
//    @Scheduled(cron = "0 0/1 * * * ?")
    public void pushHelloWorld() {
        LOG.info("执行飞书机器人定时推送任务");
        boolean ok = feishuBotService.sendText(TEST_MESSAGE);
        if (!ok) {
            // 推送失败已在 Service 内部记录，这里仅做日志标记，便于排查周期是否正常运转
            LOG.info("本轮飞书机器人推送未成功（可能未启用或 URL 未配置）");
        }
    }
}