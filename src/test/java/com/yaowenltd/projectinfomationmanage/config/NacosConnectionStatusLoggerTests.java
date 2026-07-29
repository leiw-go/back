/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.alibaba.cloud.nacos.NacosConfigProperties;
import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * {@link NacosConnectionStatusLogger} 的纯单元测试 —— 不启动 Spring 上下文,
 * 不连 Nacos,通过本地 {@link ServerSocket} 模拟"可达"与"不可达"两种情形.
 */
class NacosConnectionStatusLoggerTests {

    private Logger target;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        target = (Logger) LoggerFactory.getLogger(NacosConnectionStatusLogger.class);
        appender = new ListAppender<>();
        appender.start();
        target.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        target.detachAppender(appender);
        appender.stop();
    }

    @Test
    void endpointParseFirstExtractsHostAndPort() {
        NacosConnectionStatusLogger.Endpoint ep = NacosConnectionStatusLogger.Endpoint.parseFirst("nacos.local:8848");
        assertEquals("nacos.local", ep.host());
        assertEquals(8848, ep.port());
    }

    @Test
    void endpointParseFirstTakesFirstOfMultipleAddresses() {
        NacosConnectionStatusLogger.Endpoint ep = NacosConnectionStatusLogger.Endpoint.parseFirst("a:1,b:2,c");
        assertEquals("a", ep.host());
        assertEquals(1, ep.port());
    }

    @Test
    void endpointParseFirstDefaultsTo8848WhenPortMissing() {
        NacosConnectionStatusLogger.Endpoint ep = NacosConnectionStatusLogger.Endpoint.parseFirst("localhost");
        assertEquals("localhost", ep.host());
        assertEquals(8848, ep.port());
    }

    @Test
    void endpointParseFirstDefaultsTo8848WhenPortInvalid() {
        NacosConnectionStatusLogger.Endpoint ep = NacosConnectionStatusLogger.Endpoint.parseFirst("localhost:notaport");
        assertEquals("localhost", ep.host());
        assertEquals(8848, ep.port());
    }

    @Test
    void tryConnectReturnsTrueWhenPortAccepts() throws Exception {
        try (ServerSocket ss = new ServerSocket(0)) {
            int port = ss.getLocalPort();
            Thread acceptor = new Thread(() -> {
                try {
                    ss.setSoTimeout(2000);
                    ss.accept();
                } catch (IOException ignored) {
                    // 测试结束自然关闭
                }
            }, "test-acceptor");
            acceptor.setDaemon(true);
            acceptor.start();

            assertTrue(NacosConnectionStatusLogger.tryConnect("127.0.0.1", port, 1000));
        }
    }

    @Test
    void tryConnectReturnsFalseWhenPortClosed() throws Exception {
        // 找一个一定未被占用的端口:绑定后立即关闭,Linux 上通常不会立刻被回收
        // 但连接 127.0.0.1:1 这种特权端口几乎肯定 Connection refused
        try (ServerSocket ss = new ServerSocket(0)) {
            int port = ss.getLocalPort();
            // 立刻关闭 ss,让 tryConnect 失败
        }
        // 此时该端口已被释放;为了避免误连到其它服务,改用 127.0.0.1:1
        // (Connection refused 在 macOS / Linux 上都是毫秒级返回)
        assertFalse(NacosConnectionStatusLogger.tryConnect("127.0.0.1", 1, 500));
    }

    @Test
    void reportStatusLogsWarnWhenNacosUnreachable() {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        try {
            NacosConnectionStatusLogger instance = newInstance("127.0.0.1", 1, executor);
            instance.reportStatus();
        } finally {
            executor.shutdownNow();
        }

        ILoggingEvent warn = findEvent(Level.WARN, "[nacos-status]");
        assertNotNull(warn, "expected a [nacos-status] WARN log when Nacos is unreachable");
        assertTrue(warn.getFormattedMessage().contains("127.0.0.1:1"),
                "WARN message should mention the unreachable endpoint");
    }

    @Test
    void reportStatusLogsInfoWhenNacosReachable() throws Exception {
        try (ServerSocket ss = new ServerSocket(0)) {
            int port = ss.getLocalPort();
            startAcceptor(ss);

            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
            try {
                NacosConnectionStatusLogger instance = newInstance("127.0.0.1", port, executor);
                instance.reportStatus();
            } finally {
                executor.shutdownNow();
            }

            ILoggingEvent info = findEvent(Level.INFO, "[nacos-status]");
            assertNotNull(info, "expected a [nacos-status] INFO log when Nacos is reachable");
            assertTrue(info.getFormattedMessage().contains("127.0.0.1:" + port),
                    "INFO message should mention the reachable endpoint");
        }
    }

    @Test
    void resolvePrimaryEndpointFallsBackToDiscoveryWhenConfigMissing() {
        @SuppressWarnings("unchecked")
        ObjectProvider<NacosConfigProperties> configProvider = Mockito.mock(ObjectProvider.class);
        when(configProvider.getIfAvailable()).thenReturn(null);
        @SuppressWarnings("unchecked")
        ObjectProvider<NacosDiscoveryProperties> discoveryProvider = Mockito.mock(ObjectProvider.class);
        NacosDiscoveryProperties discoveryProps = new NacosDiscoveryProperties();
        discoveryProps.setServerAddr("discovery.local:9000");
        when(discoveryProvider.getIfAvailable()).thenReturn(discoveryProps);

        NacosConnectionStatusLogger instance = new NacosConnectionStatusLogger(
                configProvider, discoveryProvider, Executors.newSingleThreadScheduledExecutor());

        NacosConnectionStatusLogger.Endpoint ep = instance.resolvePrimaryEndpoint();
        assertNotNull(ep, "should fall back to discovery when config is absent");
        assertEquals("discovery.local", ep.host());
        assertEquals(9000, ep.port());
    }

    @Test
    void resolvePrimaryEndpointReturnsNullWhenBothMissing() {
        @SuppressWarnings("unchecked")
        ObjectProvider<NacosConfigProperties> configProvider = Mockito.mock(ObjectProvider.class);
        when(configProvider.getIfAvailable()).thenReturn(null);
        @SuppressWarnings("unchecked")
        ObjectProvider<NacosDiscoveryProperties> discoveryProvider = Mockito.mock(ObjectProvider.class);
        when(discoveryProvider.getIfAvailable()).thenReturn(null);

        NacosConnectionStatusLogger instance = new NacosConnectionStatusLogger(
                configProvider, discoveryProvider, Executors.newSingleThreadScheduledExecutor());

        assertEquals(null, instance.resolvePrimaryEndpoint(),
                "should return null when both providers are unavailable");
    }

    private static NacosConnectionStatusLogger newInstance(String addr, int port, ScheduledExecutorService executor) {
        NacosConfigProperties props = new NacosConfigProperties();
        props.setServerAddr(addr + ":" + port);
        @SuppressWarnings("unchecked")
        ObjectProvider<NacosConfigProperties> configProvider = Mockito.mock(ObjectProvider.class);
        when(configProvider.getIfAvailable()).thenReturn(props);
        @SuppressWarnings("unchecked")
        ObjectProvider<NacosDiscoveryProperties> discoveryProvider = Mockito.mock(ObjectProvider.class);
        when(discoveryProvider.getIfAvailable()).thenReturn(null);
        return new NacosConnectionStatusLogger(configProvider, discoveryProvider, executor);
    }

    private static void startAcceptor(ServerSocket ss) {
        Thread acceptor = new Thread(() -> {
            try {
                while (!ss.isClosed()) {
                    try {
                        ss.accept();
                    } catch (IOException e) {
                        return;
                    }
                }
            } catch (Exception ignored) {
                // 守护线程,异常吃掉即可
            }
        }, "test-acceptor");
        acceptor.setDaemon(true);
        acceptor.start();
    }

    private ILoggingEvent findEvent(Level level, String messageSnippet) {
        List<ILoggingEvent> events = appender.list;
        for (ILoggingEvent e : events) {
            if (e.getLevel() == level && e.getFormattedMessage().contains(messageSnippet)) {
                return e;
            }
        }
        return null;
    }
}
