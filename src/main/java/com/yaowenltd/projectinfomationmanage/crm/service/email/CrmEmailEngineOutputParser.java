/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.email;

/**
 * 邮件引擎输出解析器：把模型输出拆成「主题 + 正文」.
 * <p>
 * prompt 规定模型首行输出 {@code Subject: <主题>}，空一行后是正文。
 * 这里做一次容错解析：
 * <ul>
 *   <li>首行（前导空行忽略）以 {@code Subject:} 开头 → 主题取冒号后的内容，正文取其后。</li>
 *   <li>没有主题行 → 主题为空，整段都当正文（宁可少一个标题，也不要丢掉内容）。</li>
 * </ul>
 *
 * @since 2026-11-18
 */
public final class CrmEmailEngineOutputParser {

    /** 主题行前缀. */
    private static final String SUBJECT_PREFIX = "subject:";

    /**
     * 工具类，禁止实例化.
     */
    private CrmEmailEngineOutputParser() {
    }

    /**
     * 解析模型输出.
     *
     * @param raw 模型输出的完整文本
     * @return 解析结果
     */
    public static Parsed parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new Parsed("", "");
        }
        String normalized = raw.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);

        int first = 0;
        while (first < lines.length && lines[first].trim().isEmpty()) {
            first++;
        }
        if (first >= lines.length) {
            return new Parsed("", "");
        }
        String head = lines[first].trim();
        if (!head.toLowerCase().startsWith(SUBJECT_PREFIX)) {
            return new Parsed("", normalized.trim());
        }
        String subject = head.substring(SUBJECT_PREFIX.length()).trim();
        // 去掉标题行与正文之间的分隔空行（最多一个）
        int bodyStart = first + 1;
        if (bodyStart < lines.length && lines[bodyStart].trim().isEmpty()) {
            bodyStart++;
        }
        StringBuilder body = new StringBuilder();
        for (int i = bodyStart; i < lines.length; i++) {
            if (body.length() > 0) {
                body.append('\n');
            }
            body.append(lines[i]);
        }
        return new Parsed(subject, body.toString().trim());
    }

    /**
     * 解析结果.
     *
     * @param subject 主题
     * @param body    正文
     */
    public record Parsed(String subject, String body) {
    }
}
