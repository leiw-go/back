/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.common;

/**
 * Exception thrown when a user does not have permission to access a resource.
 */
public class ForbiddenException extends RuntimeException {

    /**
     * Constructs a ForbiddenException with the specified detail message.
     *
     * @param message the detail message
     */
    public ForbiddenException(String message) {
        super(message);
    }
}
