/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.llm.crypto;

import com.yaowenltd.projectinfomationmanage.config.LlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM 应用层加密工具 —— 用于加密 provider 上游 API key.
 * <p>
 * 输出字节数组布局：[12 byte IV][N byte ciphertext][16 byte GCM tag].
 * 解密时按相同布局切分。密钥长度固定 128 bit（GCM 推荐 256 bit 也兼容；本类使用 256 bit 32 字节）。
 * </p>
 *
 * @since 2026-08-04
 */
@Component
public class AesGcmEncryptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AesGcmEncryptor.class);

    /** IV 长度（GCM 标准 12 字节）. */
    private static final int IV_LENGTH_BYTES = 12;

    /** GCM tag 长度（128 bit）—— Sun JCE 支持 128/120/112/104/96 等. */
    private static final int GCM_TAG_LENGTH_BITS = 128;

    /** AES key 长度（32 字节 = 256 bit）—— base64 编码后 44 字符. */
    private static final int KEY_LENGTH_BYTES = 32;

    private final SecretKey secretKey;

    private final SecureRandom random = new SecureRandom();

    /**
     * 使用 LLM 配置中的密钥构造 encryptor.
     *
     * @param llmProperties LLM 配置（读 {@code llm.encrypt.key}）
     * @throws IllegalArgumentException 密钥长度不对或 base64 解析失败
     */
    public AesGcmEncryptor(LlmProperties llmProperties) {
        String base64Key = llmProperties.getEncrypt().getKey();
        if (base64Key == null || base64Key.isBlank()) {
            throw new IllegalArgumentException(
                    "LLM 加密密钥未配置：请设置环境变量 LLM_ENCRYPT_KEY（32 字节 base64）");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("LLM 加密密钥 base64 解析失败", e);
        }
        if (keyBytes.length != KEY_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "LLM 加密密钥长度必须是 " + KEY_LENGTH_BYTES + " 字节（256 bit），当前 "
                            + keyBytes.length + " 字节");
        }
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /**
     * 加密明文.
     *
     * @param plaintext 明文
     * @return 密文（含 IV + ciphertext + tag）
     */
    public byte[] encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey,
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertextAndTag = cipher.doFinal(
                    plaintext.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(IV_LENGTH_BYTES + ciphertextAndTag.length);
            buffer.put(iv);
            buffer.put(ciphertextAndTag);
            return buffer.array();
        } catch (Exception e) {
            LOGGER.error("AES-GCM 加密失败", e);
            throw new IllegalStateException("AES-GCM 加密失败", e);
        }
    }

    /**
     * 解密密文.
     *
     * @param ciphertext 密文（含 IV + ciphertext + tag）
     * @return 明文
     */
    public String decrypt(byte[] ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        try {
            if (ciphertext.length < IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("密文长度过短");
            }
            byte[] iv = new byte[IV_LENGTH_BYTES];
            System.arraycopy(ciphertext, 0, iv, 0, IV_LENGTH_BYTES);

            byte[] body = new byte[ciphertext.length - IV_LENGTH_BYTES];
            System.arraycopy(ciphertext, IV_LENGTH_BYTES, body, 0, body.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey,
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(body);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error("AES-GCM 解密失败", e);
            throw new IllegalStateException("AES-GCM 解密失败", e);
        }
    }
}