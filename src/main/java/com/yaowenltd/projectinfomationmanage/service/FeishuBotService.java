/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service;

/**
 * 飞书机器人推送服务.
 * <p>
 * 封装向飞书自定义机器人 webhook 发送消息的能力.
 * 当前实现主要面向 text 类型消息，满足定时任务推送简单文本（如
 * "hello world"）以及后续业务告警文本的需求.
 * </p>
 */
public interface FeishuBotService {

    /**
     * 向飞书机器人推送一段纯文本消息.
     *
     * @param text 要发送的文本内容
     * @return 推送是否成功 —— true 表示飞书返回 StatusCode 为 0
     */
    boolean sendText(String text);
}