/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.config.CrmCurrencyProperties;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmAccessMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CurrencyMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CurrencyRateCheckMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CurrencyRateMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CurrencySyncLogMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencyConvertDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencyRateRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrmCurrencySyncStatusDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CurrencyMetadata;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CurrencyQuote;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmCurrencyEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmCurrencyRateEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmCurrencySyncLogEntity;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.CrmCurrencyServiceImpl;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.CurrencyCrossCheckServiceImpl;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.CurrencyPlausibilityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CrmCurrencyService} 的单元测试.
 * <p>
 * 契约来自汇率专项文档 §4 / §5：双源交叉比对后才入库，偏差超标**拒绝入库**，
 * 连续超标触发币种级熔断，手工维护的汇率不被同步任务覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CrmCurrencyServiceTests {

    /** 同步日. */
    private static final LocalDate SYNC_DATE = LocalDate.of(2026, 11, 8);

    @Mock
    private CurrencyMapper currencyMapper;

    @Mock
    private CurrencyRateMapper currencyRateMapper;

    @Mock
    private CurrencySyncLogMapper currencySyncLogMapper;

    @Mock
    private CurrencyRateCheckMapper currencyRateCheckMapper;

    @Mock
    private CrmAccessMapper crmAccessMapper;

    @Mock
    private CrmCurrencySourceClient sourceClient;

    @Mock
    private CurrencyRateQueryService currencyRateQueryService;

    @Mock
    private MessageService messageService;

    private CrmCurrencyProperties properties;
    private CrmCurrencyService currencyService;

    @BeforeEach
    void setUp() {
        properties = new CrmCurrencyProperties();
        currencyService = new CrmCurrencyServiceImpl(currencyMapper, currencyRateMapper,
                currencySyncLogMapper, currencyRateCheckMapper, crmAccessMapper, sourceClient,
                new CurrencyCrossCheckServiceImpl(), new CurrencyPlausibilityServiceImpl(properties),
                currencyRateQueryService, messageService, properties);
    }

    private static CurrencyQuote quote(String code, String rate, LocalDate date) {
        CurrencyQuote q = new CurrencyQuote();
        q.setCurrencyCode(code);
        q.setRate(new BigDecimal(rate));
        q.setRateDate(date);
        return q;
    }

    private static CrmCurrencySourceClient.SecondarySnapshot secondary(Map<String, String> rates,
                                                                     LocalDate date) {
        Map<String, BigDecimal> parsed = new LinkedHashMap<>();
        rates.forEach((k, v) -> parsed.put(k, new BigDecimal(v)));
        return new CrmCurrencySourceClient.SecondarySnapshot(parsed, date);
    }

    private static CrmCurrencyEntity currency(String code, String status, int suspectDays, int recoverDays) {
        CrmCurrencyEntity entity = new CrmCurrencyEntity();
        entity.setId("CU-" + code);
        entity.setCurrencyCode(code);
        entity.setCurrencyName(code);
        entity.setStatus(status);
        entity.setSuspectDays(suspectDays);
        entity.setRecoverDays(recoverDays);
        entity.setCurrencyLevel("CORE");
        entity.setIsCommon(1);
        return entity;
    }

    @Test
    @DisplayName("两源偏差在阈值内时入库并标记 MATCH")
    void testSyncStoresMatchedRate() {
        when(sourceClient.fetchPrimary()).thenReturn(List.of(quote("CNY", "6.6635", SYNC_DATE)));
        when(sourceClient.fetchSecondary())
                .thenReturn(secondary(Collections.singletonMap("CNY", "6.6908"), SYNC_DATE));
        when(currencyMapper.selectByCode("CNY")).thenReturn(null);
        when(currencyRateMapper.selectPrevious(eq("USD"), eq("CNY"), any())).thenReturn(null);
        when(currencyRateMapper.selectByPairDate(eq("USD"), eq("CNY"), any())).thenReturn(null);

        CrmCurrencySyncStatusDto status = currencyService.sync();

        ArgumentCaptor<CrmCurrencyRateEntity> captor = ArgumentCaptor.forClass(CrmCurrencyRateEntity.class);
        verify(currencyRateMapper).insert(captor.capture());
        CrmCurrencyRateEntity saved = captor.getValue();
        assertEquals("MATCH", saved.getCrossStatus());
        assertEquals("FRANKFURTER", saved.getSource());
        assertEquals("CNY", saved.getQuoteCurrency());
        assertEquals(SYNC_DATE, saved.getRateDate());

        assertEquals("SUCCESS", status.getSyncStatus());
        assertEquals(1, status.getMatchedCount());
        assertEquals(1, status.getSavedCount());
        assertEquals(0, status.getRejectedCount());
        verify(currencySyncLogMapper).insert(any(CrmCurrencySyncLogEntity.class));
        verify(currencyRateCheckMapper).insertBatch(any());
    }

    @Test
    @DisplayName("两源偏差超过阈值时拒绝入库")
    void testSyncRejectsRateBeyondThreshold() {
        when(sourceClient.fetchPrimary()).thenReturn(List.of(quote("CNY", "6.0", SYNC_DATE)));
        when(sourceClient.fetchSecondary())
                .thenReturn(secondary(Collections.singletonMap("CNY", "6.5"), SYNC_DATE));
        when(currencyMapper.selectByCode("CNY")).thenReturn(currency("CNY", "NORMAL", 0, 0));

        CrmCurrencySyncStatusDto status = currencyService.sync();

        verify(currencyRateMapper, never()).insert(any(CrmCurrencyRateEntity.class));
        assertEquals(1, status.getRejectedCount());
        assertEquals(0, status.getSavedCount());
    }

    @Test
    @DisplayName("连续超标达到阈值天数时币种熔断并告警")
    void testCurrencySuspendedAfterConsecutiveBreaches() {
        when(sourceClient.fetchPrimary()).thenReturn(List.of(quote("CNY", "6.0", SYNC_DATE)));
        when(sourceClient.fetchSecondary())
                .thenReturn(secondary(Collections.singletonMap("CNY", "6.5"), SYNC_DATE));
        when(currencyMapper.selectByCode("CNY")).thenReturn(currency("CNY", "NORMAL", 2, 0));
        when(crmAccessMapper.selectUserIdsWithPermission("crm:access"))
                .thenReturn(Collections.singletonList("U1"));

        currencyService.sync();

        ArgumentCaptor<CrmCurrencyEntity> captor = ArgumentCaptor.forClass(CrmCurrencyEntity.class);
        verify(currencyMapper).update(captor.capture());
        assertEquals("SUSPECT", captor.getValue().getStatus());
        assertEquals(3, captor.getValue().getSuspectDays());
        verify(messageService).sendToReceivers(any(), eq(Collections.singletonList("U1")));
    }

    @Test
    @DisplayName("熔断中的币种本次不入库")
    void testSuspectCurrencyIsSkipped() {
        when(sourceClient.fetchPrimary()).thenReturn(List.of(quote("CNY", "6.6635", SYNC_DATE)));
        when(sourceClient.fetchSecondary())
                .thenReturn(secondary(Collections.singletonMap("CNY", "6.6908"), SYNC_DATE));
        when(currencyMapper.selectByCode("CNY")).thenReturn(currency("CNY", "SUSPECT", 3, 0));

        CrmCurrencySyncStatusDto status = currencyService.sync();

        verify(currencyRateMapper, never()).insert(any(CrmCurrencyRateEntity.class));
        assertEquals(1, status.getSkippedCount());
        assertEquals(0, status.getSavedCount());
    }

    @Test
    @DisplayName("手工维护的汇率不被同步任务覆盖")
    void testManualRateIsNotOverwritten() {
        CrmCurrencyRateEntity manual = new CrmCurrencyRateEntity();
        manual.setSource("MANUAL");
        manual.setRate(new BigDecimal("6.70000000"));
        when(sourceClient.fetchPrimary()).thenReturn(List.of(quote("CNY", "6.6635", SYNC_DATE)));
        when(sourceClient.fetchSecondary())
                .thenReturn(secondary(Collections.singletonMap("CNY", "6.6908"), SYNC_DATE));
        when(currencyMapper.selectByCode("CNY")).thenReturn(currency("CNY", "NORMAL", 0, 0));
        when(currencyRateMapper.selectPrevious(eq("USD"), eq("CNY"), any())).thenReturn(null);
        when(currencyRateMapper.selectByPairDate(eq("USD"), eq("CNY"), any())).thenReturn(manual);

        CrmCurrencySyncStatusDto status = currencyService.sync();

        verify(currencyRateMapper, never()).insert(any(CrmCurrencyRateEntity.class));
        assertEquals(1, status.getSkippedCount());
        assertEquals(0, status.getSavedCount());
    }

    @Test
    @DisplayName("比对源缺该币种时走单源路径入库并标记 SINGLE")
    void testSingleSourcePathWhenCurrencyMissingInSecondary() {
        when(sourceClient.fetchPrimary()).thenReturn(List.of(quote("ZWL", "26.87", SYNC_DATE)));
        when(sourceClient.fetchSecondary())
                .thenReturn(secondary(Collections.singletonMap("CNY", "6.6908"), SYNC_DATE));
        when(currencyMapper.selectByCode("ZWL")).thenReturn(null);
        when(currencyRateMapper.selectPrevious(eq("USD"), eq("ZWL"), any())).thenReturn(null);
        when(currencyRateMapper.selectByPairDate(eq("USD"), eq("ZWL"), any())).thenReturn(null);

        CrmCurrencySyncStatusDto status = currencyService.sync();

        ArgumentCaptor<CrmCurrencyRateEntity> captor = ArgumentCaptor.forClass(CrmCurrencyRateEntity.class);
        verify(currencyRateMapper).insert(captor.capture());
        assertEquals("SINGLE", captor.getValue().getCrossStatus());
        assertEquals(1, status.getSingleCount());
        assertEquals(1, status.getSavedCount());
    }

    @Test
    @DisplayName("主源不可用时比对源接管取值")
    void testSecondaryTakesOverWhenPrimaryFails() {
        when(sourceClient.fetchPrimary()).thenThrow(new IllegalStateException("primary down"));
        when(sourceClient.fetchSecondary()).thenReturn(
                secondary(Map.of("CNY", "6.6908", "EUR", "0.895793"), SYNC_DATE));
        when(currencyMapper.selectByCode(anyString())).thenReturn(null);
        when(currencyRateMapper.selectPrevious(anyString(), anyString(), any())).thenReturn(null);
        when(currencyRateMapper.selectByPairDate(anyString(), anyString(), any())).thenReturn(null);

        CrmCurrencySyncStatusDto status = currencyService.sync();

        ArgumentCaptor<CrmCurrencyRateEntity> captor = ArgumentCaptor.forClass(CrmCurrencyRateEntity.class);
        verify(currencyRateMapper, times(2)).insert(captor.capture());
        assertEquals("OPEN_ER_API", captor.getValue().getSource());
        assertEquals("SINGLE", captor.getValue().getCrossStatus());
        assertEquals("PARTIAL", status.getSyncStatus());
        assertEquals("FAILED", status.getPrimaryStatus());
    }

    @Test
    @DisplayName("主源混入的加密货币（4 位码）不入库")
    void testCryptoCurrencyIsFilteredOut() {
        CurrencyMetadata cny = new CurrencyMetadata();
        cny.setCurrencyCode("CNY");
        cny.setCurrencyName("Chinese Renminbi Yuan");
        when(sourceClient.fetchCurrencyMetadata()).thenReturn(Collections.singletonList(cny));
        when(sourceClient.fetchPrimary()).thenReturn(List.of(
                quote("CNY", "6.6635", SYNC_DATE),
                quote("AAVE", "0.0076287", SYNC_DATE)));
        when(sourceClient.fetchSecondary())
                .thenReturn(secondary(Collections.singletonMap("CNY", "6.6908"), SYNC_DATE));
        when(currencyMapper.selectByCode("CNY")).thenReturn(null);
        when(currencyRateMapper.selectPrevious(eq("USD"), eq("CNY"), any())).thenReturn(null);
        when(currencyRateMapper.selectByPairDate(eq("USD"), eq("CNY"), any())).thenReturn(null);

        CrmCurrencySyncStatusDto status = currencyService.sync();

        ArgumentCaptor<CrmCurrencyRateEntity> captor = ArgumentCaptor.forClass(CrmCurrencyRateEntity.class);
        verify(currencyRateMapper, times(1)).insert(captor.capture());
        assertEquals("CNY", captor.getValue().getQuoteCurrency());
        assertEquals(1, status.getSavedCount());
    }

    @Test
    @DisplayName("币种字典取不到时只接受 3 位码")
    void testFallbackToThreeLetterCodes() {
        when(sourceClient.fetchCurrencyMetadata())
                .thenThrow(new IllegalStateException("dictionary down"));
        when(sourceClient.fetchPrimary()).thenReturn(List.of(
                quote("CNY", "6.6635", SYNC_DATE),
                quote("AAVE", "0.0076287", SYNC_DATE)));
        when(sourceClient.fetchSecondary())
                .thenReturn(secondary(Collections.singletonMap("CNY", "6.6908"), SYNC_DATE));
        when(currencyMapper.selectByCode("CNY")).thenReturn(null);
        when(currencyRateMapper.selectPrevious(eq("USD"), eq("CNY"), any())).thenReturn(null);
        when(currencyRateMapper.selectByPairDate(eq("USD"), eq("CNY"), any())).thenReturn(null);

        CrmCurrencySyncStatusDto status = currencyService.sync();

        ArgumentCaptor<CrmCurrencyRateEntity> captor = ArgumentCaptor.forClass(CrmCurrencyRateEntity.class);
        verify(currencyRateMapper, times(1)).insert(captor.capture());
        assertEquals("CNY", captor.getValue().getQuoteCurrency());
        assertEquals(1, status.getSavedCount());
    }

    @Test
    @DisplayName("两个源都不可用时批次标 FAILED 且不发任何汇率")
    void testBothSourcesDownMarksBatchFailed() {
        when(sourceClient.fetchPrimary()).thenThrow(new IllegalStateException("primary down"));
        when(sourceClient.fetchSecondary()).thenThrow(new IllegalStateException("secondary down"));

        CrmCurrencySyncStatusDto status = currencyService.sync();

        assertEquals("FAILED", status.getSyncStatus());
        verify(currencyRateMapper, never()).insert(any(CrmCurrencyRateEntity.class));
        verify(currencySyncLogMapper).insert(any(CrmCurrencySyncLogEntity.class));
    }

    @Test
    @DisplayName("无可用汇率时折算结果标记 rateMissing")
    void testConvertWithoutRateMarksMissing() {
        when(currencyRateQueryService.convert(any(), anyString(), anyString(), any())).thenReturn(null);

        CrmCurrencyConvertDto dto = currencyService.convert(new BigDecimal("100"), "USD", "EUR", SYNC_DATE);

        assertTrue(dto.isRateMissing());
        assertEquals(null, dto.getConvertedAmount());
    }

    @Test
    @DisplayName("同币种折算直接返回原值")
    void testConvertSameCurrency() {
        CrmCurrencyConvertDto dto = currencyService.convert(new BigDecimal("100"), "usd", "USD", SYNC_DATE);

        assertEquals(0, BigDecimal.ONE.compareTo(dto.getRate()));
        assertEquals(0, new BigDecimal("100.00").compareTo(dto.getConvertedAmount()));
        assertTrue(!dto.isRateMissing());
    }

    @Test
    @DisplayName("手工维护汇率写入 MANUAL 来源")
    void testSaveManualRate() {
        CrmCurrencyRateRequest request = new CrmCurrencyRateRequest();
        request.setQuoteCurrency("cny");
        request.setRate(new BigDecimal("6.7"));
        request.setRateDate(SYNC_DATE);
        when(currencyRateMapper.selectByPairDate("USD", "CNY", SYNC_DATE)).thenReturn(null);
        when(currencyMapper.selectByCode("CNY")).thenReturn(null);

        currencyService.saveManualRate(request);

        ArgumentCaptor<CrmCurrencyRateEntity> captor = ArgumentCaptor.forClass(CrmCurrencyRateEntity.class);
        verify(currencyRateMapper).insert(captor.capture());
        assertEquals("MANUAL", captor.getValue().getSource());
        assertEquals("MANUAL", captor.getValue().getCrossStatus());
        assertEquals("CNY", captor.getValue().getQuoteCurrency());
    }

    @Test
    @DisplayName("手工维护汇率的入参非法时抛业务异常")
    void testSaveManualRateRejectsInvalidInput() {
        CrmCurrencyRateRequest request = new CrmCurrencyRateRequest();
        request.setQuoteCurrency("CNY");
        request.setRate(new BigDecimal("-1"));

        assertThrows(IllegalArgumentException.class, () -> currencyService.saveManualRate(request));
    }

    @Test
    @DisplayName("最近一次同步批次可查询")
    void testSyncStatusReturnsLatestBatch() {
        CrmCurrencySyncLogEntity log = new CrmCurrencySyncLogEntity();
        log.setId("SYNC1");
        log.setSyncStatus("SUCCESS");
        log.setSavedCount(10);
        log.setFinishedAt(LocalDateTime.now());
        when(currencySyncLogMapper.selectLatest()).thenReturn(log);

        CrmCurrencySyncStatusDto status = currencyService.syncStatus();

        assertNotNull(status);
        assertEquals("SYNC1", status.getSyncId());
        assertEquals(10, status.getSavedCount());
        assertTrue(!status.isStale());
    }

}
