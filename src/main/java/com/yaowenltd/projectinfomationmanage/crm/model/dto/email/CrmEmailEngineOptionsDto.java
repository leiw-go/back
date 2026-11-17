/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.email;

import lombok.Data;

import java.util.List;

/**
 * CRM 邮件引擎可选项（语言 / 语气 / 场景 / 模型）.
 * <p>
 * 前端初始化页面时拉一次，避免把枚举写死在前端。
 *
 * @since 2026-11-17
 */
@Data
public class CrmEmailEngineOptionsDto {

    /** 目标语言. */
    private List<Option> languages;

    /** 语气. */
    private List<Option> tones;

    /** 业务场景. */
    private List<Option> scenes;

    /** 可用模型（{@code provider:model}）. */
    private List<Option> models;

    /** 默认模型（{@code provider:model}）. */
    private String defaultModel;

    /**
     * 一个可选项.
     */
    @Data
    public static class Option {

        /** 选项值. */
        private String code;

        /** 展示名. */
        private String label;

        /** 补充说明（语气 / 场景的要点，前端 tooltip 用）. */
        private String hint;

        /**
         * 构造一个可选项.
         *
         * @param code 选项值
         * @param label 展示名
         * @param hint  补充说明
         */
        public Option(String code, String label, String hint) {
            this.code = code;
            this.label = label;
            this.hint = hint;
        }

        /**
         * 构造一个没有补充说明的可选项.
         *
         * @param code  选项值
         * @param label 展示名
         */
        public Option(String code, String label) {
            this(code, label, null);
        }
    }
}
