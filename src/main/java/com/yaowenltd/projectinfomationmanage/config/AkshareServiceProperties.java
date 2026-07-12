/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * {@code akshare.service.*} 配置块的绑定.
 * <p>
 * 挂接到 {@link RefreshScope} 上，以便在 Nacos 发布更新后的值，
 * 且 {@code spring.cloud.nacos.config.refresh-enabled} 流水线触发 {@code RefreshEvent}
 * 时销毁并重建本 Bean. 消费者持有旧实例的 CGLIB 代理，下一次字段访问即可得到新值，
 * 无需重启.
 * </p>
 *
 * <p>
 * <strong>关于 Spring Cloud Alibaba 的 diff 行为说明</strong>：
 * Nacos 的 {@code NacosContextRefresher} 只会转发那些实际发生<em>变更</em>的键
 * —— 新快照中存在而旧快照中不存在的键会被默默跳过. 因此，仅在 Nacos 上首次发布
 * 一个全新的配置块<em>不会</em>触发刷新. 本项目采用两层修复：
 * (a) 消费者也各自位于 {@code @RefreshScope} 之中，从而在刷新被触发后能拿到变更；
 * (b) 应用额外注册了一个回退监听器（参见 {@link DynamicRefreshFallback}），使得
 * {@code /actuator/refresh} 即便在变更集为空时也能完成重建.
 * </p>
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "akshare.service")
public class AkshareServiceProperties {

    /**
     * akshare FastAPI 桥接服务的基础地址. 默认回退到由
     * {@code docker-compose.akshare.yml} 映射的主机端口（18000）.
     */
    private String baseUrl = "http://localhost:18000";

    /**
     * 出站 HTTP 调用的连接 / 读取超时（毫秒）.
     * 默认 5 秒，对于 akshare 所对接的那些较慢的中文数据源而言较为保守.
     */
    private long timeoutMs = 5000L;

    /**
     * @return 下游调用所使用的基础地址
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * @param baseUrl 要使用的基础地址
     */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * @return 单次调用的超时（毫秒）
     */
    public long getTimeoutMs() {
        return timeoutMs;
    }

    /**
     * @param timeoutMs 单次调用的超时（毫秒）
     */
    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
