/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.email;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaowenltd.projectinfomationmanage.service.openai.SseFrame;
import com.yaowenltd.projectinfomationmanage.service.openai.SseFrameTranslator;
import com.yaowenltd.projectinfomationmanage.service.openai.SseUpstreamFrame;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 邮件引擎的帧翻译器：上游 OpenAI 帧 → 业务事件协议.
 * <p>
 * 事件协议：
 * <ul>
 *   <li>{@code meta} —— 仅首帧：模型 / 语言 / 语气 / 场景</li>
 *   <li>{@code reasoning} —— 推理型模型的思考过程增量</li>
 *   <li>{@code delta} —— 正文增量</li>
 *   <li>{@code done} —— 末帧：结束原因 + 耗时 + token 用量</li>
 * </ul>
 * 上游帧序（MiniMax M3.1 实测）：
 * <pre>
 * delta.reasoning_content（思考，多帧）→ delta.content（正文，多帧）
 *   → choices:[] 且带 usage 的收尾帧 → [DONE] 哨兵
 * </pre>
 *
 * @since 2026-11-17
 */
public class CrmEmailEngineFrameTranslator implements SseFrameTranslator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrmEmailEngineFrameTranslator.class);

    private final ObjectMapper objectMapper;
    private final String model;
    private final CrmEmailEngineLanguage language;
    private final CrmEmailEngineTone tone;
    private final CrmEmailEngineScene scene;
    private final String requestId;
    private final long start;
    private final String systemPrompt;
    private final String userPrompt;
    /** 生成完成后的回调 —— 用来把结果落进生成历史；可为空（不落库）. */
    private final Consumer<CrmEmailEngineGenerated> onCompleted;

    private boolean metaSent;
    private String finishReason;
    /** 累计正文 —— 末帧据此拆出「主题 + 正文」. */
    private final StringBuilder contentBuffer = new StringBuilder();

    /**
     * 构造翻译器.
     *
     * @param objectMapper JSON 序列化
     * @param model        模型名（{@code provider:model}）
     * @param language     目标语言
     * @param tone         语气
     * @param scene        场景
     * @param requestId    请求 ID
     * @param start        开始时间戳（毫秒）
     * @param systemPrompt 实际发送的系统提示（回传给前端，便于核对改写依据）
     * @param userPrompt   实际发送的用户提示（回传给前端）
     */
    public CrmEmailEngineFrameTranslator(ObjectMapper objectMapper,
                                          String model,
                                          CrmEmailEngineLanguage language,
                                          CrmEmailEngineTone tone,
                                          CrmEmailEngineScene scene,
                                          String requestId,
                                          long start,
                                          String systemPrompt,
                                          String userPrompt) {
        this(objectMapper, model, language, tone, scene, requestId, start, systemPrompt, userPrompt, null);
    }

    /**
     * 构造翻译器（带生成完成回调）.
     *
     * @param objectMapper JSON 序列化
     * @param model        模型名（{@code provider:model}）
     * @param language     目标语言
     * @param tone         语气
     * @param scene        场景
     * @param requestId    请求 ID
     * @param start        开始时间戳（毫秒）
     * @param systemPrompt 实际发送的系统提示（回传给前端，便于核对改写依据）
     * @param userPrompt   实际发送的用户提示（回传给前端）
     * @param onCompleted  生成完成回调（落生成历史用）；可为空
     */
    public CrmEmailEngineFrameTranslator(ObjectMapper objectMapper,
                                          String model,
                                          CrmEmailEngineLanguage language,
                                          CrmEmailEngineTone tone,
                                          CrmEmailEngineScene scene,
                                          String requestId,
                                          long start,
                                          String systemPrompt,
                                          String userPrompt,
                                          Consumer<CrmEmailEngineGenerated> onCompleted) {
        this.objectMapper = objectMapper;
        this.model = model;
        this.language = language;
        this.tone = tone;
        this.scene = scene;
        this.requestId = requestId;
        this.start = start;
        this.systemPrompt = systemPrompt;
        this.userPrompt = userPrompt;
        this.onCompleted = onCompleted;
    }

    @Override
    public List<SseFrame> translate(SseUpstreamFrame frame) {
        List<SseFrame> out = new ArrayList<>(2);
        if (!metaSent) {
            metaSent = true;
            out.add(SseFrame.named("meta", toJson(metaPayload())));
        }
        if (frame.isDone()) {
            // 先落历史再发 done —— 前端收到 done 就去查列表，不会撞上还没提交的记录
            notifyCompleted(frame);
            out.add(SseFrame.named("done", toJson(donePayload(frame))));
            return out;
        }
        if (frame.getFinishReason() != null) {
            finishReason = frame.getFinishReason();
        }
        if (frame.getReasoning() != null && !frame.getReasoning().isEmpty()) {
            Map<String, Object> payload = new LinkedHashMap<>();
            // 字段名刻意用 thinking 而不是 content —— 前端"只按 content 拼接"的写法
            // 不会把思考过程混进正文
            payload.put("thinking", frame.getReasoning());
            out.add(SseFrame.named("reasoning", toJson(payload)));
        }
        if (frame.getContent() != null && !frame.getContent().isEmpty()) {
            contentBuffer.append(frame.getContent());
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("content", frame.getContent());
            out.add(SseFrame.named("delta", toJson(payload)));
        }
        return out;
    }

    /**
     * 通知上层「生成已完成」—— 用来把结果落进生成历史.
     * <p>
     * 落库失败不能影响邮件本身 —— 吞掉异常只记日志。
     *
     * @param frame 结束哨兵帧（带累计 usage）
     */
    private void notifyCompleted(SseUpstreamFrame frame) {
        if (onCompleted == null) {
            return;
        }
        try {
            CrmEmailEngineOutputParser.Parsed parsed =
                    CrmEmailEngineOutputParser.parse(contentBuffer.toString());
            onCompleted.accept(new CrmEmailEngineGenerated(
                    requestId,
                    parsed.subject(),
                    parsed.body(),
                    model,
                    language.getCode(),
                    tone.getCode(),
                    scene.getCode(),
                    System.currentTimeMillis() - start,
                    frame.getPromptTokens(),
                    frame.getCompletionTokens(),
                    frame.getTotalTokens()));
        } catch (Exception e) {
            LOGGER.warn("保存邮件生成历史失败: {}", e.getMessage());
        }
    }

    /**
     * 首帧载荷.
     * <p>
     * 带上本次实际发送的两段提示词 —— 前端可以把它折叠展示出来，
     * 让业务同事核对「AI 是按什么依据改的」。
     *
     * @return 载荷
     */
    private Map<String, Object> metaPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", requestId);
        payload.put("model", model);
        payload.put("language", language.getCode());
        payload.put("languageLabel", language.getLabel());
        payload.put("tone", tone.getCode());
        payload.put("toneLabel", tone.getLabel());
        payload.put("scene", scene.getCode());
        payload.put("sceneLabel", scene.getLabel());
        payload.put("systemPrompt", systemPrompt);
        payload.put("userPrompt", userPrompt);
        return payload;
    }

    /**
     * 末帧载荷.
     *
     * @param frame 结束哨兵帧（带累计 usage）
     * @return 载荷
     */
    private Map<String, Object> donePayload(SseUpstreamFrame frame) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", requestId);
        payload.put("finishReason", finishReason);
        payload.put("elapsedMs", System.currentTimeMillis() - start);
        // 主题与正文拆开给前端 —— 前端可以分别展示 / 分别复制
        CrmEmailEngineOutputParser.Parsed parsed = CrmEmailEngineOutputParser.parse(contentBuffer.toString());
        payload.put("subject", parsed.subject());
        payload.put("body", parsed.body());
        Map<String, Object> usage = new LinkedHashMap<>();
        usage.put("promptTokens", frame.getPromptTokens() == null ? 0 : frame.getPromptTokens());
        usage.put("completionTokens", frame.getCompletionTokens() == null ? 0 : frame.getCompletionTokens());
        usage.put("totalTokens", frame.getTotalTokens() == null ? 0 : frame.getTotalTokens());
        payload.put("usage", usage);
        return payload;
    }

    /**
     * 序列化成 JSON 字符串.
     *
     * @param value 对象
     * @return JSON 字符串
     */
    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
