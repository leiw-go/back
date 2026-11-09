/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaowenltd.projectinfomationmanage.crm.config.CrmCurrencyProperties;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CurrencyMetadata;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CurrencyQuote;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.CrmCurrencySourceClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * {@link CrmCurrencySourceClient} 的单元测试.
 * <p>
 * 用的是两个免费源**实测抓下来的真实报文**（2026-11-08）：
 * 主源 Frankfurter v2 是数组格式，比对源 open.er-api v6 的时间戳是 RFC-1123 文本 +
 * Unix 秒级时间戳，两者都不能按 {@code yyyy-MM-dd} 解析 —— 这是踩过的坑。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CrmCurrencySourceClientTests {

    /** 主源真实报文（截取）. */
    private static final String PRIMARY_BODY = "[{\"date\":\"2026-11-08\",\"base\":\"USD\",\"quote\":\"CNY\","
            + "\"rate\":6.6639},{\"date\":\"2026-11-06\",\"base\":\"USD\",\"quote\":\"EUR\",\"rate\":0.89205}]";

    /** 比对源真实报文（截取）. */
    private static final String SECONDARY_BODY = "{\"result\":\"success\","
            + "\"provider\":\"https://www.exchangerate-api.com\","
            + "\"time_last_update_unix\":1794096151,"
            + "\"time_last_update_utc\":\"Sun, 08 Nov 2026 00:02:31 +0000\","
            + "\"base_code\":\"USD\",\"rates\":{\"CNY\":6.690826,\"EUR\":0.895793,\"USD\":1}}";

    /** 主源币种字典真实报文（截取）. */
    private static final String CURRENCY_BODY = "[{\"iso_code\":\"CNY\",\"iso_numeric\":\"156\","
            + "\"name\":\"Chinese Renminbi Yuan\",\"symbol\":\"¥\",\"type\":\"currency\","
            + "\"start_date\":\"1981-01-02\",\"end_date\":\"2026-11-08\"},"
            + "{\"iso_code\":\"AAVE\",\"iso_numeric\":null,\"name\":\"Aave\",\"symbol\":\"AAVE\","
            + "\"type\":\"crypto\",\"start_date\":\"2020-10-10\",\"end_date\":\"2026-11-08\"}]";

    @Mock
    private RestTemplate restTemplate;

    private CrmCurrencyProperties properties;
    private CrmCurrencySourceClient client;

    @BeforeEach
    void setUp() {
        properties = new CrmCurrencyProperties();
        client = new CrmCurrencySourceClientImpl(restTemplate, new ObjectMapper(), properties);
    }

    @Test
    @DisplayName("主源报文能解析出币种、汇率与各自的日期")
    void testFetchPrimaryParsesRates() {
        when(restTemplate.getForObject(properties.getSync().getPrimaryUrl(), String.class))
                .thenReturn(PRIMARY_BODY);

        List<CurrencyQuote> quotes = client.fetchPrimary();

        assertEquals(2, quotes.size());
        CurrencyQuote cny = quotes.get(0);
        assertEquals("CNY", cny.getCurrencyCode());
        assertEquals(0, new BigDecimal("6.6639").compareTo(cny.getRate()));
        assertEquals(LocalDate.of(2026, 11, 8), cny.getRateDate());
        assertEquals(LocalDate.of(2026, 11, 6), quotes.get(1).getRateDate());
    }

    @Test
    @DisplayName("主源可按配置的币种清单过滤")
    void testFetchPrimaryFiltersByConfiguredQuotes() {
        properties.getSync().setQuotes(java.util.Collections.singletonList("CNY"));
        when(restTemplate.getForObject(properties.getSync().getPrimaryUrl(), String.class))
                .thenReturn(PRIMARY_BODY);

        List<CurrencyQuote> quotes = client.fetchPrimary();

        assertEquals(1, quotes.size());
        assertEquals("CNY", quotes.get(0).getCurrencyCode());
    }

    @Test
    @DisplayName("比对源报文能解析出汇率表与快照日期")
    void testFetchSecondaryParsesSnapshot() {
        when(restTemplate.getForObject(properties.getSync().getSecondaryUrl(), String.class))
                .thenReturn(SECONDARY_BODY);

        CrmCurrencySourceClient.SecondarySnapshot snapshot = client.fetchSecondary();

        assertEquals(LocalDate.of(2026, 11, 8), snapshot.getDate());
        assertEquals(3, snapshot.getRates().size());
        assertEquals(0, new BigDecimal("6.690826").compareTo(snapshot.rateOf("CNY")));
        assertEquals(0, BigDecimal.ONE.compareTo(snapshot.rateOf("USD")));
        assertNull(snapshot.rateOf("JPY"));
    }

    @Test
    @DisplayName("币种字典只保留法定币种")
    void testFetchCurrencyMetadataKeepsOnlyFiat() {
        when(restTemplate.getForObject(properties.getSync().getPrimaryCurrencyUrl(), String.class))
                .thenReturn(CURRENCY_BODY);

        List<CurrencyMetadata> metadataList = client.fetchCurrencyMetadata();

        assertEquals(1, metadataList.size());
        CurrencyMetadata cny = metadataList.get(0);
        assertEquals("CNY", cny.getCurrencyCode());
        assertEquals("Chinese Renminbi Yuan", cny.getCurrencyName());
        assertEquals("156", cny.getNumericCode());
    }

    @Test
    @DisplayName("空报文视为数据源不可用并抛业务异常")
    void testEmptyBodyThrows() {
        when(restTemplate.getForObject(properties.getSync().getPrimaryUrl(), String.class)).thenReturn("");

        assertThrows(IllegalStateException.class, () -> client.fetchPrimary());
    }

    @Test
    @DisplayName("非法 JSON 视为数据源不可用并抛业务异常")
    void testMalformedBodyThrows() {
        when(restTemplate.getForObject(properties.getSync().getSecondaryUrl(), String.class)).thenReturn("not-json");

        assertThrows(IllegalStateException.class, () -> client.fetchSecondary());
    }

    @Test
    @DisplayName("比对源缺某币种时返回 null 而不是抛异常")
    void testMissingCurrencyReturnsNull() {
        when(restTemplate.getForObject(properties.getSync().getSecondaryUrl(), String.class))
                .thenReturn(SECONDARY_BODY);

        CrmCurrencySourceClient.SecondarySnapshot snapshot = client.fetchSecondary();

        assertNotNull(snapshot);
        assertTrue(snapshot.rateOf("JPY") == null);
    }

}
