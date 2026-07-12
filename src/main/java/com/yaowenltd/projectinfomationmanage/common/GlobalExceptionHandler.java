/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
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
     * 处理方法参数校验异常（{@code @Valid} 校验失败）。
     *
     * @param exception 校验异常
     * @return 含校验错误信息的响应体
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseResult<Void> handleValidationException(MethodArgumentNotValidException exception) {
        List<String> errors = new ArrayList<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.add(fieldError.getDefaultMessage());
        }
        ResponseResult<Void> result = new ResponseResult<>(
                HttpStatus.BAD_REQUEST.value(), "validation error", null);
        result.setErrors(errors);
        return result;
    }

    /**
     * 处理非法参数异常。
     *
     * @param exception 非法参数异常
     * @return 含错误信息的响应体
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseResult<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        LOGGER.warn("Illegal argument: {}", exception.getMessage());
        return ResponseResult.badRequest(exception.getMessage());
    }

    /**
     * 处理未认证异常。
     *
     * @param exception 未认证异常
     * @return 含未认证提示的响应体
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseResult<Void> handleUnauthorizedException(UnauthorizedException exception) {
        LOGGER.warn("Unauthorized access: {}", exception.getMessage());
        ResponseResult<Void> result = ResponseResult.unauthorized(exception.getMessage());
        return result;
    }

    /**
     * 处理无权限异常。
     *
     * @param exception 无权限异常
     * @return 含禁止访问提示的响应体
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseResult<Void> handleForbiddenException(ForbiddenException exception) {
        LOGGER.warn("Forbidden access: {}", exception.getMessage());
        return ResponseResult.forbidden(exception.getMessage());
    }

    /**
     * 处理所有未匹配到的 {@link RuntimeException}。
     *
     * @param exception 运行时异常
     * @return 含错误信息的响应体
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseResult<Void> handleRuntimeException(RuntimeException exception) {
        LOGGER.error("Unexpected runtime error: {}", exception.getMessage(), exception);
        return ResponseResult.error("internal server error");
    }

    /**
     * 处理所有剩余的非运行时异常。
     *
     * @param exception 异常
     * @return 含错误信息的响应体
     */
    @ExceptionHandler(Exception.class)
    public ResponseResult<Void> handleException(Exception exception) {
        LOGGER.error("Unexpected error: {}", exception.getMessage(), exception);
        return ResponseResult.error("internal server error");
    }
}
