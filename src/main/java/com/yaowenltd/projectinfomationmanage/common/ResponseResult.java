/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.common;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;

/**
 * Wrapper class for HTTP response results.
 *
 * @param <T> the type of data in the response
 */
public class ResponseResult<T> {

    private int code;

    private String message;

    private T data;

    private List<String> errors;

    /**
     * Default constructor.
     */
    public ResponseResult() {
    }

    /**
     * Constructs a response result with code, message and data.
     *
     * @param code    the HTTP status code
     * @param message the response message
     * @param data    the response data
     */
    public ResponseResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * Creates a success response with data.
     *
     * @param data the response data
     * @param <T>  the type of data
     * @return the response result
     */
    public static <T> ResponseResult<T> success(T data) {
        return new ResponseResult<>(HttpStatus.OK.value(), "success", data);
    }

    /**
     * Creates a success response without data.
     *
     * @param <T> the type of data
     * @return the response result
     */
    public static <T> ResponseResult<T> success() {
        return new ResponseResult<>(HttpStatus.OK.value(), "success", null);
    }

    /**
     * Creates a created response with data.
     *
     * @param data the response data
     * @param <T>  the type of data
     * @return the response result
     */
    public static <T> ResponseResult<T> created(T data) {
        return new ResponseResult<>(HttpStatus.CREATED.value(), "created", data);
    }

    /**
     * Creates a bad request error response.
     *
     * @param message the error message
     * @param <T>     the type of data
     * @return the response result
     */
    public static <T> ResponseResult<T> badRequest(String message) {
        ResponseResult<T> result = new ResponseResult<>(HttpStatus.BAD_REQUEST.value(), message, null);
        result.setErrors(Collections.singletonList(message));
        return result;
    }

    /**
     * Creates an unauthorized error response.
     *
     * @param message the error message
     * @param <T>     the type of data
     * @return the response result
     */
    public static <T> ResponseResult<T> unauthorized(String message) {
        ResponseResult<T> result = new ResponseResult<>(HttpStatus.UNAUTHORIZED.value(), message, null);
        result.setErrors(Collections.singletonList(message));
        return result;
    }

    /**
     * Creates a forbidden error response.
     *
     * @param message the error message
     * @param <T>     the type of data
     * @return the response result
     */
    public static <T> ResponseResult<T> forbidden(String message) {
        ResponseResult<T> result = new ResponseResult<>(HttpStatus.FORBIDDEN.value(), message, null);
        result.setErrors(Collections.singletonList(message));
        return result;
    }

    /**
     * Creates a not found error response.
     *
     * @param message the error message
     * @param <T>     the type of data
     * @return the response result
     */
    public static <T> ResponseResult<T> notFound(String message) {
        ResponseResult<T> result = new ResponseResult<>(HttpStatus.NOT_FOUND.value(), message, null);
        result.setErrors(Collections.singletonList(message));
        return result;
    }

    /**
     * Creates an internal server error response.
     *
     * @param message the error message
     * @param <T>     the type of data
     * @return the response result
     */
    public static <T> ResponseResult<T> error(String message) {
        ResponseResult<T> result = new ResponseResult<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), message, null);
        result.setErrors(Collections.singletonList(message));
        return result;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
