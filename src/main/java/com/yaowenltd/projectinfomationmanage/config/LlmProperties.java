/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LLM 中转（OpenAI 兼容）相关配置.
 * <p>
 * 对应 {@code llm.*} 配置块。provider 元数据 + 上游 api_key 走数据库
 * （见 {@code V2__add_llm_relay.sql}），本类只承载非敏感默认值：上游超时、AES-GCM
 * 加密密钥、trace 导出策略。
 * </p>
 *
 * <p>
 * 使用示例（{@code application.yml}）:
 * <pre>
 * llm:
 *   upstream:
 *     timeout-ms: 60000
 *   encrypt:
 *     key: base64编码的32字节密钥
 *   trace:
 *     exporter: none        # logging | none
 * </pre>
 * </p>
 *
 * @since 2026-08-04
 */
@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    private final Upstream upstream = new Upstream();

    private final Encrypt encrypt = new Encrypt();

    private final Trace trace = new Trace();

    /**
     * @return 上游调用相关配置
     */
    public Upstream getUpstream() {
        return upstream;
    }

    /**
     * @return 加密相关配置
     */
    public Encrypt getEncrypt() {
        return encrypt;
    }

    /**
     * @return trace 相关配置
     */
    public Trace getTrace() {
        return trace;
    }

    /**
     * 上游 HTTP 调用相关配置.
     *
     * @since 2026-08-04
     */
    public static class Upstream {

        /**
         * 非流式调用上游 HTTP 超时（毫秒）；流式调用 read timeout 固定为 0.
         */
        private long timeoutMs = 60000L;

        /**
         * @return 非流式上游超时（毫秒）
         */
        public long getTimeoutMs() {
            return timeoutMs;
        }

        /**
         * @param timeoutMs 非流式上游超时（毫秒）
         */
        public void setTimeoutMs(long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }

    /**
     * 加密相关配置.
     *
     * @since 2026-08-04
     */
    public static class Encrypt {

        /**
         * AES-GCM 加密密钥，base64 编码的 32 字节串.
         * 必须从环境变量 {@code LLM_ENCRYPT_KEY} 注入，禁止入库.
         */
        private String key = "";

        /**
         * @return base64 编码的 AES-GCM 密钥
         */
        public String getKey() {
            return key;
        }

        /**
         * @param key base64 编码的 AES-GCM 密钥
         */
        public void setKey(String key) {
            this.key = key;
        }
    }

    /**
     * trace 导出相关配置.
     *
     * @since 2026-08-04
     */
    public static class Trace {

        /**
         * span 导出方式：{@code logging} 输出到 stdout，{@code none} 不导出（in-memory）.
         */
        private String exporter = "none";

        /**
         * @return span 导出方式
         */
        public String getExporter() {
            return exporter;
        }

        /**
         * @param exporter span 导出方式
         */
        public void setExporter(String exporter) {
            this.exporter = exporter;
        }
    }
}