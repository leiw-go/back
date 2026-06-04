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
 * Utility class for JWT token generation and validation.
 */
@Component
public class JwtUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtUtil.class);

    private final long expiration;

    private final SecretKey secretKey;

    /**
     * Constructs a JwtUtil with the configured secret and expiration.
     *
     * @param secret     the base64-encoded secret key
     * @param expiration the token expiration time in milliseconds
     */
    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        this.expiration = expiration;
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a JWT token for the given username.
     *
     * @param username the username to embed in the token
     * @return the generated JWT token string
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
     * Extracts the username from a JWT token.
     *
     * @param token the JWT token
     * @return the username embedded in the token
     * @throws JwtException if the token is invalid or expired
     */
    public String getUsernameFromToken(String token) {
        Jws<Claims> claimsJws = parseToken(token);
        return claimsJws.getPayload().getSubject();
    }

    /**
     * Validates a JWT token.
     *
     * @param token the JWT token to validate
     * @return true if the token is valid, false otherwise
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
