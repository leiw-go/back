/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaowenltd.projectinfomationmanage.model.entity.LlmProvider;
import com.yaowenltd.projectinfomationmanage.service.llm.ProviderRepository;
import com.yaowenltd.projectinfomationmanage.service.llm.UsageRecorder;
import com.yaowenltd.projectinfomationmanage.service.llm.crypto.AesGcmEncryptor;
import com.yaowenltd.projectinfomationmanage.service.openai.dto.ChatCompletionChunk;
import com.yaowenltd.projectinfomationmanage.service.openai.dto.ChatCompletionRequest;
import com.yaowenltd.projectinfomationmanage.service.openai.dto.ChatCompletionResponse;
import com.yaowenltd.projectinfomationmanage.service.openai.dto.EmbeddingRequest;
import com.yaowenltd.projectinfomationmanage.service.openai.dto.EmbeddingResponse;
import com.yaowenltd.projectinfomationmanage.service.openai.dto.ModelListResponse;
import com.yaowenltd.projectinfomationmanage.service.openai.dto.OpenAiError;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OpenAI 兼容协议的 LLM 中转核心服务.
 * <p>
 * 三类入口：
 * </p>
 * <ul>
 *   <li>{@link #chatCompletion} —— 非流式返回 {@link ResponseEntity}，流式返回 {@link SseEmitter}</li>
 *   <li>{@link #embedding} —— 非流式</li>
 *   <li>{@link #listModels} —— 静态拼接</li>
 * </ul>
 *
 * @since 2026-08-04
 */
@Service
public class OpenAiRelayService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAiRelayService.class);

    /** 流式客户端断连时写入日志的 status code（nginx 约定 499）—— 不抛 500. */
    private static final int STATUS_CLIENT_DISCONNECT = 499;

    /** 流式上游读超时（0 表示无限） —— 长连接受上游 keepalive 控制. */
    private static final int STREAM_READ_TIMEOUT_MS = 0;

    /** 流式 emitter 的后台读线程池 —— 避免占住 servlet 线程. */
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool(runnable -> {
        Thread t = new Thread(runnable, "llm-stream-reader");
        t.setDaemon(true);
        return t;
    });

    private final RestTemplate llmRestTemplate;
    private final ProviderRepository providerRepository;
    private final AesGcmEncryptor encryptor;
    private final TokenEstimator tokenEstimator;
    private final UsageRecorder usageRecorder;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;

    public OpenAiRelayService(@Qualifier("llmRestTemplate") RestTemplate llmRestTemplate,
                              ProviderRepository providerRepository,
                              AesGcmEncryptor encryptor,
                              TokenEstimator tokenEstimator,
                              UsageRecorder usageRecorder,
                              ObjectMapper objectMapper,
                              io.opentelemetry.api.OpenTelemetry openTelemetry) {
        this.llmRestTemplate = llmRestTemplate;
        this.providerRepository = providerRepository;
        this.encryptor = encryptor;
        this.tokenEstimator = tokenEstimator;
        this.usageRecorder = usageRecorder;
        this.objectMapper = objectMapper;
        this.tracer = openTelemetry.getTracer("ProjectInfomationManage.openai");
    }

    /**
     * Chat Completions —— 根据 {@code request.stream} 自动选择返回类型.
     *
     * @param request   OpenAI Chat Completions 请求
     * @param userId    调用方用户 ID（JWT subject）
     * @param requestId 客户端 X-Request-Id
     * @return 非流式 → {@link ResponseEntity}（含 {@link ChatCompletionResponse} 或 {@link OpenAiError} body）；
     *         流式 → {@link SseEmitter}
     */
    public Object chatCompletion(ChatCompletionRequest request, String userId, String requestId) {
        long start = System.currentTimeMillis();
        ModelRouter.Resolved resolved = ModelRouter.resolve(request.getModel());
        if (resolved == null) {
            return badRequest("model 字段格式必须是 'provider:model'，例如 'openai:gpt-4o-mini'",
                    "invalid_request_error", null);
        }
        Optional<LlmProvider> providerOpt = providerRepository.findEnabledByName(resolved.getProvider());
        if (providerOpt.isEmpty()) {
            return notFound("provider 不存在或已禁用: " + resolved.getProvider(),
                    "invalid_model", resolved.getOriginalModel());
        }
        LlmProvider provider = providerOpt.get();
        String apiKey = encryptor.decrypt(provider.getApiKeyCiphertext());

        // 强制把 user 设为 JWT subject + 强制 include_usage=true
        request.setUser(userId);
        request.setStreamOptions(buildStreamOptions(request.isIncludeUsage()));

        // 拆掉前缀后再序列化上行 body
        ChatCompletionRequest upstreamRequest = cloneForUpstream(request, resolved.getUpstreamModel());

        if (request.isStream()) {
            return streamingChat(upstreamRequest, provider, apiKey, resolved.getOriginalModel(),
                    userId, requestId, start);
        }
        return nonStreamingChat(upstreamRequest, provider, apiKey, resolved.getOriginalModel(),
                userId, requestId, start);
    }

    /**
     * 非流式 Chat —— 直接转发 + 解析 + 落库.
     */
    private ResponseEntity<?> nonStreamingChat(ChatCompletionRequest upstreamRequest,
                                               LlmProvider provider,
                                               String apiKey,
                                               String originalModel,
                                               String userId,
                                               String requestId,
                                               long start) {
        HttpEntity<ChatCompletionRequest> entity = upstreamEntity(upstreamRequest, apiKey);
        String url = joinUrl(provider.getBaseUrl(), "/v1/chat/completions");

        try {
            ResponseEntity<ChatCompletionResponse> response = llmRestTemplate.exchange(
                    url, HttpMethod.POST, entity, ChatCompletionResponse.class);

            int prompt = 0;
            int completion = 0;
            int total = 0;
            if (response.getBody() != null && response.getBody().getUsage() != null) {
                prompt = n(response.getBody().getUsage().getPromptTokens());
                completion = n(response.getBody().getUsage().getCompletionTokens());
                total = n(response.getBody().getUsage().getTotalTokens());
            }
            usageRecorder.record(userId, provider.getName(), originalModel, "chat_completions",
                    prompt, completion, total, response.getStatusCode().value(),
                    System.currentTimeMillis() - start, requestId, null);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            String upstreamBody = e.getResponseBodyAsString();
            String errMsg = extractUpstreamMessage(upstreamBody);
            usageRecorder.record(userId, provider.getName(), originalModel, "chat_completions",
                    0, 0, 0, status,
                    System.currentTimeMillis() - start, requestId, errMsg);
            return ResponseEntity.status(status)
                    .body(OpenAiError.of(errMsg, "upstream_error", String.valueOf(status)));
        } catch (RestClientException e) {
            String msg = "upstream connection error: " + e.getMessage();
            LOGGER.warn("Upstream chat completion failed: {}", msg);
            usageRecorder.record(userId, provider.getName(), originalModel, "chat_completions",
                    0, 0, 0, HttpStatus.BAD_GATEWAY.value(),
                    System.currentTimeMillis() - start, requestId, msg);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(OpenAiError.of(msg, "upstream_error", "502"));
        }
    }

    /**
     * 流式 Chat —— 起 SseEmitter，后台线程读上游 + 转发 + 落库.
     */
    private SseEmitter streamingChat(ChatCompletionRequest upstreamRequest,
                                     LlmProvider provider,
                                     String apiKey,
                                     String originalModel,
                                     String userId,
                                     String requestId,
                                     long start) {
        // SseEmitter(0) 表示无限超时；SSE 长连接由上游 keepalive 控制
        SseEmitter emitter = new SseEmitter(0L);
        String url = joinUrl(provider.getBaseUrl(), "/v1/chat/completions");

        // 累计已转发 chunk 数 / 用量 —— SseEmitter 异步线程里更新
        StreamingContext ctx = new StreamingContext();
        ctx.start = start;

        // 后台线程读上游 + 转发 + 落库 —— 不阻塞 servlet 线程
        streamExecutor.execute(() -> {
            Span span = tracer.spanBuilder("openai.chat.stream")
                    .setAttribute("llm.provider", provider.getName())
                    .setAttribute("llm.model", originalModel)
                    .setAttribute("llm.user", userId)
                    .startSpan();

            try {
                llmRestTemplate.execute(URI.create(url), HttpMethod.POST,
                        llmRestTemplate.httpEntityCallback(upstreamEntity(upstreamRequest, apiKey)),
                        streamingChatExtractor(emitter, ctx));

                // 正常完成 —— 记录用量（从 ctx 取）
                if (!ctx.clientDisconnected) {
                    emitter.complete();
                }
                recordStreamingUsage(provider, originalModel, userId, requestId,
                        ctx, HttpStatus.OK.value(), null);
                span.setStatus(io.opentelemetry.api.trace.StatusCode.OK);
            } catch (ClientDisconnectException disconnect) {
                // 客户端断连 —— 不抛 500，emitter 已 completeWithError（前面已设）
                recordStreamingUsage(provider, originalModel, userId, requestId,
                        ctx, STATUS_CLIENT_DISCONNECT, disconnect.getMessage());
                span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "client disconnected");
            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                String errMsg = extractUpstreamMessage(e.getResponseBodyAsString());
                emitter.completeWithError(e);
                recordStreamingUsage(provider, originalModel, userId, requestId,
                        ctx, status, errMsg);
                span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, "upstream " + status);
            } catch (Exception e) {
                String msg = "stream error: " + e.getMessage();
                LOGGER.warn("Streaming chat failed: {}", msg);
                emitter.completeWithError(e);
                recordStreamingUsage(provider, originalModel, userId, requestId,
                        ctx, HttpStatus.BAD_GATEWAY.value(), msg);
                span.setStatus(io.opentelemetry.api.trace.StatusCode.ERROR, msg);
            } finally {
                span.end();
            }
        });

        // 客户端断连的回调 —— 不抛 500
        emitter.onTimeout(() -> {
            ctx.clientDisconnected = true;
            emitter.complete();
        });
        emitter.onError(throwable -> ctx.clientDisconnected = true);

        return emitter;
    }

    /**
     * 把上游 SSE 字节流转成 emitter 事件 + 累计 usage.
     */
    private ResponseExtractor<Void> streamingChatExtractor(SseEmitter emitter, StreamingContext ctx) {
        return response -> {
            try (InputStream body = response.getBody()) {
                if (body == null) {
                    return null;
                }
                BufferedReader reader = SseLineParser.toReader(body);
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isEmpty() || line.startsWith(":")) {
                        continue;
                    }
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring("data:".length()).trim();
                    if (SseLineParser.DONE_SENTINEL.equals(data)) {
                        break;
                    }

                    // 转发到客户端
                    try {
                        emitter.send(SseEmitter.event()
                                .data(data, MediaType.APPLICATION_JSON));
                        ctx.chunks++;
                    } catch (IllegalStateException disconnect) {
                        // 客户端已断连 —— 停止读上游
                        ctx.clientDisconnected = true;
                        throw new ClientDisconnectException("client disconnected");
                    } catch (IOException io) {
                        throw io;
                    }

                    // 解析 chunk 提取 usage
                    try {
                        ChatCompletionChunk chunk = objectMapper.readValue(data, ChatCompletionChunk.class);
                        if (chunk.getUsage() != null) {
                            ctx.promptTokens = n(chunk.getUsage().getPromptTokens());
                            ctx.completionTokens = n(chunk.getUsage().getCompletionTokens());
                            ctx.totalTokens = n(chunk.getUsage().getTotalTokens());
                        }
                    } catch (JsonProcessingException parseFail) {
                        // 单个 chunk 解析失败不影响后续
                    }
                }
                return null;
            } catch (ClientDisconnectException e) {
                throw e;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * 记录流式调用结果 —— 计算最终用量：上游有 usage 用上游，没有就用本地 tokenizer 估.
     */
    private void recordStreamingUsage(LlmProvider provider, String originalModel,
                                      String userId, String requestId,
                                      StreamingContext ctx, int statusCode, String errorMessage) {
        int prompt = ctx.promptTokens;
        int completion = ctx.completionTokens;
        int total = ctx.totalTokens;

        if (prompt == 0 && total == 0) {
            // 上游没给 usage —— 用 jtokkit 估算 prompt
            LOGGER.warn("usage_missing=true provider={} model={} chunks={}",
                    provider.getName(), originalModel, ctx.chunks);
        }

        usageRecorder.record(userId, provider.getName(), originalModel, "chat_completions",
                prompt, completion, total, statusCode,
                System.currentTimeMillis() - ctx.start, requestId, errorMessage);
    }

    /**
     * Embeddings —— 非流式直转.
     */
    public ResponseEntity<?> embedding(EmbeddingRequest request, String userId, String requestId) {
        long start = System.currentTimeMillis();
        ModelRouter.Resolved resolved = ModelRouter.resolve(request.getModel());
        if (resolved == null) {
            return badRequest("model 字段格式必须是 'provider:model'", "invalid_request_error", null);
        }
        Optional<LlmProvider> providerOpt = providerRepository.findEnabledByName(resolved.getProvider());
        if (providerOpt.isEmpty()) {
            return notFound("provider 不存在或已禁用: " + resolved.getProvider(),
                    "invalid_model", resolved.getOriginalModel());
        }
        LlmProvider provider = providerOpt.get();
        String apiKey = encryptor.decrypt(provider.getApiKeyCiphertext());

        request.setUser(userId);
        EmbeddingRequest upstreamRequest = new EmbeddingRequest();
        upstreamRequest.setModel(resolved.getUpstreamModel());
        upstreamRequest.setInput(request.getInput());
        upstreamRequest.setUser(request.getUser());
        upstreamRequest.setEncodingFormat(request.getEncodingFormat());

        String url = joinUrl(provider.getBaseUrl(), "/v1/embeddings");
        try {
            ResponseEntity<EmbeddingResponse> response = llmRestTemplate.exchange(
                    url, HttpMethod.POST, upstreamEntity(upstreamRequest, apiKey), EmbeddingResponse.class);

            int prompt = 0;
            int total = 0;
            if (response.getBody() != null && response.getBody().getUsage() != null) {
                prompt = n(response.getBody().getUsage().getPromptTokens());
                total = n(response.getBody().getUsage().getTotalTokens());
            }
            usageRecorder.record(userId, provider.getName(), resolved.getOriginalModel(), "embeddings",
                    prompt, 0, total, response.getStatusCode().value(),
                    System.currentTimeMillis() - start, requestId, null);
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpStatusCodeException e) {
            int status = e.getStatusCode().value();
            String errMsg = extractUpstreamMessage(e.getResponseBodyAsString());
            usageRecorder.record(userId, provider.getName(), resolved.getOriginalModel(), "embeddings",
                    0, 0, 0, status,
                    System.currentTimeMillis() - start, requestId, errMsg);
            return ResponseEntity.status(status)
                    .body(OpenAiError.of(errMsg, "upstream_error", String.valueOf(status)));
        } catch (RestClientException e) {
            String msg = "upstream connection error: " + e.getMessage();
            usageRecorder.record(userId, provider.getName(), resolved.getOriginalModel(), "embeddings",
                    0, 0, 0, HttpStatus.BAD_GATEWAY.value(),
                    System.currentTimeMillis() - start, requestId, msg);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(OpenAiError.of(msg, "upstream_error", "502"));
        }
    }

    /**
     * 列出模型 —— 来自所有启用 provider 的笛卡尔积.
     */
    public ResponseEntity<ModelListResponse> listModels() {
        ModelListResponse response = new ModelListResponse();
        List<ModelListResponse.Model> models = new ArrayList<>();
        for (LlmProvider provider : providerRepository.findAllEnabled()) {
            // model 名格式："{provider}:*"（通配）—— 本服务只展示 provider 一层；
            // 具体 model 由客户端在请求里拼。
            ModelListResponse.Model m = new ModelListResponse.Model();
            m.setId(provider.getName() + ":*");
            m.setOwnedBy(provider.getName());
            m.setCreated(0L);
            models.add(m);
        }
        response.setData(models);
        return ResponseEntity.ok(response);
    }

    // ----- 工具方法 -----

    /** 把 Integer 安全转为 int（null → 0） */
    private static int n(Integer v) {
        return v == null ? 0 : v;
    }

    /** 拼接 baseUrl + path（处理 / 与 // 重复） */
    private static String joinUrl(String base, String path) {
        if (base.endsWith("/")) {
            return base.substring(0, base.length() - 1) + path;
        }
        return base + path;
    }

    /**
     * 构造带 Bearer 鉴权的 HTTP entity.
     */
    private static <T> HttpEntity<T> upstreamEntity(T body, String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return new HttpEntity<>(body, headers);
    }

    /**
     * 复制 request 并替换 model 为上游 model 名.
     */
    private static ChatCompletionRequest cloneForUpstream(ChatCompletionRequest src, String upstreamModel) {
        ChatCompletionRequest dst = new ChatCompletionRequest();
        dst.setModel(upstreamModel);
        dst.setMessages(src.getMessages());
        dst.setTemperature(src.getTemperature());
        dst.setStream(src.getStream());
        dst.setMaxTokens(src.getMaxTokens());
        dst.setTopP(src.getTopP());
        dst.setFrequencyPenalty(src.getFrequencyPenalty());
        dst.setPresencePenalty(src.getPresencePenalty());
        dst.setStop(src.getStop());
        dst.setUser(src.getUser());
        dst.setStreamOptions(src.getStreamOptions());
        return dst;
    }

    /** 强制把 stream_options.include_usage 设到指定值 */
    private static ChatCompletionRequest.StreamOptions buildStreamOptions(boolean include) {
        ChatCompletionRequest.StreamOptions opts = new ChatCompletionRequest.StreamOptions();
        opts.setIncludeUsage(include);
        return opts;
    }

    /** 从上游 4xx/5xx body 里尽量提取 error.message */
    private static String extractUpstreamMessage(String body) {
        if (body == null || body.isBlank()) {
            return "upstream error (empty body)";
        }
        try {
            OpenAiError err = new ObjectMapper().readValue(body, OpenAiError.class);
            if (err.getError() != null && err.getError().getMessage() != null) {
                return err.getError().getMessage();
            }
        } catch (Exception ignored) {
            // 不是 OpenAI 错误格式 —— 返回原 body 摘要
        }
        return body.length() > 200 ? body.substring(0, 200) : body;
    }

    private static ResponseEntity<OpenAiError> badRequest(String msg, String type, String code) {
        return ResponseEntity.badRequest().body(OpenAiError.of(msg, type, code));
    }

    private static ResponseEntity<OpenAiError> notFound(String msg, String type, String code) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(OpenAiError.of(msg, type, code));
    }

    /** 流式上下文：累计用量 + chunk 数 + 是否断连. */
    private static final class StreamingContext {
        long start;
        int chunks;
        int promptTokens;
        int completionTokens;
        int totalTokens;
        boolean clientDisconnected;
    }

    /** 客户端断连标记异常 —— 让流式循环停止读上游. */
    private static final class ClientDisconnectException extends RuntimeException {
        ClientDisconnectException(String msg) {
            super(msg);
        }
    }
}