/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CurrencyMetadata;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CurrencyQuote;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 汇率外部数据源客户端.
 * <p>
 * 按需求文档（汇率专项文档）§2：主源 = Frankfurter v2（各国央行参考价），
 * 比对源 = open.er-api v6（市场中间价快照）。两个源都**不需要 API Key**。
 *
 * @since 2026-11-08
 */
public interface CrmCurrencySourceClient {

    /**
     * 拉取主源汇率（Frankfurter v2）.
     * <p>
     * 每个币种自带自己的 {@code rate_date}（各国央行发布节奏不同），
     * 入库时**按该币种自己的日期存**，不统一成同步日。
     *
     * @return 主源汇率列表
     */
    List<CurrencyQuote> fetchPrimary();

    /**
     * 拉取比对源汇率（open.er-api v6）.
     *
     * @return 比对源快照
     */
    SecondarySnapshot fetchSecondary();

    /**
     * 拉取主源的币种字典（名称 / 符号 / 数字码）.
     * <p>
     * 只返回**法定币种**（type = currency），加密货币与贵金属一律过滤掉。
     *
     * @return 币种元数据列表
     */
    List<CurrencyMetadata> fetchCurrencyMetadata();

    /**
     * 比对源快照.
     */
    class SecondarySnapshot {

        /** 币种 → 汇率. */
        private final Map<String, BigDecimal> rates;

        /** 快照日期. */
        private final LocalDate date;

        /**
         * 构造比对源快照.
         *
         * @param rates 币种 → 汇率
         * @param date  快照日期
         */
        public SecondarySnapshot(Map<String, BigDecimal> rates, LocalDate date) {
            this.rates = rates;
            this.date = date;
        }

        /**
         * 取某币种的汇率.
         *
         * @param currencyCode 币种码
         * @return 汇率；没有则返回 null
         */
        public BigDecimal rateOf(String currencyCode) {
            return rates == null ? null : rates.get(currencyCode);
        }

        /**
         * 取快照日期.
         *
         * @return 快照日期
         */
        public LocalDate getDate() {
            return date;
        }

        /**
         * 取汇率表.
         *
         * @return 币种 → 汇率
         */
        public Map<String, BigDecimal> getRates() {
            return rates;
        }

    }

}
