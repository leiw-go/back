/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.common;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link GlobalExceptionHandler} 在流式请求下的内容协商测试.
 * <p>
 * 锁定这个坑：OpenAI 兼容客户端发流式请求时带 {@code Accept: text/event-stream}，
 * 若此时抛出业务异常（如 token 过期的 {@code UnauthorizedException}），异常处理器返回的
 * 裸 POJO 要走内容协商，而 Jackson 只能产出 {@code application/json} —— 交集为空，
 * 处理器<strong>自己的返回值也写不出去</strong>，异常继续上抛到 servlet，
 * 客户端只拿到一个<strong>空 body</strong>，完全看不到 401 的真实原因。
 * </p>
 * <p>
 * 断言：无论 Accept 是什么，错误体一定是 JSON 且内容完整。
 * </p>
 *
 * @since 2026-08-05
 */
@DisplayName("GlobalExceptionHandler 内容协商")
class GlobalExceptionHandlerContentNegotiationTests {

    private MockMvc mockMvc;

    /** 用于触发各类异常的测试桩 controller. */
    @RestController
    @RequestMapping("/v1")
    static class ThrowingController {

        @PostMapping("/unauthorized")
        String unauthorized() {
            throw new UnauthorizedException("no permission to call");
        }

        @PostMapping("/forbidden")
        String forbidden() {
            throw new ForbiddenException("no permission to call");
        }

        @PostMapping("/illegal")
        String illegal() {
            throw new IllegalArgumentException("bad param");
        }

        @PostMapping("/boom")
        String boom() {
            throw new IllegalStateException("boom");
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /**
     * 复现线上那条栈：token 失效 + SSE Accept 头 → 必须拿到完整 JSON，而不是空 body。
     */
    @Test
    @DisplayName("UnauthorizedException + Accept:text/event-stream → JSON body 完整，不是空")
    void unauthorized_returnsJsonBody_whenAcceptIsEventStream() throws Exception {
        mockMvc.perform(post("/v1/unauthorized").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("no permission to call"));
    }

    @Test
    @DisplayName("ForbiddenException + Accept:text/event-stream → JSON body 完整")
    void forbidden_returnsJsonBody_whenAcceptIsEventStream() throws Exception {
        mockMvc.perform(post("/v1/forbidden").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("IllegalArgumentException + Accept:text/event-stream → JSON body 完整")
    void illegalArgument_returnsJsonBody_whenAcceptIsEventStream() throws Exception {
        mockMvc.perform(post("/v1/illegal").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("bad param"));
    }

    /**
     * 兜底的 RuntimeException 分支同样不能被 Accept 头搞成空 body。
     */
    @Test
    @DisplayName("未预期异常 + Accept:text/event-stream → JSON body 完整")
    void unexpectedException_returnsJsonBody_whenAcceptIsEventStream() throws Exception {
        mockMvc.perform(post("/v1/boom").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(500));
    }

    /**
     * 普通 JSON 客户端行为不变 —— 保证本次修复没有改动既有契约。
     */
    @Test
    @DisplayName("Accept:application/json 时行为不变（HTTP 200 + body.code）")
    void unauthorized_behaviourUnchanged_whenAcceptIsJson() throws Exception {
        mockMvc.perform(post("/v1/unauthorized").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(401));
    }
}
