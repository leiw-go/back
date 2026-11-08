/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.common;

import com.yaowenltd.projectinfomationmanage.common.E164PhoneValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link E164PhoneValidator} 的纯 JUnit 5 单元测试。
 * <p>
 * 契约来自需求文档 §8.2：号码必须以 {@code +} 开头，后接国家码与号码，
 * <b>不含空格 / 横线 / 括号</b>；去掉 {@code +} 后为 2~15 位数字；国家码不能以 0 开头。
 * </p>
 * <p>
 * 不依赖 Spring 上下文、不连接任何外部基础设施。
 * </p>
 */
class E164PhoneValidatorTests {

    @ParameterizedTest(name = "[{index}] 合法 E.164 号码 {0}")
    @ValueSource(strings = {
            "+14155552671",      // 美国
            "+4915112345678",    // 德国
            "+8613800138000",    // 中国
            "+447911123456",     // 英国
            "+12",               // 下限：2 位数字
            "+123456789012345"   // 上限：15 位数字
    })
    @DisplayName("合法 E.164 号码应通过校验")
    void testValidNumbersAreAccepted(String phone) {
        assertTrue(E164PhoneValidator.isValid(phone),
                "should treat " + phone + " as a valid E.164 number");
        assertDoesNotThrow(() -> E164PhoneValidator.validate(phone),
                "validate() should not throw for " + phone);
    }

    @ParameterizedTest(name = "[{index}] 非法号码 {0}")
    @ValueSource(strings = {
            "+1",                  // 下限不足：只有 1 位数字
            "+1234567890123456",   // 上限超出：16 位数字
            "+0123456789",         // 国家码以 0 开头
            "+",                   // 只有加号
            "14155552671",         // 缺少加号
            "+1 415 555 2671",     // 含空格
            "+1-415-555-2671",     // 含横线
            "+1(415)5552671",      // 含括号
            "+1a4155552671",       // 含字母
            "+1.4155552671",       // 含小数点
            "+1+4155552671"        // 多个加号
    })
    @DisplayName("非法号码应被拒绝且抛出业务异常")
    void testInvalidNumbersAreRejected(String phone) {
        assertFalse(E164PhoneValidator.isValid(phone),
                "should treat " + phone + " as an invalid E.164 number");
        assertThrows(IllegalArgumentException.class, () -> E164PhoneValidator.validate(phone),
                "validate() should throw IllegalArgumentException for " + phone);
    }

    @Test
    @DisplayName("null 应被拒绝")
    void testNullIsRejected() {
        assertFalse(E164PhoneValidator.isValid(null));
        assertThrows(IllegalArgumentException.class, () -> E164PhoneValidator.validate(null));
    }

    @Test
    @DisplayName("空串应被拒绝")
    void testEmptyStringIsRejected() {
        assertFalse(E164PhoneValidator.isValid(""));
        assertThrows(IllegalArgumentException.class, () -> E164PhoneValidator.validate(""));
    }

    @Test
    @DisplayName("纯空白应被拒绝")
    void testBlankStringIsRejected() {
        assertFalse(E164PhoneValidator.isValid("   "));
        assertThrows(IllegalArgumentException.class, () -> E164PhoneValidator.validate("   "));
    }

    @Test
    @DisplayName("校验失败文案应提示期望格式")
    void testValidateErrorMessageMentionsExpectedFormat() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> E164PhoneValidator.validate("13800138000"));
        assertTrue(exception.getMessage().contains("E.164"),
                "error message should mention E.164 but was: " + exception.getMessage());
        assertTrue(exception.getMessage().contains("+14155552671"),
                "error message should carry an example but was: " + exception.getMessage());
    }

    @Test
    @DisplayName("正则常量应与文档约定一致")
    void testPatternMatchesDocumentedRule() {
        assertTrue("+14155552671".matches(E164PhoneValidator.PATTERN));
        assertFalse("+1".matches(E164PhoneValidator.PATTERN));
        assertFalse("+1234567890123456".matches(E164PhoneValidator.PATTERN));
        assertFalse("+0123".matches(E164PhoneValidator.PATTERN));
        assertFalse("+1 415".matches(E164PhoneValidator.PATTERN));
        assertEquals(15, E164PhoneValidator.MAX_DIGITS, "E.164 号码最多 15 位数字");
    }
}
