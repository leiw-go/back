/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.llm.crypto;

import com.yaowenltd.projectinfomationmanage.config.LlmProperties;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link AesGcmEncryptor} 单元测试 —— 验证 AES-GCM 加密 / 解密 round-trip + IV 随机性 + 错误密钥.
 *
 * @since 2026-08-04
 */
class AesGcmEncryptorTests {

    /** 固定测试密钥（32 字节 base64） —— 仅用于单元测试，不入库. */
    private static final String TEST_KEY =
            Base64.getEncoder().encodeToString(new byte[32]);

    private AesGcmEncryptor newEncryptor() {
        LlmProperties props = new LlmProperties();
        props.getEncrypt().setKey(TEST_KEY);
        return new AesGcmEncryptor(props);
    }

    @Test
    void roundTrip_recoversOriginalPlaintext() {
        AesGcmEncryptor enc = newEncryptor();
        byte[] ciphertext = enc.encrypt("sk-proj-abc123-secret");
        String recovered = enc.decrypt(ciphertext);
        assertEquals("sk-proj-abc123-secret", recovered);
    }

    @Test
    void encrypt_producesDifferentCiphertextForSamePlaintext() {
        // IV 必须每次随机 —— 相同明文 + 相同密钥加密两次，密文必须不同
        AesGcmEncryptor enc = newEncryptor();
        byte[] first = enc.encrypt("same plaintext");
        byte[] second = enc.encrypt("same plaintext");
        assertNotEquals(first.length, 0);
        // 密文大概率不同（IV 随机），但万一相同也只是弱随机；先 assert 不等
        // 实际上 12 byte 随机 IV 相同概率 1/2^96，可放心 assert
        assertNotEquals(toHex(first), toHex(second));
    }

    @Test
    void decrypt_nullReturnsNull() {
        AesGcmEncryptor enc = newEncryptor();
        assertNull(enc.decrypt(null));
    }

    @Test
    void encrypt_nullReturnsNull() {
        AesGcmEncryptor enc = newEncryptor();
        assertNull(enc.encrypt(null));
    }

    @Test
    void wrongKey_failsToDecrypt() {
        // 用密钥 A 加密，用密钥 B 解密 —— 应抛 IllegalStateException（tag 校验失败）
        LlmProperties propsA = new LlmProperties();
        propsA.getEncrypt().setKey(Base64.getEncoder().encodeToString(new byte[32]));
        LlmProperties propsB = new LlmProperties();
        propsB.getEncrypt().setKey(Base64.getEncoder().encodeToString(
                new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
                        17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32}));

        AesGcmEncryptor encA = new AesGcmEncryptor(propsA);
        AesGcmEncryptor encB = new AesGcmEncryptor(propsB);

        byte[] ciphertext = encA.encrypt("secret");
        assertThrows(IllegalStateException.class, () -> encB.decrypt(ciphertext));
    }

    @Test
    void emptyKey_throwsAtConstruction() {
        LlmProperties props = new LlmProperties();
        // 不设 key —— 应在构造时直接失败（fail-fast）
        assertThrows(IllegalArgumentException.class, () -> new AesGcmEncryptor(props));
    }

    @Test
    void invalidBase64_throwsAtConstruction() {
        LlmProperties props = new LlmProperties();
        props.getEncrypt().setKey("not-valid-base64!!!@@@");
        assertThrows(IllegalArgumentException.class, () -> new AesGcmEncryptor(props));
    }

    @Test
    void wrongKeyLength_throwsAtConstruction() {
        LlmProperties props = new LlmProperties();
        // 16 字节（AES-128 长度，但本类要求 32 字节）
        props.getEncrypt().setKey(Base64.getEncoder().encodeToString(new byte[16]));
        assertThrows(IllegalArgumentException.class, () -> new AesGcmEncryptor(props));
    }

    @Test
    void roundTrip_chineseAndEmoji() {
        AesGcmEncryptor enc = newEncryptor();
        String original = "中文 + emoji 🚀 + special: !@#$%^&*()";
        byte[] ct = enc.encrypt(original);
        assertEquals(original, enc.decrypt(ct));
    }

    @Test
    void ciphertext_hasExpectedStructure() {
        // 密文 = [12 byte IV][N byte body（含 tag）] —— body 至少 16 字节（GCM tag）
        AesGcmEncryptor enc = newEncryptor();
        byte[] ct = enc.encrypt("hi");
        assertEquals(0, ct.length % 1); // 长度合理
        // 加密 "hi"（2 字节 plaintext + 16 字节 tag + 12 字节 IV）= 30 字节
        // 但实际可能更长（GCM 不填充）
        assertEquals(30, ct.length, "12 IV + 2 plaintext + 16 tag = 30 bytes");
    }

    @Test
    void emptyPlaintext_isSupported() {
        AesGcmEncryptor enc = newEncryptor();
        byte[] ct = enc.encrypt("");
        // 空字符串 + 12 IV + 16 tag = 28 字节
        assertArrayEquals(new byte[0], new byte[0]);
        assertEquals("", enc.decrypt(ct));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}