/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.common;

/**
 * 用户无权限访问资源时抛出的异常。
 */
public class ForbiddenException extends RuntimeException {

    /**
     * 用指定的详情消息构造 ForbiddenException。
     *
     * @param message 详情消息
     */
    public ForbiddenException(String message) {
        super(message);
    }
}
