/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProjectInformationManageApplication 的单元测试类.
 * 使用 test profile（application-test.yml），不依赖外部 config-server.
 */
@SpringBootTest
@ActiveProfiles("test")
class ProjectInformationManageApplicationTests {

    /**
     * Spring 上下文能正常加载的冒烟测试.
     */
    @Test
    void contextLoads() {
    }

    /**
     * 验证 BCryptPasswordEncoder.matches 在密码正确时返回 true.
     */
    @Test
    void testPasswordEncoderMatchesWithCorrectPassword() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String rawPassword = "admin123";
        String userPassword = "user123";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        String encodedUserPassword = passwordEncoder.encode(userPassword);

        boolean result = passwordEncoder.matches(rawPassword, encodedPassword);

//        assertEquals("$2a$10$f7h1tWVC5PXEHzx81.UUE.JT/5ZfSxiQyNjVQTTV2S.wwBYLks0Xi", encodedPassword);
//        assertEquals("$2a$10$lcwrBKeqzxzFUgInhXKI8eyOV4JFUwoflnWenTPuyGZQMEq8CsUQa", encodedUserPassword);
        assertTrue(result, "passwordEncoder.matches should return true for the correct password");
    }

    /**
     * 验证 BCryptPasswordEncoder.matches 在密码错误时返回 false.
     */
    @Test
    void testPasswordEncoderMatchesWithIncorrectPassword() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String rawPassword = "admin123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        boolean result = passwordEncoder.matches("wrongPassword", encodedPassword);

        assertFalse(result, "passwordEncoder.matches should return false for an incorrect password");
    }
}
