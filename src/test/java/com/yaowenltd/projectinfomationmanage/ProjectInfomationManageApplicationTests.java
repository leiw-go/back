/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProjectInfomationManageApplication.
 */
@SpringBootTest
class ProjectInfomationManageApplicationTests {

    /**
     * Context loads test.
     */
    @Test
    void contextLoads() {
    }

    /**
     * Tests that BCryptPasswordEncoder.matches returns true for the correct password.
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
     * Tests that BCryptPasswordEncoder.matches returns false for an incorrect password.
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
