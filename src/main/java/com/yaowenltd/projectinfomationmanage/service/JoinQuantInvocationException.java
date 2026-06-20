/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service;

/**
 * Thrown when calling the JoinQuant Python CLI fails.
 */
public class JoinQuantInvocationException extends RuntimeException {

    private final String code;

    private final String stderr;

    public JoinQuantInvocationException(String message, String code) {
        this(message, code, "");
    }

    public JoinQuantInvocationException(String message, String code, String stderr) {
        this(message, code, stderr, null);
    }

    public JoinQuantInvocationException(String message, String code, String stderr, Throwable cause) {
        super(message, cause);
        this.code = code == null ? "JQ_ERROR" : code;
        this.stderr = stderr == null ? "" : stderr;
    }

    public String getCode() {
        return code;
    }

    public String getStderr() {
        return stderr;
    }
}
