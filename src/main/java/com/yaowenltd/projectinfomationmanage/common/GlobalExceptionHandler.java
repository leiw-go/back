/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局异常处理器，捕获并处理 Controller 方法抛出的各类异常。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 把错误体包成 {@link ResponseEntity} 并<strong>显式钉死</strong>
     * {@code Content-Type: application/json}。
     * <p>
     * <strong>为什么必须显式设置：</strong>直接 {@code return} 一个 POJO 时，Spring 会拿请求的
     * {@code Accept} 头做内容协商。OpenAI 兼容客户端发流式请求时带
     * {@code Accept: text/event-stream}，而 Jackson converter 只能产出
     * {@code application/json}，交集为空 → 抛 {@code HttpMediaTypeNotAcceptableException}
     * → <strong>本处理器自己的返回值也写不出去</strong>（日志里表现为
     * "Failure in @ExceptionHandler"）→ 异常继续上抛到 servlet，客户端只拿到一个空 body，
     * 完全看不到 401 / 400 的真实原因。
     * </p>
     * <p>
     * Content-Type 一旦预设为具体类型，{@code AbstractMessageConverterMethodProcessor}
     * 就会跳过 Accept 协商直接用它。
     * </p>
     * <p>
     * <strong>HTTP 状态码恒为 200</strong> —— 沿用本仓库既有约定：语义状态码放在响应体的
     * {@code code} 字段里（见 {@code AuthControllerUnitTests} 的契约说明）。
     * </p>
     *
     * @param body 错误响应体
     * @return 预设 {@code application/json} 的 ResponseEntity
     */
    private static ResponseEntity<ResponseResult<Void>> json(ResponseResult<Void> body) {
        return ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    /**
     * 处理方法参数校验异常（{@code @Valid} 校验失败）。
     *
     * @param exception 校验异常
     * @return 含校验错误信息的响应体
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseResult<Void>> handleValidationException(
            MethodArgumentNotValidException exception) {
        List<String> errors = new ArrayList<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.add(fieldError.getDefaultMessage());
        }
        ResponseResult<Void> result = new ResponseResult<>(
                HttpStatus.BAD_REQUEST.value(), "validation error", null);
        result.setErrors(errors);
        return json(result);
    }

    /**
     * 处理非法参数异常。
     *
     * @param exception 非法参数异常
     * @return 含错误信息的响应体
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseResult<Void>> handleIllegalArgumentException(
            IllegalArgumentException exception) {
        LOGGER.warn("Illegal argument: {}", exception.getMessage());
        return json(ResponseResult.badRequest(exception.getMessage()));
    }

    /**
     * 处理未认证异常。
     *
     * @param exception 未认证异常
     * @return 含未认证提示的响应体
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ResponseResult<Void>> handleUnauthorizedException(
            UnauthorizedException exception) {
        LOGGER.warn("Unauthorized access: {}", exception.getMessage());
        return json(ResponseResult.unauthorized(exception.getMessage()));
    }

    /**
     * 处理无权限异常。
     *
     * @param exception 无权限异常
     * @return 含禁止访问提示的响应体
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ResponseResult<Void>> handleForbiddenException(ForbiddenException exception) {
        LOGGER.warn("Forbidden access: {}", exception.getMessage());
        return json(ResponseResult.forbidden(exception.getMessage()));
    }

    /**
     * 处理内容协商失败（{@code Accept} 头与服务端能产出的类型不匹配）。
     * <p>
     * 典型场景：OpenAI 兼容客户端发流式请求时带 {@code Accept: text/event-stream}，
     * 但服务端因为参数校验失败等原因要返回 JSON 错误体。此时若走默认处理，Spring 找不到
     * 能产出 {@code text/event-stream} 的 converter，本处理器自身的返回值也写不出去
     * （日志里表现为 "Failure in @ExceptionHandler"），客户端最终只能拿到一个 406 空 body，
     * 真实错误原因完全丢失。
     * </p>
     * <p>
     * 因此这里必须返回 {@link ResponseEntity} 并<strong>显式钉死</strong>
     * {@code Content-Type: application/json} —— 预设具体 Content-Type 会让 Spring
     * 跳过 Accept 协商，保证错误体一定能写出去。
     * </p>
     *
     * @param exception 内容协商异常
     * @return 406 + JSON 错误体
     */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ResponseResult<Void>> handleMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException exception) {
        LOGGER.warn("Content negotiation failed, supported={}: {}",
                exception.getSupportedMediaTypes(), exception.getMessage());
        return json(new ResponseResult<>(
                HttpStatus.NOT_ACCEPTABLE.value(),
                "no acceptable representation; this endpoint returns application/json",
                null));
    }

    /**
     * 处理所有未匹配到的 {@link RuntimeException}。
     *
     * @param exception 运行时异常
     * @return 含错误信息的响应体
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ResponseResult<Void>> handleRuntimeException(RuntimeException exception) {
        LOGGER.error("Unexpected runtime error: {}", exception.getMessage(), exception);
        return json(ResponseResult.error("internal server error"));
    }

    /**
     * 处理所有剩余的非运行时异常。
     *
     * @param exception 异常
     * @return 含错误信息的响应体
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseResult<Void>> handleException(Exception exception) {
        LOGGER.error("Unexpected error: {}", exception.getMessage(), exception);
        return json(ResponseResult.error("internal server error"));
    }
}
