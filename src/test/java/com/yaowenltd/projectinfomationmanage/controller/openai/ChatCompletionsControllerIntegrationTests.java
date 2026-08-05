/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller.openai;

import com.yaowenltd.projectinfomationmanage.service.openai.OpenAiRelayService;
import com.yaowenltd.projectinfomationmanage.service.openai.dto.ChatCompletionRequest;
import com.yaowenltd.projectinfomationmanage.service.openai.dto.ChatCompletionResponse;
import com.yaowenltd.projectinfomationmanage.testsupport.JwtTestTokenFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ChatCompletionsController 集成测试 —— 覆盖 {@code POST /v1/chat/completions} 的 stream/non-stream × 三态。
 * <p>
 * <strong>Mock 策略：</strong>{@code @MockBean OpenAiRelayService} 替换真实服务 bean，
 * stub {@code chatCompletion(...)} 直接返回固定响应 / SseEmitter。这样测试只覆盖
 * controller 自身的请求解析 + 响应透传 + 流式分流，不依赖任何上游 provider / 网络。
 * </p>
 * <p>
 * <strong>关于 UsageRecorder：</strong>{@code OpenAiRelayService.chatCompletion} 内部会调
 * {@code usageRecorder.record(...)} 写 {@code t_llm_request_log}。本测试 mock 整个
 * relay service —— record 调用被屏蔽，无法断言落库。要验证 record 调用应单写
 * {@code UsageRecorderTests}（不在本轮范围）。
 * </p>
 *
 * @since 2026-08-05
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("all-endpoints-it")
class ChatCompletionsControllerIntegrationTests {

    private static final String JWT_SECRET_BASE64 = generateRandomSecret();

    private static String generateRandomSecret() {
        byte[] keyBytes = new byte[64];
        new SecureRandom().nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    @DynamicPropertySource
    static void registerJwtSecret(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> JWT_SECRET_BASE64);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTestTokenFactory jwtFactory;

    @MockBean
    private OpenAiRelayService relay;

    private static final String VALID_REQUEST_BODY = "{"
            + "\"model\":\"openai:gpt-4o-mini\","
            + "\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],"
            + "\"stream\":false"
            + "}";

    private static ChatCompletionResponse sampleResponse() {
        ChatCompletionResponse resp = new ChatCompletionResponse();
        resp.setId("chatcmpl-test-123");
        resp.setObject("chat.completion");
        resp.setCreated(1700000000L);
        resp.setModel("gpt-4o-mini");

        ChatCompletionResponse.Choice choice = new ChatCompletionResponse.Choice();
        choice.setIndex(0);
        ChatCompletionResponse.Message msg = new ChatCompletionResponse.Message();
        msg.setRole("assistant");
        msg.setContent("hello back");
        choice.setMessage(msg);
        choice.setFinishReason("stop");
        resp.setChoices(List.of(choice));

        ChatCompletionResponse.Usage usage = new ChatCompletionResponse.Usage();
        usage.setPromptTokens(10);
        usage.setCompletionTokens(20);
        usage.setTotalTokens(30);
        resp.setUsage(usage);
        return resp;
    }

    // =================== stream=false ===================

    @Test
    void chatCompletions_streamFalse_admin_returnsResponseEntity_andAssertsJsonShape() throws Exception {
        when(relay.chatCompletion(any(ChatCompletionRequest.class), anyString(), any()))
                .thenReturn(ResponseEntity.ok(sampleResponse()));

        mockMvc.perform(post("/v1/chat/completions")
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("chatcmpl-test-123"))
                .andExpect(jsonPath("$.object").value("chat.completion"))
                .andExpect(jsonPath("$.choices[0].message.role").value("assistant"))
                .andExpect(jsonPath("$.choices[0].message.content").value("hello back"))
                .andExpect(jsonPath("$.usage.total_tokens").value(30));

        // 验证 controller 把 JWT subject 作为 userId 传给了 relay
        ArgumentCaptor<ChatCompletionRequest> reqCap = ArgumentCaptor.forClass(ChatCompletionRequest.class);
        ArgumentCaptor<String> userCap = ArgumentCaptor.forClass(String.class);
        verify(relay).chatCompletion(reqCap.capture(), userCap.capture(), any());
        assertEquals("admin", userCap.getValue());
        assertEquals("openai:gpt-4o-mini", reqCap.getValue().getModel());
    }

    @Test
    void chatCompletions_streamFalse_userRole_returns200() throws Exception {
        when(relay.chatCompletion(any(ChatCompletionRequest.class), anyString(), any()))
                .thenReturn(ResponseEntity.ok(sampleResponse()));

        mockMvc.perform(post("/v1/chat/completions")
                        .header("Authorization", jwtFactory.bearer("testuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("chatcmpl-test-123"));
    }

    @Test
    void chatCompletions_noToken_returns401() throws Exception {
        mockMvc.perform(post("/v1/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== stream=true ===================

    /**
     * 流式：controller 返回 SseEmitter。用 {@code asyncDispatch} 把 MockMvc 切到异步模式后，
     * emitter.complete() 后断言 status + Content-Type = text/event-stream。
     */
    @Test
    void chatCompletions_streamTrue_admin_returnsSseEmitter_andCompletesImmediately() throws Exception {
        SseEmitter emitter = new SseEmitter();
        // 立即 complete —— 不阻塞测试线程
        emitter.complete();

        when(relay.chatCompletion(any(ChatCompletionRequest.class), anyString(), any()))
                .thenReturn(emitter);

        MvcResult mvcResult = mockMvc.perform(post("/v1/chat/completions")
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"model\":\"openai:gpt-4o-mini\","
                                + "\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],"
                                + "\"stream\":true}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        // 异步派发后等 emitter 完成
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string(HttpHeaders.CONTENT_TYPE, org.hamcrest.Matchers.startsWith("text/event-stream")));

        verify(relay).chatCompletion(any(ChatCompletionRequest.class), eq("admin"), any());
    }

    @Test
    void chatCompletions_streamTrue_userRole_returnsSseEmitter() throws Exception {
        SseEmitter emitter = new SseEmitter();
        emitter.complete();

        when(relay.chatCompletion(any(ChatCompletionRequest.class), anyString(), any()))
                .thenReturn(emitter);

        MvcResult mvcResult = mockMvc.perform(post("/v1/chat/completions")
                        .header("Authorization", jwtFactory.bearer("testuser"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"model\":\"openai:gpt-4o-mini\","
                                + "\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],"
                                + "\"stream\":true}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk());

        verify(relay).chatCompletion(any(ChatCompletionRequest.class), eq("testuser"), any());
    }
}