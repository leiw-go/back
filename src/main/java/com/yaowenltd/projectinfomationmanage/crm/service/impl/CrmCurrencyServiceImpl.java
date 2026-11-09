/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.yaowenltd.projectinfomationmanage.crm.config.CrmCurrencyProperties;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmAccessMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CurrencyMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CurrencyRateCheckMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CurrencyRateMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CurrencySyncLogMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrossCheckOutcome;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrossCheckRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencyConvertDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencyDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencyRateCheckDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencyRateDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencyRateRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencySyncStatusDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CurrencyMetadata;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CurrencyQuote;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.PlausibilityOutcome;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.message.MessageDraft;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmCurrencyEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmCurrencyRateCheckEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmCurrencyRateEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmCurrencySyncLogEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.CurrencyCheckResult;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.MessageType;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmCurrencyService;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmCurrencySourceClient;
import com.yaowenltd.projectinfomationmanage.crm.service.CurrencyCrossCheckService;
import com.yaowenltd.projectinfomationmanage.crm.service.CurrencyPlausibilityService;
import com.yaowenltd.projectinfomationmanage.crm.service.CurrencyRateQueryService;
import com.yaowenltd.projectinfomationmanage.crm.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * CRM 汇率服务实现.
 * <p>
 * 按需求文档（汇率专项文档）§4 / §5：
 * <ul>
 *   <li>同步走"拉两源 → 交叉比对 → 合理性校验 → 入库"四道闸门；</li>
 *   <li>偏差超阈值**拒绝入库**（拒绝记录在比对明细里），连续超标触发币种级熔断；</li>
 *   <li>手工维护的汇率（{@code source = MANUAL}）同步任务不覆盖；</li>
 *   <li>读路径只读本地表，永远不实时调外部接口。</li>
 * </ul>
 *
 * @since 2026-11-08
 */
@Service
public class CrmCurrencyServiceImpl implements CrmCurrencyService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrmCurrencyServiceImpl.class);

    /** 汇率小数位. */
    private static final int RATE_SCALE = 8;

    /** 金额小数位. */
    private static final int AMOUNT_SCALE = 2;

    /** 币种状态：正常. */
    private static final String CURRENCY_STATUS_NORMAL = "NORMAL";

    /** 币种状态：熔断中. */
    private static final String CURRENCY_STATUS_SUSPECT = "SUSPECT";

    /** 币种状态：停用. */
    private static final String CURRENCY_STATUS_DISABLED = "DISABLED";

    /** 币种分级：核心. */
    private static final String CURRENCY_LEVEL_CORE = "CORE";

    /** 币种分级：一般. */
    private static final String CURRENCY_LEVEL_NORMAL = "NORMAL";

    /** 来源：手工维护. */
    private static final String SOURCE_MANUAL = "MANUAL";

    /** 来源：主源. */
    private static final String SOURCE_PRIMARY = "FRANKFURTER";

    /** 来源：比对源（主源不可用时接管取值）. */
    private static final String SOURCE_SECONDARY = "OPEN_ER_API";

    /** 比对源标识. */
    private static final String CROSS_SOURCE_SECONDARY = "OPEN_ER_API";

    /** 交叉比对状态：一致. */
    private static final String CROSS_MATCH = "MATCH";

    /** 交叉比对状态：警告. */
    private static final String CROSS_WARN = "WARN";

    /** 交叉比对状态：单源未交叉验证. */
    private static final String CROSS_SINGLE = "SINGLE";

    /** 交叉比对状态：拒绝. */
    private static final String CROSS_REJECT = "REJECT";

    /** 交叉比对状态：手工维护. */
    private static final String CROSS_MANUAL = "MANUAL";

    /** 同步触发方式：手动. */
    private static final String TRIGGER_MANUAL = "MANUAL";

    /** 同步触发方式：定时. */
    private static final String TRIGGER_SCHEDULE = "SCHEDULE";

    /** 权限码：CRM 访问. */
    private static final String PERMISSION_CRM_ACCESS = "crm:access";

    /** 批次日志 error_msg 的最大长度. */
    private static final int ERROR_MSG_MAX_LENGTH = 1000;

    private final CurrencyMapper currencyMapper;
    private final CurrencyRateMapper currencyRateMapper;
    private final CurrencySyncLogMapper currencySyncLogMapper;
    private final CurrencyRateCheckMapper currencyRateCheckMapper;
    private final CrmAccessMapper crmAccessMapper;
    private final CrmCurrencySourceClient sourceClient;
    private final CurrencyCrossCheckService currencyCrossCheckService;
    private final CurrencyPlausibilityService currencyPlausibilityService;
    private final CurrencyRateQueryService currencyRateQueryService;
    private final MessageService messageService;
    private final CrmCurrencyProperties properties;

    /** 比对源快照缓存（比对源官方建议 ≤1 次/24h，手动重复触发时复用）. */
    private volatile CrmCurrencySourceClient.SecondarySnapshot cachedSecondary;

    /** 比对源快照的抓取时间. */
    private volatile LocalDateTime cachedSecondaryAt;

    /** 法定币种字典缓存（主源币种清单）. */
    private volatile Map<String, CurrencyMetadata> cachedCurrencyDictionary;

    /** 法定币种字典的抓取时间. */
    private volatile LocalDateTime cachedCurrencyDictionaryAt;

    /** 法定币种字典缓存有效期（小时）. */
    private static final int CURRENCY_DICTIONARY_CACHE_HOURS = 24;

    /**
     * 使用指定依赖构造 CrmCurrencyServiceImpl.
     *
     * @param currencyMapper              币种 Mapper
     * @param currencyRateMapper          汇率 Mapper
     * @param currencySyncLogMapper       同步批次 Mapper
     * @param currencyRateCheckMapper     比对明细 Mapper
     * @param crmAccessMapper             权限 Mapper
     * @param sourceClient                外部数据源客户端
     * @param currencyCrossCheckService   交叉比对服务
     * @param currencyPlausibilityService 合理性校验服务
     * @param currencyRateQueryService    汇率折算服务
     * @param messageService              站内信服务
     * @param properties                  汇率配置
     */
    public CrmCurrencyServiceImpl(CurrencyMapper currencyMapper,
                                  CurrencyRateMapper currencyRateMapper,
                                  CurrencySyncLogMapper currencySyncLogMapper,
                                  CurrencyRateCheckMapper currencyRateCheckMapper,
                                  CrmAccessMapper crmAccessMapper,
                                  CrmCurrencySourceClient sourceClient,
                                  CurrencyCrossCheckService currencyCrossCheckService,
                                  CurrencyPlausibilityService currencyPlausibilityService,
                                  CurrencyRateQueryService currencyRateQueryService,
                                  MessageService messageService,
                                  CrmCurrencyProperties properties) {
        this.currencyMapper = currencyMapper;
        this.currencyRateMapper = currencyRateMapper;
        this.currencySyncLogMapper = currencySyncLogMapper;
        this.currencyRateCheckMapper = currencyRateCheckMapper;
        this.crmAccessMapper = crmAccessMapper;
        this.sourceClient = sourceClient;
        this.currencyCrossCheckService = currencyCrossCheckService;
        this.currencyPlausibilityService = currencyPlausibilityService;
        this.currencyRateQueryService = currencyRateQueryService;
        this.messageService = messageService;
        this.properties = properties;
    }

    @Override
    public List<CrmCurrencyDto> listCurrencies() {
        List<CrmCurrencyEntity> entities = currencyMapper.selectAll();
        List<CrmCurrencyDto> rows = new ArrayList<>(entities.size());
        for (CrmCurrencyEntity entity : entities) {
            rows.add(toDto(entity));
        }
        return rows;
    }

    @Override
    public List<CrmCurrencyRateDto> listRates(String baseCurrency, String quoteCurrency, LocalDate date,
                                              String source) {
        List<CrmCurrencyRateEntity> entities =
                currencyRateMapper.selectRates(baseCurrency, quoteCurrency, date, source);
        List<CrmCurrencyRateDto> rows = new ArrayList<>(entities.size());
        for (CrmCurrencyRateEntity entity : entities) {
            rows.add(toRateDto(entity));
        }
        return rows;
    }

    @Override
    public CrmCurrencyRateDto saveManualRate(CrmCurrencyRateRequest request) {
        if (request == null || request.getRate() == null) {
            throw new IllegalArgumentException("汇率值不能为空");
        }
        if (request.getQuoteCurrency() == null || request.getQuoteCurrency().trim().isEmpty()) {
            throw new IllegalArgumentException("目标币种不能为空");
        }
        if (request.getRate().signum() <= 0) {
            throw new IllegalArgumentException("汇率必须大于 0");
        }
        LocalDate rateDate = request.getRateDate() == null ? LocalDate.now() : request.getRateDate();
        String quote = request.getQuoteCurrency().trim().toUpperCase(Locale.ROOT);
        String base = properties.getSync().getBaseCurrency();

        CrmCurrencyRateEntity entity = new CrmCurrencyRateEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setBaseCurrency(base);
        entity.setQuoteCurrency(quote);
        entity.setRate(request.getRate().setScale(RATE_SCALE, RoundingMode.HALF_UP));
        entity.setRateDate(rateDate);
        entity.setSource(SOURCE_MANUAL);
        entity.setCrossSource(null);
        entity.setCrossStatus(CROSS_MANUAL);
        entity.setDeviationRate(null);
        entity.setDateGapDays(0);
        entity.setCheckedAt(LocalDateTime.now());
        // 唯一键 (base, quote, rate_date)：同一天重复维护走 upsert，覆盖成 MANUAL
        currencyRateMapper.insert(entity);
        ensureCurrencyExists(quote, null);

        CrmCurrencyRateEntity saved = currencyRateMapper.selectByPairDate(base, quote, rateDate);
        return toRateDto(saved == null ? entity : saved);
    }

    @Override
    public CrmCurrencyConvertDto convert(BigDecimal amount, String from, String to, LocalDate date) {
        if (amount == null) {
            throw new IllegalArgumentException("金额不能为空");
        }
        if (from == null || from.trim().isEmpty() || to == null || to.trim().isEmpty()) {
            throw new IllegalArgumentException("源币与目标币不能为空");
        }
        LocalDate actualDate = date == null ? LocalDate.now() : date;
        String fromCurrency = from.trim().toUpperCase(Locale.ROOT);
        String toCurrency = to.trim().toUpperCase(Locale.ROOT);

        CrmCurrencyConvertDto dto = new CrmCurrencyConvertDto();
        dto.setAmount(amount);
        dto.setFromCurrency(fromCurrency);
        dto.setToCurrency(toCurrency);
        dto.setDate(actualDate);

        if (fromCurrency.equals(toCurrency)) {
            dto.setRate(BigDecimal.ONE);
            dto.setRateMissing(false);
            dto.setConvertedAmount(amount.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP));
            return dto;
        }
        BigDecimal converted = currencyRateQueryService.convert(amount, fromCurrency, toCurrency, actualDate);
        if (converted == null) {
            dto.setRateMissing(true);
            dto.setRate(null);
            dto.setConvertedAmount(null);
            return dto;
        }
        dto.setRateMissing(false);
        dto.setRate(amount.signum() == 0 ? BigDecimal.ONE
                : converted.divide(amount, RATE_SCALE, RoundingMode.HALF_UP));
        dto.setConvertedAmount(converted.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrmCurrencySyncStatusDto sync() {
        return doSync(TRIGGER_MANUAL);
    }

    /**
     * 执行一次定时同步.
     *
     * @return 批次摘要
     */
    public CrmCurrencySyncStatusDto syncScheduled() {
        return doSync(TRIGGER_SCHEDULE);
    }

    /**
     * 同步主流程.
     *
     * @param triggerType 触发方式
     * @return 批次摘要
     */
    private CrmCurrencySyncStatusDto doSync(String triggerType) {
        LocalDateTime startedAt = LocalDateTime.now();
        String syncId = UUID.randomUUID().toString();
        CrmCurrencySyncLogEntity log = new CrmCurrencySyncLogEntity();
        log.setId(syncId);
        log.setTriggerType(triggerType);
        log.setPrimaryStatus("SKIPPED");
        log.setPrimaryRows(0);
        log.setSecondaryStatus("SKIPPED");
        log.setSecondaryRows(0);
        log.setComparedCount(0);
        log.setMatchedCount(0);
        log.setWarnedCount(0);
        log.setRejectedCount(0);
        log.setSingleCount(0);
        log.setSavedCount(0);
        log.setSkippedCount(0);
        log.setStartedAt(startedAt);

        List<String> suspended = new ArrayList<>();
        try {
            Map<String, CurrencyMetadata> currencyDictionary = fiatCurrencyDictionary();
            Set<String> fiatCodes = currencyDictionary.keySet();
            List<CurrencyQuote> primary = fetchPrimary(log, fiatCodes);
            CrmCurrencySourceClient.SecondarySnapshot secondary = fetchSecondary(log);
            String rateSource = SOURCE_PRIMARY;
            if (primary == null || primary.isEmpty()) {
                if (secondary == null) {
                    primary = null;
                } else {
                    // 主源不可用 → 比对源接管取值（单源路径，不做交叉比对）
                    primary = quotesFromSecondary(secondary);
                    rateSource = SOURCE_SECONDARY;
                    secondary = null;
                    log.setPrimaryRows(primary.size());
                }
            }
            if (primary == null || primary.isEmpty()) {
                log.setSyncStatus("FAILED");
                log.setErrorMsg("两个数据源都不可用，本次不更新任何汇率");
                notifySyncFailed("两个数据源都不可用，本次不更新任何汇率");
            } else {
                SyncCounters counters = crossCheckAndSave(syncId, primary, secondary, rateSource,
                        currencyDictionary, suspended);
                log.setComparedCount(counters.compared);
                log.setMatchedCount(counters.matched);
                log.setWarnedCount(counters.warned);
                log.setRejectedCount(counters.rejected);
                log.setSingleCount(counters.single);
                log.setSavedCount(counters.saved);
                log.setSkippedCount(counters.skipped);
                log.setMaxDeviation(counters.maxDeviation);
                log.setMaxDeviationCurrency(counters.maxDeviationCurrency);
                boolean degraded = log.getPrimaryStatus() != null && !"OK".equals(log.getPrimaryStatus());
                log.setSyncStatus(counters.saved > 0
                        ? (degraded ? "PARTIAL" : "SUCCESS") : "PARTIAL");
            }
        } catch (Exception e) {
            LOGGER.error("currency sync failed: {}", e.getMessage(), e);
            log.setSyncStatus("FAILED");
            log.setErrorMsg(truncate(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage()));
            notifySyncFailed(log.getErrorMsg());
        } finally {
            log.setFinishedAt(LocalDateTime.now());
            log.setDurationMs((int) ChronoUnit.MILLIS.between(startedAt, log.getFinishedAt()));
            currencySyncLogMapper.insert(log);
            notifySuspended(suspended);
        }
        return toSyncStatus(log);
    }

    /**
     * 拉取主源.
     *
     * @param log 批次日志
     * @return 主源汇率；失败返回 null
     */
    private List<CurrencyQuote> fetchPrimary(CrmCurrencySyncLogEntity log, Set<String> fiatCodes) {
        try {
            List<CurrencyQuote> allQuotes = sourceClient.fetchPrimary();
            List<CurrencyQuote> quotes = new ArrayList<>();
            for (CurrencyQuote quote : allQuotes) {
                if (isFiatCurrency(quote.getCurrencyCode(), fiatCodes)) {
                    quotes.add(quote);
                }
            }
            log.setPrimaryStatus("OK");
            log.setPrimaryRows(quotes.size());
            LocalDate latest = null;
            for (CurrencyQuote quote : quotes) {
                if (quote.getRateDate() != null && (latest == null || quote.getRateDate().isAfter(latest))) {
                    latest = quote.getRateDate();
                }
            }
            log.setPrimaryDate(latest);
            return quotes;
        } catch (Exception e) {
            LOGGER.warn("primary currency source failed: {}", e.getMessage());
            log.setPrimaryStatus("FAILED");
            return null;
        }
    }

    /**
     * 截断过长的错误信息（{@code t_currency_sync_log.error_msg} 只有 1024 字符）.
     *
     * @param message 原始错误信息
     * @return 截断后的错误信息
     */
    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() <= ERROR_MSG_MAX_LENGTH ? message : message.substring(0, ERROR_MSG_MAX_LENGTH);
    }

    /**
     * 取法定币种码集合.
     * <p>
     * **主源的汇率接口会混进加密货币**（实测 200 条里有 AAVE / ADA / ATOM 这类 4 位码，
     * 它们不是 ISO 4217 法定币种，且 {@code t_currency.currency_code} 只有 3 位），
     * 所以必须用主源的币种字典（{@code type = currency}）过滤后再入库。
     * <p>
     * 字典每天拉一次即可（24h 缓存）；拉不到时退化为"只认 3 位码"的保守规则。
     *
     * @return 币种码 → 元数据；取不到时为空 Map（表示不按字典过滤）
     */
    private Map<String, CurrencyMetadata> fiatCurrencyDictionary() {
        Map<String, CurrencyMetadata> cached = cachedCurrencyDictionary;
        LocalDateTime cachedAt = cachedCurrencyDictionaryAt;
        if (cached != null && cachedAt != null
                && cachedAt.isAfter(LocalDateTime.now().minusHours(CURRENCY_DICTIONARY_CACHE_HOURS))) {
            return cached;
        }
        try {
            List<CurrencyMetadata> metadataList = sourceClient.fetchCurrencyMetadata();
            Map<String, CurrencyMetadata> dictionary = new HashMap<>();
            for (CurrencyMetadata metadata : metadataList) {
                dictionary.put(metadata.getCurrencyCode(), metadata);
            }
            cachedCurrencyDictionary = dictionary;
            cachedCurrencyDictionaryAt = LocalDateTime.now();
            LOGGER.info("currency dictionary cached: {} fiat currencies", dictionary.size());
            return dictionary;
        } catch (Exception e) {
            LOGGER.warn("fetch currency dictionary failed, fallback to 3-letter codes only: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 判断某币种码是否为可入库的法定币种.
     *
     * @param currencyCode 币种码
     * @param fiatCodes    法定币种码集合；为空表示没有字典，按 3 位码兜底
     * @return 是法定币种返回 true
     */
    private boolean isFiatCurrency(String currencyCode, Set<String> fiatCodes) {
        if (currencyCode == null || currencyCode.isEmpty()) {
            return false;
        }
        if (fiatCodes != null && !fiatCodes.isEmpty()) {
            return fiatCodes.contains(currencyCode);
        }
        return currencyCode.length() == 3;
    }

    /**
     * 拉取比对源（24h 内重复触发时复用上次快照）.
     *
     * @param log 批次日志
     * @return 比对源快照；不可用返回 null
     */
    private CrmCurrencySourceClient.SecondarySnapshot fetchSecondary(CrmCurrencySyncLogEntity log) {
        CrmCurrencySourceClient.SecondarySnapshot cached = cachedSecondary;
        LocalDateTime cachedAt = cachedSecondaryAt;
        int minIntervalHours = properties.getSync().getSecondary().getMinIntervalHours();
        if (cached != null && cachedAt != null && minIntervalHours > 0
                && cachedAt.isAfter(LocalDateTime.now().minusHours(minIntervalHours))) {
            log.setSecondaryStatus("REUSED");
            log.setSecondaryRows(cached.getRates() == null ? 0 : cached.getRates().size());
            log.setSecondaryDate(cached.getDate());
            LOGGER.info("secondary currency source reused from cache (fetched at {})", cachedAt);
            return cached;
        }
        try {
            CrmCurrencySourceClient.SecondarySnapshot snapshot = sourceClient.fetchSecondary();
            cachedSecondary = snapshot;
            cachedSecondaryAt = LocalDateTime.now();
            log.setSecondaryStatus("OK");
            log.setSecondaryRows(snapshot.getRates() == null ? 0 : snapshot.getRates().size());
            log.setSecondaryDate(snapshot.getDate());
            return snapshot;
        } catch (Exception e) {
            LOGGER.warn("secondary currency source failed: {}", e.getMessage());
            log.setSecondaryStatus(e.getMessage() != null && e.getMessage().contains("429")
                    ? "RATE_LIMITED" : "FAILED");
            return null;
        }
    }

    /**
     * 把比对源快照转成主源格式的报价列表（主源不可用时接管取值）.
     *
     * @param snapshot 比对源快照
     * @return 报价列表
     */
    private List<CurrencyQuote> quotesFromSecondary(CrmCurrencySourceClient.SecondarySnapshot snapshot) {
        List<CurrencyQuote> quotes = new ArrayList<>();
        if (snapshot.getRates() == null) {
            return quotes;
        }
        String base = properties.getSync().getBaseCurrency();
        for (Map.Entry<String, BigDecimal> entry : snapshot.getRates().entrySet()) {
            if (entry.getKey().equals(base)) {
                continue;
            }
            CurrencyQuote quote = new CurrencyQuote();
            quote.setCurrencyCode(entry.getKey());
            quote.setRate(entry.getValue());
            quote.setRateDate(snapshot.getDate());
            quotes.add(quote);
        }
        return quotes;
    }

    /**
     * 交叉比对 + 合理性校验 + 入库.
     */
    private SyncCounters crossCheckAndSave(String syncId, List<CurrencyQuote> primary,
                                           CrmCurrencySourceClient.SecondarySnapshot secondary,
                                           String rateSource,
                                           Map<String, CurrencyMetadata> currencyDictionary,
                                           List<String> suspended) {
        SyncCounters counters = new SyncCounters();
        List<CrmCurrencyRateCheckEntity> checks = new ArrayList<>();
        Map<String, CrmCurrencyEntity> currencyStates = new HashMap<>();
        String base = properties.getSync().getBaseCurrency();

        for (CurrencyQuote quote : primary) {
            String code = quote.getCurrencyCode();
            if (code == null || code.isEmpty() || quote.getRate() == null) {
                continue;
            }
            if (quote.getRateDate() == null) {
                counters.skipped++;
                continue;
            }
            CrmCurrencyEntity currency = currencyStates.get(code);
            if (currency == null) {
                currency = ensureCurrencyExists(code, currencyDictionary.get(code));
                currencyStates.put(code, currency);
            }
            if (currency == null) {
                continue;
            }
            if (CURRENCY_STATUS_SUSPECT.equals(currency.getStatus())
                    || CURRENCY_STATUS_DISABLED.equals(currency.getStatus())) {
                // 熔断 / 停用的币种本次不入库，沿用最后一次成功值
                counters.skipped++;
                continue;
            }

            CrmCurrencyRateCheckEntity check = new CrmCurrencyRateCheckEntity();
            check.setId(UUID.randomUUID().toString());
            check.setSyncId(syncId);
            check.setBaseCurrency(base);
            check.setQuoteCurrency(code);
            check.setPrimaryRate(quote.getRate());
            check.setPrimaryDate(quote.getRateDate());
            check.setSecondaryDate(secondary == null ? null : secondary.getDate());
            check.setCheckedAt(LocalDateTime.now());
            check.setThreshold(properties.thresholdOf(code));
            check.setDateGapDays(0);

            BigDecimal secondaryRate = secondary == null ? null : secondary.rateOf(code);
            check.setSecondaryRate(secondaryRate);

            CurrencyCheckResult crossResult;
            BigDecimal deviation = null;
            int dateGapDays = 0;
            if (secondaryRate == null) {
                // 比对源缺该币种（或比对源整体不可用）→ 单源路径
                crossResult = CurrencyCheckResult.SINGLE;
                check.setRejectReason("NOT_IN_SECONDARY");
            } else {
                CrossCheckRequest request = new CrossCheckRequest();
                request.setPrimary(quote);
                CurrencyQuote secondaryQuote = new CurrencyQuote();
                secondaryQuote.setCurrencyCode(code);
                secondaryQuote.setRate(secondaryRate);
                secondaryQuote.setRateDate(secondary.getDate());
                request.setSecondary(secondaryQuote);
                request.setThreshold(properties.thresholdOf(code));
                request.setMaxDateGapDays(properties.getCrossCheck().getMaxDateGapDays());
                request.setCoreCurrency(properties.isCoreCurrency(code));

                CrossCheckOutcome outcome = currencyCrossCheckService.check(request);
                crossResult = outcome.getResult();
                deviation = outcome.getDeviation();
                dateGapDays = outcome.getDateGapDays();
                check.setDeviationRate(deviation);
                check.setDateGapDays(dateGapDays);
                check.setThreshold(outcome.getAppliedThreshold());
                if (crossResult == CurrencyCheckResult.REJECT) {
                    check.setRejectReason("DEVIATION_EXCEEDED");
                } else if (crossResult == CurrencyCheckResult.SKIP) {
                    check.setRejectReason("DATE_GAP_EXCEEDED");
                }
                counters.compared++;
            }
            if (deviation != null
                    && (counters.maxDeviation == null || deviation.compareTo(counters.maxDeviation) > 0)) {
                counters.maxDeviation = deviation;
                counters.maxDeviationCurrency = code;
            }

            if (crossResult == CurrencyCheckResult.REJECT) {
                // 交叉比对超标：拒绝入库，沿用上次成功值
                counters.rejected++;
                check.setCheckResult(CROSS_REJECT);
                checks.add(check);
                applyBreach(currency, code, suspended);
                continue;
            }

            // 交叉比对通过（或单源路径）→ 合理性校验
            CrmCurrencyRateEntity previous =
                    currencyRateMapper.selectPrevious(base, code, quote.getRateDate());
            PlausibilityOutcome plausibility =
                    currencyPlausibilityService.check(quote, previous == null ? null : previous.getRate());
            if (!plausibility.isPassed()) {
                counters.rejected++;
                check.setCheckResult(CROSS_REJECT);
                check.setRejectReason(plausibility.getReasonCode());
                checks.add(check);
                applyBreach(currency, code, suspended);
                continue;
            }

            String crossStatus;
            if (crossResult == CurrencyCheckResult.SINGLE || crossResult == CurrencyCheckResult.SKIP) {
                crossStatus = CROSS_SINGLE;
                counters.single++;
            } else if (plausibility.isWarn()) {
                crossStatus = CROSS_WARN;
                counters.warned++;
            } else {
                crossStatus = CROSS_MATCH;
                counters.matched++;
            }
            check.setCheckResult(crossStatus);
            checks.add(check);

            // 手工维护的汇率不被同步任务覆盖
            CrmCurrencyRateEntity existing =
                    currencyRateMapper.selectByPairDate(base, code, quote.getRateDate());
            if (existing != null && SOURCE_MANUAL.equals(existing.getSource())) {
                counters.skipped++;
                continue;
            }

            CrmCurrencyRateEntity rate = new CrmCurrencyRateEntity();
            rate.setId(UUID.randomUUID().toString());
            rate.setBaseCurrency(base);
            rate.setQuoteCurrency(code);
            rate.setRate(quote.getRate().setScale(RATE_SCALE, RoundingMode.HALF_UP));
            rate.setRateDate(quote.getRateDate());
            rate.setSource(rateSource);
            rate.setCrossSource(CROSS_SOURCE_SECONDARY);
            rate.setCrossStatus(crossStatus);
            rate.setDeviationRate(deviation);
            rate.setDateGapDays(dateGapDays);
            rate.setCheckedAt(LocalDateTime.now());
            currencyRateMapper.insert(rate);
            counters.saved++;
            applyOk(currency, code, suspended);
        }

        if (!checks.isEmpty()) {
            currencyRateCheckMapper.insertBatch(checks);
        }
        return counters;
    }

    /**
     * 记录一次"超标"，累计连续超标天数并按需熔断.
     *
     * @param currency  币种
     * @param code      币种码
     * @param suspended 熔断币种收集列表
     */
    private void applyBreach(CrmCurrencyEntity currency, String code, List<String> suspended) {
        int suspectDays = currency.getSuspectDays() == null ? 0 : currency.getSuspectDays();
        currency.setSuspectDays(suspectDays + 1);
        currency.setRecoverDays(0);
        if (currencyCrossCheckService.shouldSuspend(currency.getSuspectDays(),
                properties.getCrossCheck().getSuspectDays())
                && !CURRENCY_STATUS_SUSPECT.equals(currency.getStatus())) {
            currency.setStatus(CURRENCY_STATUS_SUSPECT);
            suspended.add(code);
            LOGGER.warn("currency {} suspended after {} consecutive breaches", code, currency.getSuspectDays());
        }
        currencyMapper.update(currency);
    }

    /**
     * 记录一次"达标"，累计连续达标天数并按需解除熔断.
     *
     * @param currency  币种
     * @param code      币种码
     * @param suspended 熔断币种收集列表
     */
    private void applyOk(CrmCurrencyEntity currency, String code, List<String> suspended) {
        int recoverDays = currency.getRecoverDays() == null ? 0 : currency.getRecoverDays();
        currency.setRecoverDays(recoverDays + 1);
        currency.setSuspectDays(0);
        if (CURRENCY_STATUS_SUSPECT.equals(currency.getStatus())
                && currencyCrossCheckService.shouldRecover(currency.getRecoverDays(),
                properties.getCrossCheck().getRecoverDays())) {
            currency.setStatus(CURRENCY_STATUS_NORMAL);
            LOGGER.info("currency {} recovered from suspect state", code);
        }
        currencyMapper.update(currency);
    }

    /**
     * 保证 {@code t_currency} 里有该币种；没有则新建.
     *
     * @param code     币种码
     * @param metadata 币种元数据；为空时只补最小字段
     * @return 币种实体
     */
    private CrmCurrencyEntity ensureCurrencyExists(String code, CurrencyMetadata metadata) {
        CrmCurrencyEntity existing = currencyMapper.selectByCode(code);
        if (existing != null) {
            if (metadata != null && !metadata.getCurrencyName().equals(existing.getCurrencyName())) {
                existing.setCurrencyName(metadata.getCurrencyName());
                existing.setSymbol(metadata.getSymbol());
                existing.setNumericCode(metadata.getNumericCode());
                currencyMapper.update(existing);
            }
            return existing;
        }
        CrmCurrencyEntity entity = new CrmCurrencyEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setCurrencyCode(code);
        entity.setCurrencyName(metadata == null ? code : metadata.getCurrencyName());
        entity.setSymbol(metadata == null ? null : metadata.getSymbol());
        entity.setNumericCode(metadata == null ? null : metadata.getNumericCode());
        entity.setIsCommon(properties.isCoreCurrency(code) ? 1 : 0);
        entity.setCurrencyLevel(properties.isCoreCurrency(code) ? CURRENCY_LEVEL_CORE : CURRENCY_LEVEL_NORMAL);
        entity.setStatus(CURRENCY_STATUS_NORMAL);
        entity.setSuspectDays(0);
        entity.setRecoverDays(0);
        currencyMapper.insert(entity);
        return entity;
    }

    /**
     * 同步币种字典（名称 / 符号 / 数字码）.
     *
     * @return 同步到的币种数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int syncCurrencyDictionary() {
        List<CurrencyMetadata> metadataList;
        try {
            metadataList = sourceClient.fetchCurrencyMetadata();
        } catch (Exception e) {
            LOGGER.warn("fetch currency dictionary failed: {}", e.getMessage());
            return 0;
        }
        int count = 0;
        for (CurrencyMetadata metadata : metadataList) {
            if (ensureCurrencyExists(metadata.getCurrencyCode(), metadata) != null) {
                count++;
            }
        }
        return count;
    }

    @Override
    public CrmCurrencySyncStatusDto syncStatus() {
        CrmCurrencySyncLogEntity log = currencySyncLogMapper.selectLatest();
        return log == null ? null : toSyncStatus(log);
    }

    @Override
    public List<CrmCurrencyRateCheckDto> verification(String quoteCurrency, String checkResult) {
        List<CrmCurrencyRateCheckEntity> entities =
                currencyRateCheckMapper.selectPage(quoteCurrency, checkResult);
        List<CrmCurrencyRateCheckDto> rows = new ArrayList<>(entities.size());
        for (CrmCurrencyRateCheckEntity entity : entities) {
            CrmCurrencyRateCheckDto dto = new CrmCurrencyRateCheckDto();
            dto.setId(entity.getId());
            dto.setSyncId(entity.getSyncId());
            dto.setBaseCurrency(entity.getBaseCurrency());
            dto.setQuoteCurrency(entity.getQuoteCurrency());
            dto.setPrimaryRate(entity.getPrimaryRate());
            dto.setPrimaryDate(entity.getPrimaryDate());
            dto.setSecondaryRate(entity.getSecondaryRate());
            dto.setSecondaryDate(entity.getSecondaryDate());
            dto.setDeviationRate(entity.getDeviationRate());
            dto.setDateGapDays(entity.getDateGapDays());
            dto.setThreshold(entity.getThreshold());
            dto.setCheckResult(entity.getCheckResult());
            dto.setRejectReason(entity.getRejectReason());
            dto.setCheckedAt(entity.getCheckedAt());
            rows.add(dto);
        }
        return rows;
    }

    /**
     * 给全部持有 {@code crm:access} 的用户发汇率同步失败告警.
     *
     * @param reason 失败原因
     */
    private void notifySyncFailed(String reason) {
        try {
            List<String> receivers = crmAccessMapper.selectUserIdsWithPermission(PERMISSION_CRM_ACCESS);
            if (receivers == null || receivers.isEmpty()) {
                return;
            }
            MessageDraft draft = new MessageDraft();
            draft.setMessageType(MessageType.RATE_SYNC_FAILED.name());
            draft.setLevel("URGENT");
            draft.setTitle("汇率同步失败");
            draft.setContent("汇率同步失败：" + reason);
            draft.setBizType("CURRENCY");
            messageService.sendToReceivers(draft, receivers);
        } catch (Exception e) {
            LOGGER.warn("send rate sync failed message failed: {}", e.getMessage());
        }
    }

    /**
     * 给全部持有 {@code crm:access} 的用户发币种熔断告警.
     *
     * @param suspended 本次熔断的币种
     */
    private void notifySuspended(List<String> suspended) {
        if (suspended == null || suspended.isEmpty()) {
            return;
        }
        try {
            List<String> receivers = crmAccessMapper.selectUserIdsWithPermission(PERMISSION_CRM_ACCESS);
            if (receivers == null || receivers.isEmpty()) {
                return;
            }
            MessageDraft draft = new MessageDraft();
            draft.setMessageType(MessageType.RATE_CURRENCY_SUSPENDED.name());
            draft.setLevel("IMPORTANT");
            draft.setTitle("汇率币种熔断");
            draft.setContent("以下币种连续偏差超标，已停止入库并沿用最后一次成功值："
                    + String.join("、", suspended));
            draft.setBizType("CURRENCY");
            messageService.sendToReceivers(draft, receivers);
        } catch (Exception e) {
            LOGGER.warn("send rate suspended message failed: {}", e.getMessage());
        }
    }

    private CrmCurrencyDto toDto(CrmCurrencyEntity entity) {
        CrmCurrencyDto dto = new CrmCurrencyDto();
        dto.setId(entity.getId());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setCurrencyName(entity.getCurrencyName());
        dto.setSymbol(entity.getSymbol());
        dto.setNumericCode(entity.getNumericCode());
        dto.setIsCommon(entity.getIsCommon());
        dto.setCurrencyLevel(entity.getCurrencyLevel());
        dto.setStatus(entity.getStatus());
        dto.setSuspectDays(entity.getSuspectDays());
        dto.setRecoverDays(entity.getRecoverDays());
        return dto;
    }

    private CrmCurrencyRateDto toRateDto(CrmCurrencyRateEntity entity) {
        CrmCurrencyRateDto dto = new CrmCurrencyRateDto();
        dto.setId(entity.getId());
        dto.setBaseCurrency(entity.getBaseCurrency());
        dto.setQuoteCurrency(entity.getQuoteCurrency());
        dto.setRate(entity.getRate());
        dto.setRateDate(entity.getRateDate());
        dto.setSource(entity.getSource());
        dto.setCrossSource(entity.getCrossSource());
        dto.setCrossStatus(entity.getCrossStatus());
        dto.setDeviationRate(entity.getDeviationRate());
        dto.setDateGapDays(entity.getDateGapDays());
        return dto;
    }

    private CrmCurrencySyncStatusDto toSyncStatus(CrmCurrencySyncLogEntity log) {
        CrmCurrencySyncStatusDto dto = new CrmCurrencySyncStatusDto();
        dto.setSyncId(log.getId());
        dto.setTriggerType(log.getTriggerType());
        dto.setSyncStatus(log.getSyncStatus());
        dto.setPrimaryStatus(log.getPrimaryStatus());
        dto.setPrimaryRows(log.getPrimaryRows());
        dto.setPrimaryDate(log.getPrimaryDate());
        dto.setSecondaryStatus(log.getSecondaryStatus());
        dto.setSecondaryRows(log.getSecondaryRows());
        dto.setSecondaryDate(log.getSecondaryDate());
        dto.setComparedCount(log.getComparedCount());
        dto.setMatchedCount(log.getMatchedCount());
        dto.setWarnedCount(log.getWarnedCount());
        dto.setRejectedCount(log.getRejectedCount());
        dto.setSingleCount(log.getSingleCount());
        dto.setSavedCount(log.getSavedCount());
        dto.setSkippedCount(log.getSkippedCount());
        dto.setMaxDeviation(log.getMaxDeviation());
        dto.setMaxDeviationCurrency(log.getMaxDeviationCurrency());
        dto.setErrorMsg(log.getErrorMsg());
        dto.setDurationMs(log.getDurationMs());
        dto.setStartedAt(log.getStartedAt());
        dto.setFinishedAt(log.getFinishedAt());
        dto.setStale(log.getFinishedAt() == null
                || log.getFinishedAt().isBefore(LocalDateTime.now().minusDays(1)));
        return dto;
    }

    /**
     * 同步计数.
     */
    private static class SyncCounters {

        /** 参与比对的币种数. */
        private int compared;

        /** 一致数. */
        private int matched;

        /** 警告数. */
        private int warned;

        /** 拒绝数. */
        private int rejected;

        /** 单源数. */
        private int single;

        /** 入库数. */
        private int saved;

        /** 跳过数（熔断币种 / 手工维护行 / 缺日期）. */
        private int skipped;

        /** 最大偏差（%）. */
        private BigDecimal maxDeviation;

        /** 最大偏差对应币种. */
        private String maxDeviationCurrency;

    }

}
