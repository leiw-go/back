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
 * Global exception handler for intercepting and handling exceptions
 * thrown during controller method execution.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles MethodArgumentNotValidException (validation errors).
     *
     * @param exception the validation exception
     * @return the response result with validation error messages
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
     * Handles IllegalArgumentException.
     *
     * @param exception the illegal argument exception
     * @return the response result with error message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseResult<Void> handleIllegalArgumentException(IllegalArgumentException exception) {
        LOGGER.warn("Illegal argument: {}", exception.getMessage());
        return ResponseResult.badRequest(exception.getMessage());
    }

    /**
     * Handles UnauthorizedException.
     *
     * @param exception the unauthorized exception
     * @return the response result with unauthorized message
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseResult<Void> handleUnauthorizedException(UnauthorizedException exception) {
        LOGGER.warn("Unauthorized access: {}", exception.getMessage());
        ResponseResult<Void> result = ResponseResult.unauthorized(exception.getMessage());
        return result;
    }

    /**
     * Handles ForbiddenException.
     *
     * @param exception the forbidden exception
     * @return the response result with forbidden message
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseResult<Void> handleForbiddenException(ForbiddenException exception) {
        LOGGER.warn("Forbidden access: {}", exception.getMessage());
        return ResponseResult.forbidden(exception.getMessage());
    }

    /**
     * Handles all unhandled RuntimeException.
     *
     * @param exception the runtime exception
     * @return the response result with error message
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseResult<Void> handleRuntimeException(RuntimeException exception) {
        LOGGER.error("Unexpected runtime error: {}", exception.getMessage(), exception);
        return ResponseResult.error("internal server error");
    }

    /**
     * Handles all remaining exceptions.
     *
     * @param exception the exception
     * @return the response result with error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseResult<Void> handleException(Exception exception) {
        LOGGER.error("Unexpected error: {}", exception.getMessage(), exception);
        return ResponseResult.error("internal server error");
    }
}
