/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.email;

/**
 * 邮件引擎的目标语言.
 * <p>
 * {@code promptName} 是写进 prompt 的语言说明（中英对照），
 * 只给模型看中文名容易出现语言漂移，所以两种都给。
 *
 * @since 2026-11-17
 */
public enum CrmEmailEngineLanguage {

    /** 英语. */
    EN("en", "英语", "英语（English）"),
    /** 德语. */
    DE("de", "德语", "德语（German）"),
    /** 西班牙语. */
    ES("es", "西班牙语", "西班牙语（Spanish）"),
    /** 法语. */
    FR("fr", "法语", "法语（French）"),
    /** 葡萄牙语. */
    PT("pt", "葡萄牙语", "葡萄牙语（Portuguese）"),
    /** 意大利语. */
    IT("it", "意大利语", "意大利语（Italian）"),
    /** 俄语. */
    RU("ru", "俄语", "俄语（Russian）"),
    /** 日语. */
    JA("ja", "日语", "日语（Japanese）"),
    /** 韩语. */
    KO("ko", "韩语", "韩语（Korean）"),
    /** 阿拉伯语. */
    AR("ar", "阿拉伯语", "阿拉伯语（Arabic）"),
    /** 中文. */
    ZH("zh", "中文", "中文（Chinese）");

    /** 语言码（前端传参 / 落库 language 字段）. */
    private final String code;

    /** 中文标签（前端展示）. */
    private final String label;

    /** 写进 prompt 的语言说明. */
    private final String promptName;

    /**
     * 构造一个语言枚举.
     *
     * @param code       语言码
     * @param label      中文标签
     * @param promptName prompt 里的语言说明
     */
    CrmEmailEngineLanguage(String code, String label, String promptName) {
        this.code = code;
        this.label = label;
        this.promptName = promptName;
    }

    /**
     * 按语言码取枚举；取不到时回落到英语.
     *
     * @param code 语言码
     * @return 语言枚举
     */
    public static CrmEmailEngineLanguage of(String code) {
        if (code != null) {
            for (CrmEmailEngineLanguage language : values()) {
                if (language.code.equalsIgnoreCase(code.trim())) {
                    return language;
                }
            }
        }
        return EN;
    }

    /**
     * 取语言码.
     *
     * @return 语言码
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
     * 取 prompt 里的语言说明.
     *
     * @return 语言说明
     */
    public String getPromptName() {
        return promptName;
    }
}
