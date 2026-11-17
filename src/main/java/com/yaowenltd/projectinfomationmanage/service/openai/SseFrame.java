/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.openai;

/**
 * 翻译后的 SSE 事件.
 * <p>
 * {@link SseFrameTranslator} 把上游 OpenAI 帧翻译成这个对象，
 * 由中转负责按 event / data 写给客户端。
 *
 * @since 2026-11-17
 */
public class SseFrame {

    /** 事件名；为 null 时用默认的 {@code message}. */
    private final String event;

    /** 事件载荷（JSON 字符串）. */
    private final String data;

    /**
     * 构造一个 SSE 事件.
     *
     * @param event 事件名，可为 null
     * @param data  事件载荷
     */
    public SseFrame(String event, String data) {
        this.event = event;
        this.data = data;
    }

    /**
     * 构造默认事件名（{@code message}）的 SSE 事件.
     *
     * @param data 事件载荷
     * @return SSE 事件
     */
    public static SseFrame message(String data) {
        return new SseFrame(null, data);
    }

    /**
     * 构造带事件名的 SSE 事件.
     *
     * @param event 事件名
     * @param data  事件载荷
     * @return SSE 事件
     */
    public static SseFrame named(String event, String data) {
        return new SseFrame(event, data);
    }

    /**
     * 取事件名.
     *
     * @return 事件名，可能为 null
     */
    public String getEvent() {
        return event;
    }

    /**
     * 取事件载荷.
     *
     * @return 事件载荷
     */
    public String getData() {
        return data;
    }
}
