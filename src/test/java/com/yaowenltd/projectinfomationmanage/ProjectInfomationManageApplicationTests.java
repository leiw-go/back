/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        String encodedPassword = passwordEncoder.encode(rawPassword);
        System.out.println(encodedPassword);

        boolean result = passwordEncoder.matches(rawPassword, encodedPassword);

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
