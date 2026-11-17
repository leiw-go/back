/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.openai;

import java.util.List;

/**
 * 上游 SSE 帧翻译器.
 * <p>
 * 中转负责上游调用、provider 路由、用量计量与断连处理；
 * 调用方通过本接口把 OpenAI 帧翻译成自己的事件协议（例如 CRM 邮件引擎的
 * {@code meta / reasoning / delta / done / error}）。
 * <p>
 * 返回空列表表示丢弃该帧（例如上游的思考过程不想透出）；
 * 返回多个表示一个上游帧要产出多个事件（例如首帧同时产出 meta 与 delta）。
 *
 * @since 2026-11-17
 */
@FunctionalInterface
public interface SseFrameTranslator {

    /**
     * 翻译一个上游帧.
     *
     * @param frame 已解析的上游帧
     * @return 要写给客户端的 SSE 事件列表；空列表表示丢弃该帧
     */
    List<SseFrame> translate(SseUpstreamFrame frame);
}
