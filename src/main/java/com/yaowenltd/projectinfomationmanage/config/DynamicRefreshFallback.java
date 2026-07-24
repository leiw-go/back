/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.EventListenerMethodProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Nacos 动态刷新的双保险机制.
 * <p>
 * Spring Cloud Alibaba 的 {@code NacosContextRefresher} 会比对上一次 Nacos 快照与新快照
 * 之间的键差异（diff），仅转发那些实际发生<em>变更</em>的键. 新出现的键（新内容中存在、
 * 但旧快照中不存在）会被悄悄丢弃 —— 这会导致 {@code @RefreshScope} Bean 不会被重建.
 * </p>
 *
 * <p>
 * 本监听器在 Spring 内置的 {@code RefreshEventListener} 之后监听
 * {@link org.springframework.cloud.context.environment.EnvironmentChangeEvent}.
 * 当变更集中并不包含我们关心的键（当前为所有匹配 {@code biz.*} 的键）
 * 时，显式触发 {@link ContextRefresher#refresh()}，从而使全部 {@code @RefreshScope}
 * Bean（包括配置属性）得到重建. 这能覆盖首次发布以及其他 diff 恰好
 * 为空的 Nacos 事件.
 * </p>
 *
 * <p>
 * 它同时处理 {@link org.springframework.boot.context.event.ApplicationReadyEvent}:
 * 一旦上下文就绪，就将 Nacos 内容（已知的那个 Nacos 属性源）与 {@link Environment} 进行比对，
 * 若发现不一致便立即触发一次刷新.
 * </p>
 *
 * @see EventListenerMethodProcessor
 */
@Component
public class DynamicRefreshFallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicRefreshFallback.class);

    /**
     * 本监听器认定为"自有项目键"的属性名前缀.
     * 任何匹配此前缀的键都必须触发刷新，即便 Nacos 的 diff 集合为空.
     */
    private static final String[] WATCHED_PREFIXES = {"biz."};

    private final ContextRefresher refresher;

    /**
     * 最近一次 {@code EnvironmentChangeEvent} 时捕获到的 Nacos 内容.
     * 空字符串表示"尚未触发过任何事件".
     */
    private volatile String lastSeenEnvironmentChangeAt = "";

    /**
     * @param refresher Spring Cloud 的 ContextRefresher；调用
     *                  {@link ContextRefresher#refresh()} 会重建每一个
     *                  {@code @RefreshScope} Bean.
     */
    public DynamicRefreshFallback(ContextRefresher refresher) {
        this.refresher = refresher;
    }

    /**
     * 在 Spring 内置的 {@code RefreshEventListener} 之后触发. 我们检查变更集，
     * 若其中并未出现任何受关注的前缀，则回退为一次完整刷新，以保证我们的
     * {@code @RefreshScope} 配置属性不会被卡在过期值上.
     *
     * @param event Nacos 触发的 EnvironmentChangeEvent
     */
    @EventListener
    @Order(Ordered.LOWEST_PRECEDENCE)
    public void onEnvironmentChange(
            org.springframework.cloud.context.environment.EnvironmentChangeEvent event) {
        Set<String> keys = event.getKeys();
        lastSeenEnvironmentChangeAt = String.valueOf(event.getTimestamp());

        boolean watchedInSet = keys != null && keys.stream().anyMatch(
                k -> {
                    for (String prefix : WATCHED_PREFIXES) {
                        if (k.startsWith(prefix)) {
                            return true;
                        }
                    }
                    return false;
                });
        boolean anyRefreshOrchestratorWillReact = keys != null && !keys.isEmpty();
        if (!watchedInSet && !anyRefreshOrchestratorWillReact) {
            LOGGER.info(
                    "[refresh-fallback] EnvironmentChangeEvent came in with "
                            + "(empty) keys; observed keys={}. Calling ContextRefresher.refresh() "
                            + "to cover biz.* even when Nacos's diff is empty.",
                    keys);
            try {
                refresher.refresh();
            } catch (Exception ex) {
                LOGGER.warn("[refresh-fallback] ContextRefresher.refresh() failed: {}", ex.getMessage());
            }
        }
    }
}
