/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES/CBC/PKCS5Padding 加密工具。
 *
 * <p>用于对聚宽账户的密码等敏感凭据进行加解密存储。密钥在 {@code application.yml}
 * 中通过 {@code quant.aes.key} 配置 (16/24/32 字节); 初始化向量使用密钥的前 16
 * 字节。生产环境务必通过环境变量或密钥管理服务注入密钥, 不要将明文密钥提交进仓库。</p>
 */
@Component
public class AesUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    @Value("${quant.aes.key:}")
    private String configuredKey;

    private SecretKeySpec keySpec;

    private IvParameterSpec ivSpec;

    /**
     * 初始化密钥与 IV. 配置缺失或长度不足时直接抛错, 防止意外使用默认密钥.
     */
    @PostConstruct
    public void init() {
        if (configuredKey == null || configuredKey.trim().length() < 16) {
            throw new IllegalStateException(
                "quant.aes.key is missing or too short (need >= 16 chars). "
                    + "Set the QUANT_AES_KEY env var or `quant.aes.key` in application.yml.");
        }
        byte[] keyBytes = padOrTruncate(configuredKey.getBytes(StandardCharsets.UTF_8), 32);
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
        byte[] iv = new byte[16];
        System.arraycopy(keyBytes, 0, iv, 0, 16);
        this.ivSpec = new IvParameterSpec(iv);
    }

    /**
     * 加密字符串, 返回 Base64.
     *
     * @param plaintext 明文
     * @return 加密后的 Base64
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(cipherBytes);
        } catch (Exception ex) {
            throw new IllegalStateException("AES encryption failed", ex);
        }
    }

    /**
     * 解密 Base64 字符串.
     *
     * @param cipherText 加密后的 Base64
     * @return 明文
     */
    public String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] plainBytes = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("AES decryption failed", ex);
        }
    }

    private static byte[] padOrTruncate(byte[] src, int size) {
        byte[] out = new byte[size];
        if (src.length >= size) {
            System.arraycopy(src, 0, out, 0, size);
            return out;
        }
        System.arraycopy(src, 0, out, 0, src.length);
        for (int i = src.length; i < size; i++) {
            out[i] = (byte) ('0' + (i % 10));
        }
        return out;
    }
}
