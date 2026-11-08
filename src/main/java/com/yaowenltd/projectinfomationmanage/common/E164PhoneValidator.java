/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.common;

/**
 * E.164 电话号码格式校验器.<p>按需求文档 §8.2：号码必须以加号开头，后接国家码与号码，不含空格、横线与括号。
 *
 * @since 2026-11-08
 */
public final class E164PhoneValidator {

    /**
     * E.164 格式正则：加号 + 非 0 开头国家码 + 1~14 位数字（合计 2~15 位）.
     */
    public static final String PATTERN = "^\\+[1-9]\\d{1,14}$";

    /**
     * E.164 号码允许的最大数字位数（不含加号）.
     */
    public static final int MAX_DIGITS = 15;

    private E164PhoneValidator() {
    }

    /**
     * 判断给定号码是否为合法的 E.164 格式.
     *
     * @param phone 待校验的号码
     * @return 合法返回 true
     */
    public static boolean isValid(String phone) {
        throw new UnsupportedOperationException("TDD: 待实现");
    }

    /**
     * 校验给定号码是否为合法的 E.164 格式，非法时抛出业务异常.
     *
     * @param phone 待校验的号码
     */
    public static void validate(String phone) {
        throw new UnsupportedOperationException("TDD: 待实现");
    }

}
