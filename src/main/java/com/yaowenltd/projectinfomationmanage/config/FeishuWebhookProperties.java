/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 飞书机器人 webhook 配置.
 * <p>
 * 对应 {@code feishu.webhook.*} 配置块，绑定机器人接入所需的字段，
 * 挂接到 {@link RefreshScope} 后即可在 Nacos 上发布变更并通过
 * {@code /actuator/refresh} 触发实时刷新，无需重启应用.
 * </p>
 *
 * <p>
 * 使用示例（{@code application.yml}）:
 * <pre>
 * feishu:
 *   webhook:
 *     url: https://open.feishu.cn/open-apis/bot/v2/hook/xxxxxxxx
 *     enabled: true
 *     timeout-ms: 3000
 * </pre>
 * </p>
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "feishu.webhook")
public class FeishuWebhookProperties {

    /**
     * 飞书自定义机器人 webhook 地址.
     * 形如 {@code https://open.feishu.cn/open-apis/bot/v2/hook/<token>}.
     * 默认空字符串 —— 留空表示尚未配置，调用方应据此跳过推送.
     */
    private String url = "https://open.feishu.cn/open-apis/bot/v2/hook/a13b74c9-3f18-4612-a4e7-79da95858857";

    /**
     * 是否启用机器人推送.
     * 用于在测试环境关闭推送避免打扰真实群组；默认 false（安全默认）.
     */
    private boolean enabled = true;

    /**
     * webhook HTTP 调用的超时（毫秒）.
     * 默认 3 秒，飞书 webhook 同步响应通常很快.
     */
    private long timeoutMs = 3000L;

    /**
     * 飞书机器人加签密钥（"安全设置"中勾选"加签"时显示的密钥，形如 {@code sec-xxxx}）.
     * <p>
     * 留空表示机器人未开启加签，此时不会向请求体注入 {@code timestamp} / {@code sign} 字段.
     * 开启加签但未配置 secret 会导致飞书返回 {@code 190002 params error}，文案
     * 类似 {@code unknown content value}.
     * </p>
     */
    private String secret = "";

    /**
     * @return 飞书机器人 webhook 地址
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url 飞书机器人 webhook 地址
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return 是否启用机器人推送
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @param enabled 是否启用机器人推送
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return webhook 调用超时（毫秒）
     */
    public long getTimeoutMs() {
        return timeoutMs;
    }

    /**
     * @param timeoutMs webhook 调用超时（毫秒）
     */
    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    /**
     * @return 加签密钥，未开启加签时为空字符串
     */
    public String getSecret() {
        return secret;
    }

    /**
     * @param secret 加签密钥
     */
    public void setSecret(String secret) {
        this.secret = secret;
    }
}