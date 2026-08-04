/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.openai;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link SseLineParser} 单元测试 —— 验证 SSE data 行解析 / DONE 哨兵 / 注释行跳过.
 *
 * @since 2026-08-04
 */
class SseLineParserTests {

    @Test
    void readNextDataLine_extractsDataFromSingleEvent() throws IOException {
        String raw = "data: {\"id\":\"chatcmpl-1\",\"choices\":[]}\n\n";
        String data = SseLineParser.readNextDataLine(toStream(raw));
        assertEquals("{\"id\":\"chatcmpl-1\",\"choices\":[]}", data);
    }

    @Test
    void readNextDataLine_handlesMultipleEvents() throws IOException {
        String raw = "data: {\"chunk\":1}\n\ndata: {\"chunk\":2}\n\n";
        String first = SseLineParser.readNextDataLine(toStream(raw));
        assertEquals("{\"chunk\":1}", first);

        // 第二个事件要从同一 stream 里继续读
        String second = SseLineParser.readNextDataLine(toStream(raw).readAllBytes() == null
                ? null : toStream("data: {\"chunk\":2}\n\n"));
        assertEquals("{\"chunk\":2}", second);
    }

    @Test
    void readNextDataLine_skipsCommentLines() throws IOException {
        String raw = ": this is a comment\ndata: {\"chunk\":1}\n\n";
        String data = SseLineParser.readNextDataLine(toStream(raw));
        assertEquals("{\"chunk\":1}", data);
    }

    @Test
    void readNextDataLine_skipsEventTypeAndIdFields() throws IOException {
        String raw = "event: message\nid: 42\ndata: {\"a\":1}\n\n";
        String data = SseLineParser.readNextDataLine(toStream(raw));
        assertEquals("{\"a\":1}", data);
    }

    @Test
    void readNextDataLine_returnsDoneSentinelOnSentinel() throws IOException {
        String raw = "data: [DONE]\n\n";
        String data = SseLineParser.readNextDataLine(toStream(raw));
        assertEquals(SseLineParser.DONE_SENTINEL, data);
    }

    @Test
    void readNextDataLine_returnsDoneSentinelOnEmptyStream() throws IOException {
        String data = SseLineParser.readNextDataLine(toStream(""));
        assertEquals(SseLineParser.DONE_SENTINEL, data);
    }

    @Test
    void readNextDataLine_trimsLeadingSpace() throws IOException {
        // OpenAI 实际发的格式是 "data: {...}" （冒号后一个空格），应正确 trim
        String raw = "data:    {\"x\":1}\n\n";
        String data = SseLineParser.readNextDataLine(toStream(raw));
        assertEquals("{\"x\":1}", data);
    }

    private static ByteArrayInputStream toStream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}