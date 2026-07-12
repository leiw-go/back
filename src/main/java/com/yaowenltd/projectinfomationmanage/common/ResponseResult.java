/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.common;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.List;

/**
 * 统一 HTTP 响应结果的包装类。
 *
 * @param <T> 响应数据类型
 */
public class ResponseResult<T> {

    private int code;

    private String message;

    private T data;

    private List<String> errors;

    /**
     * 默认构造器（用于 JSON 反序列化等场景）。
     */
    public ResponseResult() {
    }

    /**
     * 用状态码、消息、数据构造一个响应结果。
     *
     * @param code    HTTP 状态码
     * @param message 响应消息
     * @param data    响应数据
     */
    public ResponseResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 创建一个带数据的成功响应。
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 响应结果
     */
    public static <T> ResponseResult<T> success(T data) {
        return new ResponseResult<>(HttpStatus.OK.value(), "success", data);
    }

    /**
     * 创建一个不带数据的成功响应。
     *
     * @param <T> 数据类型
     * @return 响应结果
     */
    public static <T> ResponseResult<T> success() {
        return new ResponseResult<>(HttpStatus.OK.value(), "success", null);
    }

    /**
     * 创建一个带数据的已创建（201）响应。
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 响应结果
     */
    public static <T> ResponseResult<T> created(T data) {
        return new ResponseResult<>(HttpStatus.CREATED.value(), "created", data);
    }

    /**
     * 创建一个错误请求（400）响应。
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> ResponseResult<T> badRequest(String message) {
        ResponseResult<T> result = new ResponseResult<>(HttpStatus.BAD_REQUEST.value(), message, null);
        result.setErrors(Collections.singletonList(message));
        return result;
    }

    /**
     * 创建一个未认证（401）响应。
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> ResponseResult<T> unauthorized(String message) {
        ResponseResult<T> result = new ResponseResult<>(HttpStatus.UNAUTHORIZED.value(), message, null);
        result.setErrors(Collections.singletonList(message));
        return result;
    }

    /**
     * 创建一个禁止访问（403）响应。
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> ResponseResult<T> forbidden(String message) {
        ResponseResult<T> result = new ResponseResult<>(HttpStatus.FORBIDDEN.value(), message, null);
        result.setErrors(Collections.singletonList(message));
        return result;
    }

    /**
     * 创建一个资源未找到（404）响应。
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 响应结果
     */
    public static <T> ResponseResult<T> notFound(String message) {
        ResponseResult<T> result = new ResponseResult<>(HttpStatus.NOT_FOUND.value(), message, null);
        result.setErrors(Collections.singletonList(message));
        return result;
    }

    /**
     * 创建一个服务器内部错误（500）响应。
     *
     * @param message 错误消息
     * @param <T>     数据类型
     * @return 响应结果
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
