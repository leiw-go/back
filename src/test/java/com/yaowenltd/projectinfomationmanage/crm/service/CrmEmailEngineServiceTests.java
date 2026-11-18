/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailEngineRewriteRequest;
import com.yaowenltd.projectinfomationmanage.crm.service.email.CrmEmailEnginePromptBuilder;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmEmailEngineHistoryService;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.CrmEmailEngineServiceImpl;
import com.yaowenltd.projectinfomationmanage.service.openai.OpenAiRelayService;
import com.yaowenltd.projectinfomationmanage.service.openai.SseFrame;
import com.yaowenltd.projectinfomationmanage.service.openai.SseFrameTranslator;
import com.yaowenltd.projectinfomationmanage.service.openai.SseUpstreamFrame;
import com.yaowenltd.projectinfomationmanage.service.openai.dto.ChatCompletionRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CrmEmailEngineService} 的单元测试.
 * <p>
 * 只验证「拼 prompt + 组装上游请求 + 挂上翻译器」这一层，
 * 不连真实大模型（上游链路由中转自己的测试覆盖）。
 *
 * @since 2026-11-17
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("邮件引擎服务")
class CrmEmailEngineServiceTests {

    @Mock
    private OpenAiRelayService relayService;

    @Mock
    private CrmEmailEngineHistoryService historyService;

    private CrmEmailEngineServiceImpl service() {
        return new CrmEmailEngineServiceImpl(new CrmEmailEnginePromptBuilder(),
                relayService, new ObjectMapper(), historyService);
    }

    private CrmEmailEngineRewriteRequest request() {
        CrmEmailEngineRewriteRequest request = new CrmEmailEngineRewriteRequest();
        request.setContent("We want your quotation for 1000 pcs LED strip.");
        request.setLanguage("en");
        request.setTone("new_customer");
        request.setScene("quotation");
        return request;
    }

    @Test
    @DisplayName("草稿与特殊要求都为空时直接拒绝")
    void rejectsEmptyRequest() {
        CrmEmailEngineRewriteRequest request = request();
        request.setContent("   ");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service().rewrite(request, "admin"));

        assertTrue(error.getMessage().contains("请填写草稿内容"));
    }

    @Test
    @DisplayName("没有草稿但给了特殊要求 —— 允许直接生成")
    void allowsGenerateWithoutDraft() {
        when(relayService.chatCompletion(any(), anyString(), anyString(), any())).thenReturn(new SseEmitter(0L));
        CrmEmailEngineRewriteRequest request = request();
        request.setContent(null);
        request.setCustomInstruction("客户展会后要跟进，强调交期");

        SseEmitter emitter = service().rewrite(request, "admin");

        assertNotNull(emitter);
    }

    @Test
    @DisplayName("超长内容直接拒绝")
    void rejectsTooLongContent() {
        CrmEmailEngineRewriteRequest request = request();
        request.setContent("x".repeat(CrmEmailEngineServiceImpl.MAX_CONTENT_LENGTH + 1));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service().rewrite(request, "admin"));

        assertTrue(error.getMessage().contains("过长"));
    }

    @Test
    @DisplayName("请求以 system + user 两条消息发给中转")
    void buildsUpstreamRequest() {
        when(relayService.chatCompletion(any(), anyString(), anyString(), any())).thenReturn(new SseEmitter(0L));

        service().rewrite(request(), "admin");

        ArgumentCaptor<ChatCompletionRequest> captor = ArgumentCaptor.forClass(ChatCompletionRequest.class);
        verify(relayService).chatCompletion(captor.capture(), eq("admin"), anyString(), any());

        ChatCompletionRequest upstream = captor.getValue();
        assertEquals(CrmEmailEngineServiceImpl.DEFAULT_MODEL, upstream.getModel());
        assertEquals(Boolean.TRUE, upstream.getStream());
        assertEquals(CrmEmailEngineServiceImpl.DEFAULT_TEMPERATURE, upstream.getTemperature());
        assertEquals(CrmEmailEngineServiceImpl.DEFAULT_MAX_TOKENS, upstream.getMaxTokens());
        assertEquals(2, upstream.getMessages().size());
        assertEquals("system", upstream.getMessages().get(0).getRole());
        assertEquals("user", upstream.getMessages().get(1).getRole());
        assertTrue(upstream.getMessages().get(1).getContent().contains("1000 pcs LED strip"));
    }

    @Test
    @DisplayName("挂上帧翻译器 —— 上游帧会被翻译成业务协议")
    void passesFrameTranslator() {
        when(relayService.chatCompletion(any(), anyString(), anyString(), any())).thenReturn(new SseEmitter(0L));

        service().rewrite(request(), "admin");

        ArgumentCaptor<SseFrameTranslator> captor = ArgumentCaptor.forClass(SseFrameTranslator.class);
        verify(relayService).chatCompletion(any(), anyString(), anyString(), captor.capture());

        assertNotNull(captor.getValue());
        List<SseFrame> frames = captor.getValue().translate(new SseUpstreamFrame(
                "{}", false, "hi", null, null, null, null, null));
        assertEquals("meta", frames.get(0).getEvent());
    }

    @Test
    @DisplayName("中转返回错误体时转成一条 error 事件")
    void convertsRelayErrorToErrorEvent() {
        when(relayService.chatCompletion(any(), anyString(), anyString(), any())).thenReturn(
                org.springframework.http.ResponseEntity.badRequest().body(
                        com.yaowenltd.projectinfomationmanage.service.openai.dto.OpenAiError.of(
                                "provider 不存在或已禁用: minimax-cn", "invalid_model", "404")));

        SseEmitter emitter = service().rewrite(request(), "admin");

        assertNotNull(emitter);
    }

    @Test
    @DisplayName("可选项包含语言、语气、场景与默认模型")
    void optionsAreComplete() {
        var options = service().options();

        assertEquals(11, options.getLanguages().size());
        assertEquals(2, options.getTones().size());
        assertEquals("new_customer", options.getTones().get(0).getCode());
        assertEquals("existing_customer", options.getTones().get(1).getCode());
        assertEquals(8, options.getScenes().size());
        assertEquals(CrmEmailEngineServiceImpl.DEFAULT_MODEL, options.getDefaultModel());
        assertTrue(options.getTones().get(0).getHint() != null);
    }
}
