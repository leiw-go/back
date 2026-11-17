/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.openai;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 解析上游 OpenAI 兼容 SSE 流 —— 把字节流转成一个个 {@code data: <json>} 行.
 * <p>
 * SSE 协议规定事件以空行（{@code \n\n}）分隔，每行以 {@code field: value} 形式组织。
 * 我们只关心 {@code data:} 字段：去掉前缀，trim，作为一段 JSON 字符串返回；
 * 遇到 {@code [DONE]} 哨兵返回 null 表示流结束。
 * </p>
 * <p>
 * 非 data 行（{@code event:} / {@code id:} / {@code retry:} / {@code :comment}）跳过。
 * </p>
 *
 * @since 2026-08-04
 */
public final class SseLineParser {

    /** OpenAI 流式哨兵. */
    public static final String DONE_SENTINEL = "[DONE]";

    private SseLineParser() {
    }

    /**
     * 从 InputStream 读取一个 {@code data:} 行的内容.
     *
     * @param input 上游响应字节流（{@code text/event-stream}）
     * @return data 行内容（trim 过）；流结束时返回 {@link #DONE_SENTINEL}；遇到 IO 异常抛出
     * @throws IOException IO 异常
     */
    public static String readNextDataLine(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                // 事件分隔空行，继续读
                continue;
            }
            if (line.startsWith(":")) {
                // 注释行
                continue;
            }
            if (line.startsWith("data:")) {
                String data = line.substring("data:".length()).trim();
                return data;
            }
            // event: / id: / retry: 等其它字段忽略
        }
        return DONE_SENTINEL;
    }

    /**
     * 包装 InputStream 为 {@link java.io.BufferedReader}，方便循环调用.
     *
     * @param input 字节流
     * @return BufferedReader
     */
    public static BufferedReader toReader(InputStream input) {
        return new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
    }
}