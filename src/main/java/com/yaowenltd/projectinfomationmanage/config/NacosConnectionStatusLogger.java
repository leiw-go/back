/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.config;

import com.alibaba.cloud.nacos.NacosConfigProperties;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Nacos 连接状态一次性提示器.
 * <p>
 * Spring Cloud Alibaba 默认在 {@code spring.cloud.nacos.config.fail-fast=false} 时
 * 允许 Nacos 暂不可达、随后再连上. 但客户端内部的
 * {@code RpcClient} / {@code GrpcClient} / {@code ClientWorker} 会持续以
 * {@code ERROR} 级别刷"连接失败"日志——既扰乱运维视线,又和
 * "应用已正常启动"这一事实相矛盾.
 * </p>
 * <p>
 * 本组件在 {@link ApplicationReadyEvent ApplicationReadyEvent} 触发后,
 * 异步做一次轻量 TCP 握手（连接配置的 Nacos HTTP 端口）,并以
 * {@code INFO} / {@code WARN} 各打印一行作为启动期唯一的提示;
 * 配合 {@code logback-spring.xml} 把 Nacos 内部包降到 {@code WARN} 后,
 * 实际运行期就不会再看到循环报错.
 * </p>
 *
 * <p>
 * 仅在 Nacos 配置中心启用时加载（test profile 下
 * {@code spring.cloud.nacos.config.enabled=false} 不会装配）;
 * 探测时使用普通 {@link Socket} 握手,完全独立于 Nacos 客户端,
 * 即便客户端本身在循环重试也不会触发额外的客户端日志.
 * </p>
 *
 * @since 2026-07-28
 */
@Component
@ConditionalOnProperty(name = "spring.cloud.nacos.config.enabled", havingValue = "true", matchIfMissing = true)
public class NacosConnectionStatusLogger implements ApplicationListener<ApplicationReadyEvent> {

    /**
     * 启动后等待 Nacos 客户端做首次连接的时间,再进行可达性探测.
     */
    static final long PROBE_DELAY_SECONDS = 3L;

    /**
     * 单次 TCP 握手超时时间,单位毫秒.
     */
    static final int PROBE_TIMEOUT_MS = 2000;

    private static final Logger LOGGER = LoggerFactory.getLogger(NacosConnectionStatusLogger.class);

    private final NacosConfigProperties configProperties;
    private final NacosDiscoveryProperties discoveryProperties;
    private final ScheduledExecutorService scheduler;

    /**
     * 注入 Nacos 配置与发现客户端属性.
     * <p>
     * 使用 {@link ObjectProvider} 兼容 Nacos 关闭时属性 Bean 缺席的情况
     * （此时本类已被 {@code @ConditionalOnProperty} 过滤,不会真正运行）.
     * </p>
     * <p>
     * 本类有两个构造器(本构造器 + 下面的 3 参测试构造器),
     * Spring 4.3+ 在多构造器场景下不会自动选择,必须显式
     * {@link Autowired @Autowired} 指明用于自动装配的入口.
     * </p>
     *
     * @param configProperties    Nacos 配置中心连接属性
     * @param discoveryProperties Nacos 服务发现连接属性
     */
    @Autowired
    public NacosConnectionStatusLogger(ObjectProvider<NacosConfigProperties> configProperties,
            ObjectProvider<NacosDiscoveryProperties> discoveryProperties) {
        this(configProperties, discoveryProperties, Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "nacos-startup-probe");
            t.setDaemon(true);
            return t;
        }));
    }

    /**
     * 测试用构造器:允许替换调度器便于同步执行.
     *
     * @param configProperties    Nacos 配置中心连接属性
     * @param discoveryProperties Nacos 服务发现连接属性
     * @param scheduler           探测调度器
     */
    NacosConnectionStatusLogger(ObjectProvider<NacosConfigProperties> configProperties,
            ObjectProvider<NacosDiscoveryProperties> discoveryProperties,
            ScheduledExecutorService scheduler) {
        this.configProperties = configProperties.getIfAvailable();
        this.discoveryProperties = discoveryProperties.getIfAvailable();
        this.scheduler = scheduler;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        scheduler.schedule(this::reportStatus, PROBE_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * 报告一次 Nacos 可达性状态,以 {@code INFO} 或 {@code WARN} 各打印一行.
     * <p>
     * 设计为包内可见,便于测试中直接调用.
     * </p>
     */
    void reportStatus() {
        Endpoint endpoint = resolvePrimaryEndpoint();
        if (endpoint == null) {
            LOGGER.warn("[nacos-status] 未能解析出 Nacos server-addr, 跳过启动期可达性探测。");
            return;
        }
        try {
            if (tryConnect(endpoint.host(), endpoint.port(), PROBE_TIMEOUT_MS)) {
                LOGGER.info("[nacos-status] Nacos 在 {}:{} 可达, 配置中心 / 注册中心已就绪。", endpoint.host(), endpoint.port());
            } else {
                LOGGER.warn("[nacos-status] Nacos 在 {}:{} 暂不可达, 应用继续以本地兜底配置启动, 客户端会在后台持续重试。",
                        endpoint.host(), endpoint.port());
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 选择主配置端的 Nacos server-addr,优先取 config 端,缺失时回退到 discovery 端.
     * 多地址（逗号分隔）取首段.
     *
     * @return 解析得到的主端点;两者都缺失时返回 {@code null}
     */
    Endpoint resolvePrimaryEndpoint() {
        String raw = null;
        if (configProperties != null) {
            raw = configProperties.getServerAddr();
        }
        if (raw == null || raw.isBlank()) {
            if (discoveryProperties != null) {
                raw = discoveryProperties.getServerAddr();
            }
        }
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return Endpoint.parseFirst(raw);
    }

    /**
     * 用普通 {@link Socket} 做一次 TCP 握手,探测目标主机端口是否接受连接.
     *
     * @param host       目标主机
     * @param port       目标端口
     * @param timeoutMs  连接超时（毫秒）
     * @return 建立连接成功返回 {@code true};被远端拒绝 / 超时返回 {@code false}
     * @throws InterruptedException 线程在等待连接时被中断
     */
    static boolean tryConnect(String host, int port, int timeoutMs) throws InterruptedException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Nacos 端点描述,记录主机与端口.
     *
     * @param host 主机
     * @param port 端口
     */
    record Endpoint(String host, int port) {
        static final int DEFAULT_PORT = 8848;

        /**
         * 解析形如 {@code "host:port"} 的单个端点;无端口或端口非法时使用默认 {@value DEFAULT_PORT}.
         *
         * @param raw 一段端点字符串
         * @return 解析得到的端点
         */
        static Endpoint parseFirst(String raw) {
            String segment = raw.split(",", 2)[0].trim();
            int colon = segment.lastIndexOf(':');
            if (colon < 0) {
                return new Endpoint(segment, DEFAULT_PORT);
            }
            String host = segment.substring(0, colon);
            int port;
            try {
                port = Integer.parseInt(segment.substring(colon + 1));
            } catch (NumberFormatException nfe) {
                port = DEFAULT_PORT;
            }
            return new Endpoint(host, port);
        }
    }
}
