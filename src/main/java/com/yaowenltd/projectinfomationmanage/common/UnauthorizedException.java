/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.common;

/**
 * Exception thrown when a user is not authenticated.
 */
public class UnauthorizedException extends RuntimeException {

    /**
     * Constructs an UnauthorizedException with the specified detail message.
     *
     * @param message the detail message
     */
    public UnauthorizedException(String message) {
        super(message);
    }
}
