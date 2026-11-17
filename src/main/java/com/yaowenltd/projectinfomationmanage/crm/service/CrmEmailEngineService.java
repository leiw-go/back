/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailEngineOptionsDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailEngineRewriteRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * CRM 邮件引擎服务.
 * <p>
 * 把一段粗糙的基础邮件改写成专业、地道、对应目标语言的商务邮件，
 * 通过大模型中转（{@code /design/v1/chat/completions}）流式返回。
 * <p>
 * 输出协议（不是 OpenAI 帧，是给前端用的业务协议）：
 * <ul>
 *   <li>{@code meta} —— 仅首帧：本次调用的模型 / 语言 / 语气 / 场景</li>
 *   <li>{@code reasoning} —— 可选：推理型模型的思考过程增量</li>
 *   <li>{@code delta} —— 正文增量（前端打字机直接 append）</li>
 *   <li>{@code done} —— 末帧：结束原因 + 耗时 + token 用量</li>
 *   <li>{@code error} —— 出错时的唯一一帧</li>
 * </ul>
 *
 * @since 2026-11-17
 */
public interface CrmEmailEngineService {

    /**
     * AI 改写（流式）.
     *
     * @param request 改写请求
     * @param userId  调用方用户（JWT subject，用于用量归属）
     * @return SSE 输出
     */
    SseEmitter rewrite(CrmEmailEngineRewriteRequest request, String userId);

    /**
     * 查询可选项（语言 / 语气 / 场景 / 模型）.
     *
     * @return 可选项
     */
    CrmEmailEngineOptionsDto options();
}
