/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaowenltd.projectinfomationmanage.crm.service.email.CrmEmailEngineFrameTranslator;
import com.yaowenltd.projectinfomationmanage.crm.service.email.CrmEmailEngineLanguage;
import com.yaowenltd.projectinfomationmanage.crm.service.email.CrmEmailEngineScene;
import com.yaowenltd.projectinfomationmanage.crm.service.email.CrmEmailEngineTone;
import com.yaowenltd.projectinfomationmanage.service.openai.SseFrame;
import com.yaowenltd.projectinfomationmanage.service.openai.SseUpstreamFrame;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CrmEmailEngineFrameTranslator} 的单元测试.
 * <p>
 * 上游帧序（MiniMax M3.1 实测）：reasoning → content → usage 收尾帧 → [DONE]。
 * 这里锁定「每个上游帧翻译成什么事件」，前端完全依赖这个协议。
 *
 * @since 2026-11-17
 */
@DisplayName("邮件引擎帧翻译")
class CrmEmailEngineFrameTranslatorTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CrmEmailEngineFrameTranslator translator() {
        return new CrmEmailEngineFrameTranslator(objectMapper, "minimax-cn:MiniMax-M3.1-Flash",
                CrmEmailEngineLanguage.EN, CrmEmailEngineTone.NEW_CUSTOMER, CrmEmailEngineScene.QUOTATION,
                "req-1", System.currentTimeMillis(), "system prompt", "user prompt");
    }

    @Test
    @DisplayName("首帧产出 meta")
    void firstFrameProducesMeta() {
        List<SseFrame> frames = translator().translate(new SseUpstreamFrame(
                "{}", false, "Hello", null, null, null, null, null));

        assertEquals(2, frames.size());
        assertEquals("meta", frames.get(0).getEvent());
        assertTrue(frames.get(0).getData().contains("\"model\":\"minimax-cn:MiniMax-M3.1-Flash\""));
        assertTrue(frames.get(0).getData().contains("\"language\":\"en\""));
        assertTrue(frames.get(0).getData().contains("\"tone\":\"new_customer\""));
        assertTrue(frames.get(0).getData().contains("\"scene\":\"quotation\""));
    }

    @Test
    @DisplayName("meta 帧回传实际发送的提示词")
    void metaCarriesPrompts() {
        List<SseFrame> frames = translator().translate(new SseUpstreamFrame(
                "{}", false, "Hello", null, null, null, null, null));

        assertTrue(frames.get(0).getData().contains("\"systemPrompt\":\"system prompt\""));
        assertTrue(frames.get(0).getData().contains("\"userPrompt\":\"user prompt\""));
    }

    @Test
    @DisplayName("思考增量产出 reasoning 事件（字段名用 thinking，不与正文 content 混淆）")
    void reasoningProducesReasoningEvent() {
        List<SseFrame> frames = translator().translate(new SseUpstreamFrame(
                "{}", false, null, "思考中", null, null, null, null));

        assertEquals(2, frames.size());
        assertEquals("meta", frames.get(0).getEvent());
        assertEquals("reasoning", frames.get(1).getEvent());
        assertTrue(frames.get(1).getData().contains("\"thinking\":\"思考中\""));
        assertFalse(frames.get(1).getData().contains("\"content\""));
    }

    @Test
    @DisplayName("正文增量产出 delta 事件")
    void contentProducesDeltaEvent() {
        List<SseFrame> frames = translator().translate(new SseUpstreamFrame(
                "{}", false, "Dear Mr. Smith,", null, null, null, null, null));

        assertEquals(2, frames.size());
        assertEquals("delta", frames.get(1).getEvent());
        assertTrue(frames.get(1).getData().contains("Dear Mr. Smith,"));
    }

    @Test
    @DisplayName("正文里的引号与换行被正确转义")
    void contentIsJsonEscaped() {
        List<SseFrame> frames = translator().translate(new SseUpstreamFrame(
                "{}", false, "He said \"hi\"\nnext line", null, null, null, null, null));

        String data = frames.get(1).getData();
        assertTrue(data.contains("\\\""), "引号应被转义");
        assertTrue(data.contains("\\n"), "换行应被转义");
    }

    @Test
    @DisplayName("结束哨兵产出 done 事件并带上用量")
    void doneFrameProducesDoneEventWithUsage() {
        CrmEmailEngineFrameTranslator translator = translator();
        translator.translate(new SseUpstreamFrame(
                "{}", false, "done text", null, "stop", null, null, null));

        List<SseFrame> frames = translator.translate(new SseUpstreamFrame(
                "[DONE]", true, null, null, null, 207, 40, 247));

        assertEquals(1, frames.size());
        assertEquals("done", frames.get(0).getEvent());
        assertTrue(frames.get(0).getData().contains("\"finishReason\":\"stop\""));
        assertTrue(frames.get(0).getData().contains("\"promptTokens\":207"));
        assertTrue(frames.get(0).getData().contains("\"completionTokens\":40"));
        assertTrue(frames.get(0).getData().contains("\"totalTokens\":247"));
    }

    @Test
    @DisplayName("meta 只出现一次")
    void metaIsEmittedOnce() {
        CrmEmailEngineFrameTranslator translator = translator();
        translator.translate(new SseUpstreamFrame("{}", false, "a", null, null, null, null, null));

        List<SseFrame> frames = translator.translate(new SseUpstreamFrame(
                "{}", false, "b", null, null, null, null, null));

        assertEquals(1, frames.size());
        assertEquals("delta", frames.get(0).getEvent());
    }

    @Test
    @DisplayName("done 帧把正文拆成主题与正文")
    void doneFrameSplitsSubjectAndBody() {
        CrmEmailEngineFrameTranslator translator = translator();
        translator.translate(new SseUpstreamFrame("{}", false, "Subject: LED strip quotation\n", null, null, null, null, null));
        translator.translate(new SseUpstreamFrame("{}", false, "\nDear Mr. Smith,\n", null, null, null, null, null));
        translator.translate(new SseUpstreamFrame("{}", false, "Could you please quote 1000 pcs?\n", null, null, null, null, null));

        List<SseFrame> frames = translator.translate(new SseUpstreamFrame(
                "[DONE]", true, null, null, null, 10, 20, 30));

        String data = frames.get(0).getData();
        assertTrue(data.contains("\"subject\":\"LED strip quotation\""), "应拆出主题");
        assertTrue(data.contains("Dear Mr. Smith,"), "正文应保留");
        assertTrue(data.contains("Could you please quote 1000 pcs?"), "正文应完整");
        assertFalse(data.contains("Subject: LED strip quotation\\n"), "主题行不应留在正文里");
    }
}
