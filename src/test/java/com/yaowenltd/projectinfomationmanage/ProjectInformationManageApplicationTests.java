/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
     * <p>
     * 不去断言 {@code encode(...)} 的输出等于某个具体的哈希字符串 —— BCrypt 每次
     * 调用都会生成随机 salt，输出天然不可复现；要"验证 hash 与 seed 中的某段对得上"
     * 应走 {@code matches(raw, seedHash)} 通路，参见 {@link #testMatchesSeedHashFromDataSql}.
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
     * 验证 {@code data.sql} / {@code data-user.sql} 中种子账户的 BCrypt 哈希与明文密码匹配.
     * <p>
     * 原本 {@code testPasswordEncoderMatchesWithCorrectPassword} 试图把 {@code encode("user123")}
     * 的输出与种子哈希做 {@code assertEquals}，但 BCrypt 是随机 salt，每次输出都不一样，这个断言
     * 永远过不了。本测试把契约改成"种子哈希能被同一份明文密码还原"，逻辑上等价于
     * 验证 Service 启动时 {@code AuthServiceImpl#login} 能在真实 DB 里匹配这两个种子账户。
     * </p>
     */
    @Test
    void testMatchesSeedHashFromDataSql() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

        // 取自 src/main/resources/sql/data.sql（admin/admin123）
        String adminSeedHash = "$2a$10$FtMOPk5bSAG2p6udDM1wy.hf0GCFGeX5hbRg74Bh6Z1fVQxfByCOi";
        assertTrue(passwordEncoder.matches("admin123", adminSeedHash),
                "admin seed hash in data.sql should match raw password 'admin123'");

        // 取自 src/main/resources/sql/data-user.sql（testuser/test123456）
        String testUserSeedHash = "$2a$10$aV7SsUHGnfAyHN5dIzpCW.ljxQbHSHQKwBxV3UPeZ2LR0pweelWbK";
        assertTrue(passwordEncoder.matches("test123456", testUserSeedHash),
                "testuser seed hash in data-user.sql should match raw password 'test123456'");
    }
}
