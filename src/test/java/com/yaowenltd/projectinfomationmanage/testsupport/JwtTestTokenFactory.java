/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.testsupport;

import com.yaowenltd.projectinfomationmanage.common.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 集成测试专用的 JWT token 工厂 —— 调用真实 {@link JwtUtil} 签发 token，
 * 保证与生产路径行为一致；测试类只需注入本 Bean 即可拿到 {@code Authorization} 头。
 * <p>
 * 配套提供 {@link #expiredBearer(String)} 生成已过期 token（用于权限矩阵 C 行）。
 * </p>
 *
 * @since 2026-08-05
 */
@Component
public class JwtTestTokenFactory {

    private final JwtUtil jwtUtil;

    private final long expirationMs;

    private final String secretBase64;

    /**
     * 用真实 JwtUtil、配置的过期时间、配置的 secret base64 构造工厂.
     * <p>
     * 三者均来自 Spring 上下文（{@code @DynamicPropertySource} 注入随机 secret 到
     * {@code jwt.secret}，测试 yml 注入 {@code jwt.expiration=86400000}）。
     * </p>
     *
     * @param jwtUtil      Spring 注入的 JwtUtil
     * @param expirationMs jwt.expiration（IT profile 默认 86400000ms = 24h）
     * @param secretBase64 与 JwtUtil 同一份 base64 编码 secret —— 用于签发过期 token
     */
    public JwtTestTokenFactory(JwtUtil jwtUtil,
                               @Value("${jwt.expiration:86400000}") long expirationMs,
                               @Value("${jwt.secret}") String secretBase64) {
        this.jwtUtil = jwtUtil;
        this.expirationMs = expirationMs;
        this.secretBase64 = secretBase64;
    }

    /**
     * 生成 {@code Authorization} 头值（含 {@code Bearer } 前缀）.
     *
     * @param username JWT subject
     * @return 形如 {@code Bearer eyJ...}
     */
    public String bearer(String username) {
        return "Bearer " + jwtUtil.generateToken(username);
    }

    /**
     * 生成裸 token 字符串（不含前缀）.
     *
     * @param username JWT subject
     * @return 原始 JWT
     */
    public String rawToken(String username) {
        return jwtUtil.generateToken(username);
    }

    /**
     * 生成已过期的 token —— 临时构造一个 {@link JwtUtil} 把 expiration 设为 {@code -1000ms}，
     * 这样签出来的 token 立即过期，用于验证"过期 token → 401".
     * <p>
     * 复用同一份 {@code jwt.secret} 保证签名一致、只是时间戳在过期区间内。
     * </p>
     *
     * @param username JWT subject
     * @return 已过期的 JWT 字符串（无前缀）
     */
    public String expiredToken(String username) {
        JwtUtil expired = new JwtUtil(secretBase64, -1000L);
        return expired.generateToken(username);
    }

    /**
     * 已过期 token 的 Bearer 头.
     *
     * @param username JWT subject
     * @return 形如 {@code Bearer eyJ...}（已过期）
     */
    public String expiredBearer(String username) {
        return "Bearer " + expiredToken(username);
    }

    /**
     * 拿到测试用的过期时间（毫秒）.
     *
     * @return 当前测试 profile 的 jwt.expiration
     */
    public long getExpirationMs() {
        return expirationMs;
    }
}