/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CRM 汇率模块配置.
 * <p>
 * 配置键与 {@code docs/crm-currency-rate-design.md} §8 一一对应：
 * <pre>
 * crm.currency.base-currency
 * crm.currency.sync.*
 * crm.currency.cross-check.*
 * crm.currency.plausibility.*
 * </pre>
 * 配置来源为仓库内的 {@code application-{profile}.yml} 与环境变量；
 * 本工程没有配置中心，改这些值需要重启应用才能生效。
 *
 * @since 2026-11-08
 */
@Component
@ConfigurationProperties(prefix = "crm.currency")
public class CrmCurrencyProperties {

    /** 本位币（折算目标币，CRM 各表的 {@code *_base} 列按它存）. */
    private String baseCurrency = "USD";

    /** 同步相关配置. */
    private Sync sync = new Sync();

    /** 交叉比对相关配置. */
    private CrossCheck crossCheck = new CrossCheck();

    /** 合理性校验相关配置. */
    private Plausibility plausibility = new Plausibility();

    /**
     * 判断某币种是否核心币种.
     *
     * @param currencyCode 币种码
     * @return 是核心币种返回 true
     */
    public boolean isCoreCurrency(String currencyCode) {
        return currencyCode != null && crossCheck.getCoreCurrencies().contains(currencyCode.toUpperCase());
    }

    /**
     * 取某币种适用的偏差阈值.
     *
     * @param currencyCode 币种码
     * @return 阈值（%）
     */
    public BigDecimal thresholdOf(String currencyCode) {
        return isCoreCurrency(currencyCode) ? crossCheck.getThresholdCore() : crossCheck.getThresholdNormal();
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public Sync getSync() {
        return sync;
    }

    public void setSync(Sync sync) {
        this.sync = sync;
    }

    public CrossCheck getCrossCheck() {
        return crossCheck;
    }

    public void setCrossCheck(CrossCheck crossCheck) {
        this.crossCheck = crossCheck;
    }

    public Plausibility getPlausibility() {
        return plausibility;
    }

    public void setPlausibility(Plausibility plausibility) {
        this.plausibility = plausibility;
    }

    /**
     * 同步配置.
     */
    public static class Sync {

        /** 是否开启定时同步. */
        private boolean enabled = true;

        /** 定时同步 cron（Asia/Shanghai）. */
        private String cron = "0 0 7 * * ?";

        /** 落库基准币（统一 USD）. */
        private String baseCurrency = "USD";

        /** 主源地址：Frankfurter v2 汇率. */
        private String primaryUrl = "https://api.frankfurter.dev/v2/rates?base=USD";

        /** 主源币种字典地址：Frankfurter v2 币种清单. */
        private String primaryCurrencyUrl = "https://api.frankfurter.dev/v2/currencies?scope=all";

        /** 比对源地址：open.er-api v6. */
        private String secondaryUrl = "https://open.er-api.com/v6/latest/USD";

        /** 单次请求超时（毫秒）. */
        private int timeoutMs = 10000;

        /** 只同步指定币种；为空表示全部法定币种. */
        private List<String> quotes = new ArrayList<>();

        /** 币种字典里"最后有数据日期"距今超过该天数的币种视为已废止，不入库. */
        private int currencyStaleDays = 30;

        /** 比对源调用保护配置. */
        private Secondary secondary = new Secondary();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public String getBaseCurrency() {
            return baseCurrency;
        }

        public void setBaseCurrency(String baseCurrency) {
            this.baseCurrency = baseCurrency;
        }

        public String getPrimaryUrl() {
            return primaryUrl;
        }

        public void setPrimaryUrl(String primaryUrl) {
            this.primaryUrl = primaryUrl;
        }

        public String getPrimaryCurrencyUrl() {
            return primaryCurrencyUrl;
        }

        public void setPrimaryCurrencyUrl(String primaryCurrencyUrl) {
            this.primaryCurrencyUrl = primaryCurrencyUrl;
        }

        public String getSecondaryUrl() {
            return secondaryUrl;
        }

        public void setSecondaryUrl(String secondaryUrl) {
            this.secondaryUrl = secondaryUrl;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public List<String> getQuotes() {
            return quotes;
        }

        public void setQuotes(List<String> quotes) {
            this.quotes = quotes;
        }

        public int getCurrencyStaleDays() {
            return currencyStaleDays;
        }

        public void setCurrencyStaleDays(int currencyStaleDays) {
            this.currencyStaleDays = currencyStaleDays;
        }

        public Secondary getSecondary() {
            return secondary;
        }

        public void setSecondary(Secondary secondary) {
            this.secondary = secondary;
        }

        /**
         * 比对源调用保护配置.
         */
        public static class Secondary {

            /** 比对源两次调用之间的最小间隔（小时）—— 比对源官方建议 ≤1 次/24h. */
            private int minIntervalHours = 24;

            public int getMinIntervalHours() {
                return minIntervalHours;
            }

            public void setMinIntervalHours(int minIntervalHours) {
                this.minIntervalHours = minIntervalHours;
            }

        }

    }

    /**
     * 交叉比对配置.
     */
    public static class CrossCheck {

        /** 是否开启交叉比对. */
        private boolean enabled = true;

        /** 核心币种清单（阈值 1%）—— 唯一真相源，不在迁移里硬编码. */
        private List<String> coreCurrencies = new ArrayList<>(Arrays.asList(
                "USD", "EUR", "CNY", "JPY", "GBP", "HKD", "KRW", "SGD", "TWD", "AUD", "CAD", "CHF",
                "AED", "SAR", "THB", "MYR", "INR", "RUB", "BRL", "MXN", "VND", "ZAR", "TRY",
                "PLN", "NOK", "SEK", "DKK", "EGP", "PHP", "IDR"));

        /** 核心币种偏差阈值（%）. */
        private BigDecimal thresholdCore = new BigDecimal("1.0");

        /** 一般币种偏差阈值（%）. */
        private BigDecimal thresholdNormal = new BigDecimal("3.0");

        /** 允许的最大数据日期差（天），超过判 SKIP. */
        private int maxDateGapDays = 3;

        /** 连续超标多少天触发熔断. */
        private int suspectDays = 3;

        /** 连续达标多少天解除熔断. */
        private int recoverDays = 5;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getCoreCurrencies() {
            return coreCurrencies;
        }

        public void setCoreCurrencies(List<String> coreCurrencies) {
            this.coreCurrencies = coreCurrencies;
        }

        public BigDecimal getThresholdCore() {
            return thresholdCore;
        }

        public void setThresholdCore(BigDecimal thresholdCore) {
            this.thresholdCore = thresholdCore;
        }

        public BigDecimal getThresholdNormal() {
            return thresholdNormal;
        }

        public void setThresholdNormal(BigDecimal thresholdNormal) {
            this.thresholdNormal = thresholdNormal;
        }

        public int getMaxDateGapDays() {
            return maxDateGapDays;
        }

        public void setMaxDateGapDays(int maxDateGapDays) {
            this.maxDateGapDays = maxDateGapDays;
        }

        public int getSuspectDays() {
            return suspectDays;
        }

        public void setSuspectDays(int suspectDays) {
            this.suspectDays = suspectDays;
        }

        public int getRecoverDays() {
            return recoverDays;
        }

        public void setRecoverDays(int recoverDays) {
            this.recoverDays = recoverDays;
        }

    }

    /**
     * 合理性校验配置（不依赖第二源的单源校验）.
     */
    public static class Plausibility {

        /** 汇率下界. */
        private BigDecimal minRate = new BigDecimal("0.0001");

        /** 汇率上界. */
        private BigDecimal maxRate = new BigDecimal("100000");

        /** 单日环比突变阈值（%）. */
        private BigDecimal maxDailyChange = new BigDecimal("20");

        /** 数据最大可接受陈旧天数. */
        private int maxStaleDays = 7;

        public BigDecimal getMinRate() {
            return minRate;
        }

        public void setMinRate(BigDecimal minRate) {
            this.minRate = minRate;
        }

        public BigDecimal getMaxRate() {
            return maxRate;
        }

        public void setMaxRate(BigDecimal maxRate) {
            this.maxRate = maxRate;
        }

        public BigDecimal getMaxDailyChange() {
            return maxDailyChange;
        }

        public void setMaxDailyChange(BigDecimal maxDailyChange) {
            this.maxDailyChange = maxDailyChange;
        }

        public int getMaxStaleDays() {
            return maxStaleDays;
        }

        public void setMaxStaleDays(int maxStaleDays) {
            this.maxStaleDays = maxStaleDays;
        }

    }

}
