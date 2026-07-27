/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * BCryptPasswordEncoder 的纯 JUnit 5 单元测试。
 * <p>
 * 不依赖 Spring 上下文，不读取 {@code application-test.yml}，
 * 不连接任何外部基础设施；与 {@link ProjectInformationManageApplicationTests}
 * 的 context 冒烟测试解耦。
 * </p>
 */
class BCryptPasswordEncoderTests {

    /**
     * 验证 BCryptPasswordEncoder.matches 在密码正确时返回 true.
     * <p>
     * 不去断言 {@code encode(...)} 的输出等于某个具体的哈希字符串 —— BCrypt 每次
     * 调用都会生成随机 salt，输出天然不可复现；要"验证 hash 与 seed 中的某段对得上"
     * 应走 {@code matches(raw, seedHash)} 通路，参见 {@link #testMatchesSeedHashFromV1Migration}.
     * </p>
     */
    @Test
    void testPasswordEncoderMatchesWithCorrectPassword() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String rawPassword = "admin123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        boolean result = passwordEncoder.matches(rawPassword, encodedPassword);

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

    /**
     * 验证 {@code V1__baseline_project_info_manage.sql} 中种子账户的 BCrypt 哈希与明文密码匹配.
     * <p>
     * 本方法不读取任何 SQL 文件，也不连接数据库；它只校验仓库中已固定的 BCrypt
     * 哈希字符串与明文密码之间的契约，防止种子数据被误改。
     * </p>
     */
    @Test
    void testMatchesSeedHashFromV1Migration() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        // 取自 src/main/resources/db/migration/V1__baseline_project_info_manage.sql（admin/admin123）
        String adminSeedHash = "$2a$10$FtMOPk5bSAG2p6udDM1wy.hf0GCFGeX5hbRg74Bh6Z1fVQxfByCOi";
        assertTrue(passwordEncoder.matches("admin123", adminSeedHash),
                "admin seed hash in V1 migration should match raw password 'admin123'");

        // 取自 src/main/resources/db/migration/V1__baseline_project_info_manage.sql（testuser/test123456）
        String testUserSeedHash = "$2a$10$aV7SsUHGnfAyHN5dIzpCW.ljxQbHSHQKwBxV3UPeZ2LR0pweelWbK";
        assertTrue(passwordEncoder.matches("test123456", testUserSeedHash),
                "testuser seed hash in V1 migration should match raw password 'test123456'");
    }
}
