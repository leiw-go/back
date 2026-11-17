/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.email;

/**
 * 邮件引擎的语气（按客户关系分两档）.
 * <p>
 * 只有两种：<strong>新客户</strong>（第一次合作，客气礼貌、句式委婉）与
 * <strong>老客户</strong>（长期合作，随和亲近、句式轻松）。
 * 两者都要求邮件<strong>简洁、干练、专业</strong> —— 这一条写在 prompt 的固定骨架里，
 * 不随语气变化。
 * <p>
 * {@code hint} 是写进 prompt 的语气要求 —— 话术集中在这里维护，改话术不用发版前端。
 *
 * @since 2026-11-17
 */
public enum CrmEmailEngineTone {

    /** 新客户：非常客气、礼貌、友好. */
    NEW_CUSTOMER("new_customer", "新客户语气",
            "客户关系：新客户（第一次合作或彼此还不熟）—— 非常客气、礼貌、友好。"
                    + "请求对方帮忙时用委婉句式，例如 “Could you please help me ...”"
                    + "、“I would appreciate it if you could ...”、“Would it be possible to ...”；"
                    + "表达感谢与期待，不要用生硬命令式的祈使句。"),

    /** 老客户：随和、亲近、简单. */
    EXISTING_CUSTOMER("existing_customer", "老客户语气",
            "客户关系：老客户（长期合作，彼此熟悉）—— 随和、亲近、简单直接，"
                    + "不用过度客气。请求对方帮忙时用轻松句式，例如 “Please can you ...”、"
                    + "“Just let me know if ...”；可以有一句自然的过渡，但仍保持专业。");

    /** 语气码（前端传参）. */
    private final String code;

    /** 中文标签（前端展示）. */
    private final String label;

    /** 写进 prompt 的语气要求. */
    private final String hint;

    /**
     * 构造一个语气枚举.
     *
     * @param code  语气码
     * @param label 中文标签
     * @param hint  prompt 里的语气要求
     */
    CrmEmailEngineTone(String code, String label, String hint) {
        this.code = code;
        this.label = label;
        this.hint = hint;
    }

    /**
     * 按语气码取枚举；取不到时回落到新客户（更保守的一档）.
     *
     * @param code 语气码
     * @return 语气枚举
     */
    public static CrmEmailEngineTone of(String code) {
        if (code != null) {
            for (CrmEmailEngineTone tone : values()) {
                if (tone.code.equalsIgnoreCase(code.trim())) {
                    return tone;
                }
            }
        }
        return NEW_CUSTOMER;
    }

    /**
     * 取语气码.
     *
     * @return 语气码
     */
    public String getCode() {
        return code;
    }

    /**
     * 取中文标签.
     *
     * @return 中文标签
     */
    public String getLabel() {
        return label;
    }

    /**
     * 取 prompt 里的语气要求.
     *
     * @return 语气要求
     */
    public String getHint() {
        return hint;
    }
}
