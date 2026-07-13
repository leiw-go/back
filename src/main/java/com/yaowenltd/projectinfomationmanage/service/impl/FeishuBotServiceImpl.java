/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaowenltd.projectinfomationmanage.config.FeishuWebhookProperties;
import com.yaowenltd.projectinfomationmanage.service.FeishuBotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 飞书机器人推送服务的默认实现.
 * <p>
 * 通过 {@link RestTemplate} POST 一段 JSON 到飞书自定义机器人的 webhook，
 * 请求体遵循飞书官方"自定义机器人"接入文档的 {@code text} 消息格式:
 * <pre>
 * {
 *     "msg_type": "text",
 *     "content": {
 *         "text": "..."
 *     }
 * }
 * </pre>
 * </p>
 *
 * <p>
 * 请求体使用 {@link LinkedHashMap} 显式组装，不依赖任何 POJO 反射，
 * 保证 Jackson 序列化输出的字段顺序与命名完全可预期，便于排障.
 * </p>
 *
 * <p>
 * 当 {@link FeishuWebhookProperties#isEnabled()} 为 false、webhook URL 未配置、
 * 或推送文本为 null 时直接跳过推送并返回 false；任何网络异常或飞书返回非 0
 * 业务码都视为失败，仅记录日志，不向上抛出 —— 定时任务调用方因此不会因为
 * 偶发推送失败而中断后续周期.
 * </p>
 */
@Service
public class FeishuBotServiceImpl implements FeishuBotService {

    private static final Logger LOG = LoggerFactory.getLogger(FeishuBotServiceImpl.class);

    private final RestTemplate restTemplate;

    private final FeishuWebhookProperties properties;

    private final ObjectMapper objectMapper;

    /**
     * 构造 FeishuBotServiceImpl.
     *
     * @param restTemplate 共享的 HTTP 客户端
     * @param properties   飞书 webhook 配置属性
     * @param objectMapper Jackson 序列化器，仅用于排障日志
     */
    public FeishuBotServiceImpl(RestTemplate restTemplate,
                                FeishuWebhookProperties properties,
                                ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * 向飞书机器人推送一段纯文本消息.
     *
     * @param text 要发送的文本内容，传入 null 时视为无操作并返回 false
     * @return 是否推送成功
     */
    @Override
    public boolean sendText(String text) {
        if (!properties.isEnabled()) {
            LOG.debug("飞书机器人推送未启用，跳过推送");
            return false;
        }
        String url = properties.getUrl();
        if (url == null || url.isBlank()) {
            LOG.warn("飞书机器人 webhook URL 未配置，跳过推送");
            return false;
        }
        if (text == null) {
            LOG.warn("飞书机器人推送文本为空，跳过推送");
            return false;
        }

        // 用 LinkedHashMap 显式组装请求体，字段顺序与命名完全确定
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("text", text);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("msg_type", "text");
        payload.put("content", content);

        // 排障：把实际发出的 JSON 打到日志，飞书返回非 0 时直接对比官方文档格式
        String payloadJson = toJson(payload);
        if (payloadJson != null) {
            LOG.info("飞书机器人推送请求体：{}", payloadJson);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        try {
            ResponseEntity<FeishuResponse> response = restTemplate.postForEntity(
                    url, new HttpEntity<>(payload, headers), FeishuResponse.class);
            FeishuResponse body = response.getBody();
            if (body != null && body.code == 0) {
                LOG.info("飞书机器人推送成功，文本长度={}", text.length());
                return true;
            }
            String message = body == null ? "empty body" : ("code=" + body.code + ", msg=" + body.msg);
            LOG.warn("飞书机器人推送失败：{}", message);
            return false;
        } catch (RestClientException ex) {
            LOG.error("飞书机器人推送异常：{}", ex.getMessage(), ex);
            return false;
        }
    }

    /**
     * 将请求体序列化为 JSON 字符串用于日志；序列化失败时返回 null 并打印警告.
     *
     * @param payload 请求体
     * @return 序列化后的 JSON 字符串，失败时返回 null
     */
    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            LOG.warn("序列化飞书推送请求体失败：{}", ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * 飞书 webhook 响应体.
     * <p>
     * 仅关心 {@code code}（0 表示成功）和 {@code msg}，其余字段忽略.
     * </p>
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class FeishuResponse {

        /**
         * 业务状态码，0 表示成功，其它值含义见飞书文档.
         */
        private int code;

        /**
         * 业务状态描述.
         */
        private String msg;

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }
    }
}