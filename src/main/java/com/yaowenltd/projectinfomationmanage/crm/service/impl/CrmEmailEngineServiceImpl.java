/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailEngineOptionsDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailEngineRewriteRequest;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmEmailEngineService;
import com.yaowenltd.projectinfomationmanage.crm.service.email.CrmEmailEngineFrameTranslator;
import com.yaowenltd.projectinfomationmanage.crm.service.email.CrmEmailEngineLanguage;
import com.yaowenltd.projectinfomationmanage.crm.service.email.CrmEmailEnginePromptBuilder;
import com.yaowenltd.projectinfomationmanage.crm.service.email.CrmEmailEngineScene;
import com.yaowenltd.projectinfomationmanage.crm.service.email.CrmEmailEngineTone;
import com.yaowenltd.projectinfomationmanage.service.openai.OpenAiRelayService;
import com.yaowenltd.projectinfomationmanage.service.openai.dto.ChatCompletionRequest;
import com.yaowenltd.projectinfomationmanage.service.openai.dto.OpenAiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * CRM 邮件引擎服务实现.
 * <p>
 * 上游调用复用大模型中转（provider 路由、密钥解密、用量计量、断连处理），
 * 这里只负责两件事：**拼 prompt** 与**把 OpenAI 帧翻译成业务事件协议**。
 *
 * @since 2026-11-17
 */
@Service
public class CrmEmailEngineServiceImpl implements CrmEmailEngineService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrmEmailEngineServiceImpl.class);

    /** 默认模型（provider:model）—— provider 在 t_llm_provider 里配置. */
    public static final String DEFAULT_MODEL = "minimax-cn:MiniMax-M3.1-Flash";

    /** 原始邮件长度上限（字符）—— 防止超长输入打爆上游. */
    public static final int MAX_CONTENT_LENGTH = 8000;

    /** 默认采样温度 —— 改写要稳定，不要发散. */
    public static final double DEFAULT_TEMPERATURE = 0.3D;

    /** 默认最大生成 token 数 —— 推理模型会先吃思考 token（实测一次简单改写思考就占了 1300+），不能给太小. */
    public static final int DEFAULT_MAX_TOKENS = 4096;

    private final CrmEmailEnginePromptBuilder promptBuilder;
    private final OpenAiRelayService relayService;
    private final ObjectMapper objectMapper;

    /**
     * 使用指定的依赖构造 CrmEmailEngineServiceImpl.
     *
     * @param promptBuilder prompt 构造器
     * @param relayService  大模型中转
     * @param objectMapper  JSON 序列化
     */
    public CrmEmailEngineServiceImpl(CrmEmailEnginePromptBuilder promptBuilder,
                                      OpenAiRelayService relayService,
                                      ObjectMapper objectMapper) {
        this.promptBuilder = promptBuilder;
        this.relayService = relayService;
        this.objectMapper = objectMapper;
    }

    @Override
    public SseEmitter rewrite(CrmEmailEngineRewriteRequest request, String userId) {
        validate(request);
        CrmEmailEngineLanguage language = CrmEmailEngineLanguage.of(request.getLanguage());
        CrmEmailEngineTone tone = CrmEmailEngineTone.of(request.getTone());
        CrmEmailEngineScene scene = CrmEmailEngineScene.of(request.getScene());
        String model = request.getModel() == null || request.getModel().trim().isEmpty()
                ? DEFAULT_MODEL : request.getModel().trim();
        String requestId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();

        String systemPrompt = promptBuilder.buildSystemPrompt(request);
        String userPrompt = promptBuilder.buildUserPrompt(request);

        ChatCompletionRequest upstream = new ChatCompletionRequest();
        upstream.setModel(model);
        upstream.setStream(Boolean.TRUE);
        upstream.setTemperature(request.getTemperature() == null ? DEFAULT_TEMPERATURE : request.getTemperature());
        upstream.setMaxTokens(request.getMaxTokens() == null ? DEFAULT_MAX_TOKENS : request.getMaxTokens());
        upstream.setMessages(List.of(
                message("system", systemPrompt),
                message("user", userPrompt)));

        CrmEmailEngineFrameTranslator translator = new CrmEmailEngineFrameTranslator(
                objectMapper, model, language, tone, scene, requestId, start, systemPrompt, userPrompt);
        Object result = relayService.chatCompletion(upstream, userId, requestId, translator);
        if (result instanceof SseEmitter emitter) {
            return emitter;
        }
        // 中转在模型 / provider 不合法时返回错误体（不是流）—— 转成一条 error 事件
        return errorEmitter(result, requestId);
    }

    @Override
    public CrmEmailEngineOptionsDto options() {
        CrmEmailEngineOptionsDto dto = new CrmEmailEngineOptionsDto();

        List<CrmEmailEngineOptionsDto.Option> languages = new ArrayList<>();
        for (CrmEmailEngineLanguage language : CrmEmailEngineLanguage.values()) {
            languages.add(new CrmEmailEngineOptionsDto.Option(language.getCode(), language.getLabel()));
        }
        dto.setLanguages(languages);

        List<CrmEmailEngineOptionsDto.Option> tones = new ArrayList<>();
        for (CrmEmailEngineTone tone : CrmEmailEngineTone.values()) {
            tones.add(new CrmEmailEngineOptionsDto.Option(tone.getCode(), tone.getLabel(), tone.getHint()));
        }
        dto.setTones(tones);

        List<CrmEmailEngineOptionsDto.Option> scenes = new ArrayList<>();
        for (CrmEmailEngineScene scene : CrmEmailEngineScene.values()) {
            scenes.add(new CrmEmailEngineOptionsDto.Option(scene.getCode(), scene.getLabel(), scene.getHint()));
        }
        dto.setScenes(scenes);

        List<CrmEmailEngineOptionsDto.Option> models = new ArrayList<>();
        models.add(new CrmEmailEngineOptionsDto.Option(DEFAULT_MODEL, "MiniMax M3.1 Flash（默认）"));
        dto.setModels(models);
        dto.setDefaultModel(DEFAULT_MODEL);
        return dto;
    }

    /**
     * 校验改写请求.
     * <p>
     * 草稿（content）与特殊要求（customInstruction）至少要有一个：
     * 都没有的话模型无从写起。
     *
     * @param request 改写请求
     */
    private void validate(CrmEmailEngineRewriteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        boolean hasDraft = request.getContent() != null && !request.getContent().trim().isEmpty();
        boolean hasInstruction = request.getCustomInstruction() != null
                && !request.getCustomInstruction().trim().isEmpty();
        if (!hasDraft && !hasInstruction) {
            throw new IllegalArgumentException("请填写草稿内容，或说明特殊场景与要求");
        }
        if (request.getContent() != null && request.getContent().length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("草稿内容过长（上限 " + MAX_CONTENT_LENGTH + " 字符）");
        }
    }

    /**
     * 构造一条对话消息.
     *
     * @param role    角色
     * @param content 内容
     * @return 对话消息
     */
    private ChatCompletionRequest.Message message(String role, String content) {
        ChatCompletionRequest.Message message = new ChatCompletionRequest.Message();
        message.setRole(role);
        message.setContent(content);
        return message;
    }

    /**
     * 把中转返回的错误体转成一条 error 事件.
     *
     * @param result    中转返回值（错误时是 ResponseEntity）
     * @param requestId 请求 ID
     * @return SSE 输出
     */
    private SseEmitter errorEmitter(Object result, String requestId) {
        SseEmitter emitter = new SseEmitter(0L);
        String message = "大模型调用失败";
        String code = "upstream_error";
        if (result instanceof ResponseEntity<?> entity && entity.getBody() instanceof OpenAiError error
                && error.getError() != null) {
            message = error.getError().getMessage() == null ? message : error.getError().getMessage();
            code = error.getError().getType() == null ? code : error.getError().getType();
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", requestId);
        payload.put("code", code);
        payload.put("message", message);
        try {
            emitter.send(SseEmitter.event().name("error").data(toJson(payload), MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            LOGGER.warn("发送 error 事件失败: {}", e.getMessage());
        }
        emitter.complete();
        return emitter;
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
