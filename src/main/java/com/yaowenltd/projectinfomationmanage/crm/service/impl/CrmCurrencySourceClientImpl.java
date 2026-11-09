/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaowenltd.projectinfomationmanage.crm.config.CrmCurrencyProperties;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CurrencyMetadata;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CurrencyQuote;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmCurrencySourceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 汇率外部数据源客户端实现.
 * <p>
 * 主源返回的 JSON 形如
 * {@code [{"date":"2026-11-08","base":"USD","quote":"CNY","rate":6.6635}, ...]}；
 * 比对源形如
 * {@code {"result":"success","base_code":"USD","rates":{...},"time_last_update_unix":1794096151}}；
 * 币种字典形如
 * {@code [{"iso_code":"CNY","iso_numeric":"156","name":"Chinese Renminbi Yuan",
 * "symbol":"¥","type":"currency", ...}, ...]}。
 *
 * @since 2026-11-08
 */
@Component
public class CrmCurrencySourceClientImpl implements CrmCurrencySourceClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrmCurrencySourceClientImpl.class);

    /** 币种字典里的法定币种类型. */
    private static final String TYPE_CURRENCY = "currency";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CrmCurrencyProperties properties;

    /**
     * 使用指定依赖构造 CrmCurrencySourceClientImpl.
     * <p>
     * 走独立构造器：外部汇率接口的超时由 {@code crm.currency.sync.timeout-ms} 控制，
     * 不复用工程里那个固定 5s 超时的通用 {@code RestTemplate}。
     *
     * @param objectMapper JSON 解析器
     * @param properties   汇率配置
     */
    @Autowired
    public CrmCurrencySourceClientImpl(ObjectMapper objectMapper, CrmCurrencyProperties properties) {
        this(buildRestTemplate(properties.getSync().getTimeoutMs()), objectMapper, properties);
    }

    /**
     * 使用指定依赖构造 CrmCurrencySourceClientImpl（单元测试用，可注入 mock 的 HTTP 客户端）.
     *
     * @param restTemplate 出站 HTTP 客户端
     * @param objectMapper JSON 解析器
     * @param properties   汇率配置
     */
    public CrmCurrencySourceClientImpl(RestTemplate restTemplate,
                                        ObjectMapper objectMapper,
                                        CrmCurrencyProperties properties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * 按配置的超时时间构造出站 HTTP 客户端.
     *
     * @param timeoutMs 超时（毫秒）
     * @return HTTP 客户端
     */
    private static RestTemplate buildRestTemplate(int timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        RestTemplate template = new RestTemplate(factory);
        template.setErrorHandler(new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }

            @Override
            public void handleError(ClientHttpResponse response) {
                // 不抛异常：由调用方按响应体内容判断数据源是否可用
            }
        });
        return template;
    }

    @Override
    public List<CurrencyQuote> fetchPrimary() {
        String body = restTemplate.getForObject(properties.getSync().getPrimaryUrl(), String.class);
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalStateException("primary currency source returned empty body");
        }
        try {
            JsonNode array = objectMapper.readTree(body);
            Set<String> wanted = wantedQuotes();
            List<CurrencyQuote> quotes = new ArrayList<>();
            for (JsonNode node : array) {
                String quote = node.path("quote").asText(null);
                if (quote == null || quote.isEmpty()) {
                    continue;
                }
                quote = quote.toUpperCase(Locale.ROOT);
                if (!wanted.isEmpty() && !wanted.contains(quote)) {
                    continue;
                }
                CurrencyQuote currencyQuote = new CurrencyQuote();
                currencyQuote.setCurrencyCode(quote);
                currencyQuote.setRate(node.path("rate").decimalValue());
                String date = node.path("date").asText(null);
                currencyQuote.setRateDate(date == null || date.isEmpty() ? null : LocalDate.parse(date));
                quotes.add(currencyQuote);
            }
            LOGGER.info("primary currency source fetched: {} quotes", quotes.size());
            return quotes;
        } catch (Exception e) {
            throw new IllegalStateException("failed to parse primary currency source response", e);
        }
    }

    @Override
    public SecondarySnapshot fetchSecondary() {
        String body = restTemplate.getForObject(properties.getSync().getSecondaryUrl(), String.class);
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalStateException("secondary currency source returned empty body");
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode rates = root.path("rates");
            Map<String, BigDecimal> parsed = new LinkedHashMap<>();
            Iterator<String> names = rates.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                parsed.put(name.toUpperCase(Locale.ROOT), rates.path(name).decimalValue());
            }
            LocalDate date = parseSecondaryDate(root);
            LOGGER.info("secondary currency source fetched: {} rates, date={}", parsed.size(), date);
            return new SecondarySnapshot(parsed, date);
        } catch (Exception e) {
            throw new IllegalStateException("failed to parse secondary currency source response", e);
        }
    }

    @Override
    public List<CurrencyMetadata> fetchCurrencyMetadata() {
        String body = restTemplate.getForObject(properties.getSync().getPrimaryCurrencyUrl(), String.class);
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalStateException("primary currency dictionary returned empty body");
        }
        try {
            JsonNode array = objectMapper.readTree(body);
            LocalDate today = LocalDate.now();
            List<CurrencyMetadata> metadataList = new ArrayList<>();
            for (JsonNode node : array) {
                String type = node.path("type").asText(null);
                if (!TYPE_CURRENCY.equals(type)) {
                    continue;
                }
                String endDate = node.path("end_date").asText(null);
                if (endDate != null && !endDate.isEmpty()) {
                    // end_date 是"最后一次有数据的日期"，不是废止日期：
                    // end_date 是"最后一次有数据的日期"，不是废止日期：在用币种的 end_date 就是昨天，只有长期没有数据的才算已废止。
                    LocalDate end = LocalDate.parse(endDate);
                    if (end.isBefore(today.minusDays(properties.getSync().getCurrencyStaleDays()))) {
                        continue;
                    }
                }
                String code = node.path("iso_code").asText(null);
                if (code == null || code.isEmpty()) {
                    continue;
                }
                CurrencyMetadata metadata = new CurrencyMetadata();
                metadata.setCurrencyCode(code.toUpperCase(Locale.ROOT));
                metadata.setCurrencyName(node.path("name").asText(code));
                String symbol = node.path("symbol").asText(null);
                metadata.setSymbol(symbol == null || symbol.isEmpty() ? null : symbol);
                String numeric = node.path("iso_numeric").asText(null);
                metadata.setNumericCode(numeric == null || numeric.isEmpty() ? null : numeric);
                metadataList.add(metadata);
            }
            LOGGER.info("primary currency dictionary fetched: {} fiat currencies", metadataList.size());
            return metadataList;
        } catch (Exception e) {
            throw new IllegalStateException("failed to parse primary currency dictionary response", e);
        }
    }

    /**
     * 取比对源的快照日期.
     * <p>
     * 优先用 {@code time_last_update_unix}（秒级时间戳，UTC），
     * 退化时解析 {@code time_last_update_utc}（形如 {@code Sun, 08 Nov 2026 00:02:31 +0000}）。
     */
    private LocalDate parseSecondaryDate(JsonNode root) {
        long unix = root.path("time_last_update_unix").asLong(0L);
        if (unix > 0L) {
            return Instant.ofEpochSecond(unix).atZone(ZoneOffset.UTC).toLocalDate();
        }
        String updatedAt = root.path("time_last_update_utc").asText(null);
        if (updatedAt == null || updatedAt.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(updatedAt.substring(0, 10));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 取配置里限定的币种集合；为空表示不限（同步全部法定币种）.
     */
    private Set<String> wantedQuotes() {
        List<String> quotes = properties.getSync().getQuotes();
        Set<String> wanted = new HashSet<>();
        if (quotes != null) {
            for (String quote : quotes) {
                if (quote != null && !quote.trim().isEmpty()) {
                    wanted.add(quote.trim().toUpperCase(Locale.ROOT));
                }
            }
        }
        return wanted;
    }

}
