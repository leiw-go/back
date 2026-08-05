/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaowenltd.projectinfomationmanage.common.GlobalExceptionHandler;
import com.yaowenltd.projectinfomationmanage.service.llm.ProviderRepository;
import com.yaowenltd.projectinfomationmanage.service.llm.UsageRecorder;
import com.yaowenltd.projectinfomationmanage.service.llm.crypto.AesGcmEncryptor;
import com.yaowenltd.projectinfomationmanage.service.openai.OpenAiRelayService;
import com.yaowenltd.projectinfomationmanage.service.openai.TokenEstimator;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link ChatCompletionsController} 的内容协商回归测试.
 * <p>
 * 复现并锁定这个 bug：OpenAI 兼容客户端发流式请求时会带
 * {@code Accept: text/event-stream}，但请求本身如果有问题（model 格式错、provider 不存在、
 * 上游报错），服务端返回的是 JSON 错误体。若返回的 {@code ResponseEntity} 没有预设
 * Content-Type，Spring 会拿 Accept 做内容协商，找不到能产出 {@code text/event-stream}
 * 的 converter，抛 {@code HttpMediaTypeNotAcceptableException}，客户端只拿到
 * 406 空 body，真实错误原因全部丢失。
 * </p>
 * <p>
 * 断言：带 {@code Accept: text/event-stream} 时，错误响应仍是 JSON + 语义状态码，
 * <strong>不是</strong> 406。
 * </p>
 *
 * @since 2026-08-05
 */
@DisplayName("ChatCompletionsController 内容协商")
class ChatCompletionsControllerContentNegotiationTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProviderRepository providerRepository = mock(ProviderRepository.class);
        // provider 一律查不到 —— 触发 notFound 分支（同样返回 JSON 错误体）
        when(providerRepository.findEnabledByName(anyString())).thenReturn(Optional.empty());

        OpenAiRelayService relay = new OpenAiRelayService(
                mock(RestTemplate.class),
                providerRepository,
                mock(AesGcmEncryptor.class),
                mock(TokenEstimator.class),
                mock(UsageRecorder.class),
                new ObjectMapper(),
                OpenTelemetry.noop());

        mockMvc = MockMvcBuilders.standaloneSetup(new ChatCompletionsController(relay))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * model 缺少 {@code provider:} 前缀 → 400 JSON，即使 Accept 只接受 SSE。
     */
    @Test
    @DisplayName("model 格式非法 + Accept:text/event-stream → 400 JSON，不是 406")
    void malformedModel_returnsJsonError_whenAcceptIsEventStream() throws Exception {
        mockMvc.perform(post("/v1/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"model\":\"gpt-4o-mini\","
                                + "\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],"
                                + "\"stream\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.type").value("invalid_request_error"));
    }

    /**
     * provider 不存在 → 404 JSON，即使 Accept 只接受 SSE。
     */
    @Test
    @DisplayName("provider 不存在 + Accept:text/event-stream → 404 JSON，不是 406")
    void unknownProvider_returnsJsonError_whenAcceptIsEventStream() throws Exception {
        mockMvc.perform(post("/v1/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"model\":\"nosuch:some-model\","
                                + "\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}],"
                                + "\"stream\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error.type").value("invalid_model"));
    }

    /**
     * 非流式请求（不带 Accept）走原路径，行为不受本次修复影响。
     */
    @Test
    @DisplayName("非流式请求 model 格式非法 → 400 JSON")
    void malformedModel_returnsJsonError_whenNoAcceptHeader() throws Exception {
        mockMvc.perform(post("/v1/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"model\":\"gpt-4o-mini\","
                                + "\"messages\":[{\"role\":\"user\",\"content\":\"hi\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
