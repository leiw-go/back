/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * JWT 令牌生成与校验的工具类。
 */
@Component
public class JwtUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtil.class);

    private final long expiration;

    private final SecretKey secretKey;

    /**
     * 用配置的密钥和过期时间构造 JwtUtil。
     *
     * @param secret     base64 编码的密钥
     * @param expiration 令牌过期时间（毫秒）
     */
    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        this.expiration = expiration;
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 为指定用户名生成 JWT 令牌。
     *
     * @param username 嵌入令牌的用户名
     * @return 生成的 JWT 令牌字符串
     */
    public String generateToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 从 JWT 令牌里解析出用户名。
     *
     * @param token JWT 令牌
     * @return 嵌入令牌的用户名
     * @throws JwtException 令牌无效或已过期
     */
    public String getUsernameFromToken(String token) {
        Jws<Claims> claimsJws = parseToken(token);
        return claimsJws.getPayload().getSubject();
    }

    /**
     * 校验 JWT 令牌是否有效。
     *
     * @param token 待校验的 JWT 令牌
     * @return 令牌有效返回 true，否则返回 false
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            LOGGER.warn("Invalid JWT token: {}", exception.getMessage());
            return false;
        }
    }

    private Jws<Claims> parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token);
    }
}
