/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.common;

/**
 * 用户未通过身份认证时抛出的异常。
 */
public class UnauthorizedException extends RuntimeException {

    /**
     * 用指定的详情消息构造 UnauthorizedException。
     *
     * @param message 详情消息
     */
    public UnauthorizedException(String message) {
        super(message);
    }
}
