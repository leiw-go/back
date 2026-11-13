/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.config.CrmCurrencyProperties;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmCountryRefMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CurrencyRateMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.country.CrmCountryInfoDto;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmCountryRefEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmCurrencyRateEntity;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.CountryReferenceServiceImpl;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * {@link CountryReferenceService} 的单元测试.
 * <p>
 * 契约：输入国家 → 该国时区、当前时间、主要货币、对美元与人民币的汇率；
 * 汇率取本地汇率表最近一次成功值（基准币 USD），缺失时标 {@code rateMissing} 而不报错。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CountryReferenceServiceTests {

    @Mock
    private CrmCountryRefMapper countryRefMapper;

    @Mock
    private CurrencyRateMapper currencyRateMapper;

    private CountryReferenceService countryReferenceService;

    @BeforeEach
    void setUp() {
        countryReferenceService = new CountryReferenceServiceImpl(countryRefMapper, currencyRateMapper,
                new CrmCurrencyProperties());
    }

    private static CrmCountryRefEntity country(String code, String zh, String timezone, String currency) {
        CrmCountryRefEntity entity = new CrmCountryRefEntity();
        entity.setId("R-" + code);
        entity.setCountryCode(code);
        entity.setCountryName("Country " + code);
        entity.setCountryNameZh(zh);
        entity.setTimezone(timezone);
        entity.setTimezones(timezone);
        entity.setCurrencyCode(currency);
        entity.setCurrencyName(currency + " currency");
        entity.setCurrencySymbol("¤");
        entity.setIsCommon(1);
        return entity;
    }

    private static CrmCurrencyRateEntity rate(String quote, String value) {
        CrmCurrencyRateEntity entity = new CrmCurrencyRateEntity();
        entity.setBaseCurrency("USD");
        entity.setQuoteCurrency(quote);
        entity.setRate(new BigDecimal(value));
        entity.setRateDate(LocalDate.of(2026, 11, 8));
        entity.setSource("FRANKFURTER");
        entity.setCrossStatus("MATCH");
        return entity;
    }

    @Test
    @DisplayName("按关键词查询国家返回分页结果")
    void testListByKeyword() {
        when(countryRefMapper.selectPage("日本")).thenReturn(
                Collections.singletonList(country("JP", "日本", "Asia/Tokyo", "JPY")));

        PageResponse<com.yaowenltd.projectinfomationmanage.crm.model.dto.country.CrmCountryRefDto> page =
                countryReferenceService.list("日本", 1, 20);

        assertEquals(1, page.getTotal());
        assertEquals("JP", page.getData().get(0).getCountryCode());
        assertEquals("Asia/Tokyo", page.getData().get(0).getTimezone());
    }

    @Test
    @DisplayName("国家信息包含时区与当地时间")
    void testGetInfoReturnsLocalTime() {
        when(countryRefMapper.selectByKeyword("日本")).thenReturn(
                country("JP", "日本", "Asia/Tokyo", "JPY"));

        CrmCountryInfoDto dto = countryReferenceService.getInfo("日本");

        assertEquals("JP", dto.getCountryCode());
        assertEquals("Asia/Tokyo", dto.getTimezone());
        assertEquals("JPY", dto.getCurrencyCode());
        assertNotNull(dto.getCurrentTime());
        assertNotNull(dto.getCurrentTimeText());
        assertEquals("+09:00", dto.getUtcOffset());
        // 东京 UTC+9，比北京（UTC+8）早 1 小时
        assertEquals(0, new BigDecimal("1").compareTo(dto.getHoursFromBeijing()));
        assertTrue(dto.getBeijingOffsetText().contains("比北京时间早"));
    }

    @Test
    @DisplayName("北京时区的国家时差为 0")
    void testSameTimeZoneAsBeijing() {
        when(countryRefMapper.selectByKeyword("中国")).thenReturn(
                country("CN", "中国", "Asia/Shanghai", "CNY"));

        CrmCountryInfoDto dto = countryReferenceService.getInfo("中国");

        assertEquals(0, BigDecimal.ZERO.compareTo(dto.getHoursFromBeijing()));
        assertEquals("与北京时间相同", dto.getBeijingOffsetText());
    }

    @Test
    @DisplayName("对美元与人民币的汇率按 USD 基准三角折算")
    void testRatesAgainstUsdAndCny() {
        when(countryRefMapper.selectByKeyword("日本")).thenReturn(
                country("JP", "日本", "Asia/Tokyo", "JPY"));
        when(currencyRateMapper.selectLatest("USD", "JPY")).thenReturn(rate("JPY", "150"));
        when(currencyRateMapper.selectLatest("USD", "CNY")).thenReturn(rate("CNY", "7"));

        CrmCountryInfoDto dto = countryReferenceService.getInfo("日本");

        assertFalse(dto.isRateMissing());
        assertEquals("2026-11-08", dto.getRateDate());
        // 1 美元 = 150 日元
        assertEquals(0, new BigDecimal("150.00000000").compareTo(dto.getRateFromUsd()));
        // 1 日元 = 1/150 美元
        assertEquals(0, new BigDecimal("0.00666667").compareTo(dto.getRateToUsd()));
        // 1 人民币 = 150/7 日元
        assertEquals(0, new BigDecimal("21.42857143").compareTo(dto.getRateFromCny()));
        // 1 日元 = 7/150 人民币
        assertEquals(0, new BigDecimal("0.04666667").compareTo(dto.getRateToCny()));
    }

    @Test
    @DisplayName("美元本币的汇率为 1:1")
    void testUsdCountryRateIsOne() {
        when(countryRefMapper.selectByKeyword("美国")).thenReturn(
                country("US", "美国", "America/New_York", "USD"));
        when(currencyRateMapper.selectLatest("USD", "CNY")).thenReturn(rate("CNY", "7"));

        CrmCountryInfoDto dto = countryReferenceService.getInfo("美国");

        assertFalse(dto.isRateMissing());
        assertEquals(0, BigDecimal.ONE.compareTo(dto.getRateFromUsd()));
        assertEquals(0, BigDecimal.ONE.compareTo(dto.getRateToUsd()));
        // 1 人民币 = 1/7 美元
        assertEquals(0, new BigDecimal("0.14285714").compareTo(dto.getRateFromCny()));
    }

    @Test
    @DisplayName("汇率缺失时标记 rateMissing，其余字段照常返回")
    void testMissingRateIsFlagged() {
        when(countryRefMapper.selectByKeyword("日本")).thenReturn(
                country("JP", "日本", "Asia/Tokyo", "JPY"));
        when(currencyRateMapper.selectLatest("USD", "JPY")).thenReturn(null);
        when(currencyRateMapper.selectLatest("USD", "CNY")).thenReturn(rate("CNY", "7"));

        CrmCountryInfoDto dto = countryReferenceService.getInfo("日本");

        assertTrue(dto.isRateMissing());
        assertNull(dto.getRateFromUsd());
        assertNull(dto.getRateToCny());
        assertEquals("JP", dto.getCountryCode());
        assertNotNull(dto.getCurrentTime());
    }

    @Test
    @DisplayName("查不到国家时抛业务异常")
    void testCountryNotFound() {
        when(countryRefMapper.selectByKeyword("不存在的国家")).thenReturn(null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> countryReferenceService.getInfo("不存在的国家"));
        assertTrue(error.getMessage().contains("country not found"));
    }

    @Test
    @DisplayName("国家为空时抛业务异常")
    void testBlankCountryRejected() {
        assertThrows(IllegalArgumentException.class, () -> countryReferenceService.getInfo(" "));
        assertThrows(IllegalArgumentException.class, () -> countryReferenceService.getInfo(null));
    }

    @Test
    @DisplayName("列表关键词为空时查全部国家")
    void testListWithoutKeyword() {
        List<CrmCountryRefEntity> all = List.of(
                country("US", "美国", "America/New_York", "USD"),
                country("JP", "日本", "Asia/Tokyo", "JPY"));
        when(countryRefMapper.selectPage(any())).thenReturn(all);

        PageResponse<com.yaowenltd.projectinfomationmanage.crm.model.dto.country.CrmCountryRefDto> page =
                countryReferenceService.list(null, 1, 20);

        assertEquals(2, page.getData().size());
    }

}
