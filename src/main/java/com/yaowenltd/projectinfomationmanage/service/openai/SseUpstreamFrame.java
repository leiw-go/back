/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.openai;

/**
 * 上游 SSE 帧（已解析）.
 * <p>
 * 中转把上游 OpenAI 的 {@code data:} 载荷解析后交给 {@link SseFrameTranslator}，
 * 调用方据此产出自己的事件协议，不必再关心 OpenAI 的帧结构。
 *
 * @since 2026-11-17
 */
public class SseUpstreamFrame {

    /** 原始 data 载荷（未解析的字符串，或 {@code [DONE]} 哨兵）. */
    private final String raw;

    /** 是否结束哨兵（上游 {@code data: [DONE]}）. */
    private final boolean done;

    /** 正文增量（{@code choices[0].delta.content}）. */
    private final String content;

    /** 思考过程增量（{@code choices[0].delta.reasoning_content}）—— 推理型模型才有. */
    private final String reasoning;

    /** 结束原因（{@code choices[0].finish_reason}）. */
    private final String finishReason;

    /** 已累计的 prompt token 数. */
    private final Integer promptTokens;

    /** 已累计的 completion token 数. */
    private final Integer completionTokens;

    /** 已累计的总 token 数. */
    private final Integer totalTokens;

    /**
     * 构造一个上游帧.
     *
     * @param raw              原始载荷
     * @param done             是否结束哨兵
     * @param content          正文增量
     * @param reasoning        思考增量
     * @param finishReason     结束原因
     * @param promptTokens     prompt token 数
     * @param completionTokens completion token 数
     * @param totalTokens      总 token 数
     */
    public SseUpstreamFrame(String raw,
                            boolean done,
                            String content,
                            String reasoning,
                            String finishReason,
                            Integer promptTokens,
                            Integer completionTokens,
                            Integer totalTokens) {
        this.raw = raw;
        this.done = done;
        this.content = content;
        this.reasoning = reasoning;
        this.finishReason = finishReason;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
    }

    /**
     * 取原始载荷.
     *
     * @return 原始载荷
     */
    public String getRaw() {
        return raw;
    }

    /**
     * 是否结束哨兵.
     *
     * @return true 表示流已结束
     */
    public boolean isDone() {
        return done;
    }

    /**
     * 取正文增量.
     *
     * @return 正文增量，可能为 null
     */
    public String getContent() {
        return content;
    }

    /**
     * 取思考增量.
     *
     * @return 思考增量，可能为 null
     */
    public String getReasoning() {
        return reasoning;
    }

    /**
     * 取结束原因.
     *
     * @return 结束原因，可能为 null
     */
    public String getFinishReason() {
        return finishReason;
    }

    /**
     * 取 prompt token 数.
     *
     * @return token 数，可能为 null
     */
    public Integer getPromptTokens() {
        return promptTokens;
    }

    /**
     * 取 completion token 数.
     *
     * @return token 数，可能为 null
     */
    public Integer getCompletionTokens() {
        return completionTokens;
    }

    /**
     * 取总 token 数.
     *
     * @return token 数，可能为 null
     */
    public Integer getTotalTokens() {
        return totalTokens;
    }
}
