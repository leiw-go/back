/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yaowenltd.projectinfomationmanage.common.AesUtil;
import com.yaowenltd.projectinfomationmanage.mapper.QuantAccountMapper;
import com.yaowenltd.projectinfomationmanage.mapper.QuantStrategyLogMapper;
import com.yaowenltd.projectinfomationmanage.mapper.QuantStrategyMapper;
import com.yaowenltd.projectinfomationmanage.mapper.QuantStrategyMetricMapper;
import com.yaowenltd.projectinfomationmanage.model.dto.QuantPageResult;
import com.yaowenltd.projectinfomationmanage.model.dto.QuantStrategyDto;
import com.yaowenltd.projectinfomationmanage.model.dto.QuantStrategyQuery;
import com.yaowenltd.projectinfomationmanage.model.entity.QuantAccount;
import com.yaowenltd.projectinfomationmanage.model.entity.QuantStrategy;
import com.yaowenltd.projectinfomationmanage.model.entity.QuantStrategyLog;
import com.yaowenltd.projectinfomationmanage.model.entity.QuantStrategyMetric;
import com.yaowenltd.projectinfomationmanage.service.JoinQuantInvocationException;
import com.yaowenltd.projectinfomationmanage.service.JoinQuantInvoker;
import com.yaowenltd.projectinfomationmanage.service.QuantStrategyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of {@link QuantStrategyService}.
 */
@Service
public class QuantStrategyServiceImpl implements QuantStrategyService {

    private static final Logger log = LoggerFactory.getLogger(QuantStrategyServiceImpl.class);

    private final QuantStrategyMapper strategyMapper;
    private final QuantAccountMapper accountMapper;
    private final QuantStrategyLogMapper logMapper;
    private final QuantStrategyMetricMapper metricMapper;
    private final JoinQuantInvoker joinQuantInvoker;
    private final AesUtil aesUtil;
    private final ObjectMapper mapper = new ObjectMapper();

    public QuantStrategyServiceImpl(QuantStrategyMapper strategyMapper,
                                    QuantAccountMapper accountMapper,
                                    QuantStrategyLogMapper logMapper,
                                    QuantStrategyMetricMapper metricMapper,
                                    JoinQuantInvoker joinQuantInvoker,
                                    AesUtil aesUtil) {
        this.strategyMapper = strategyMapper;
        this.accountMapper = accountMapper;
        this.logMapper = logMapper;
        this.metricMapper = metricMapper;
        this.joinQuantInvoker = joinQuantInvoker;
        this.aesUtil = aesUtil;
    }

    @Override
    public QuantStrategyDto createStrategy(QuantStrategyDto dto) {
        QuantAccount account = accountMapper.findAccountById(dto.getAccountId());
        if (account == null) {
            throw new IllegalArgumentException("account not found: " + dto.getAccountId());
        }
        QuantStrategy strategy = new QuantStrategy();
        strategy.setId(UUID.randomUUID().toString());
        strategy.setAccountId(dto.getAccountId());
        strategy.setName(dto.getName());
        strategy.setDescription(dto.getDescription());
        strategy.setCode(dto.getCode());
        strategy.setParameters(dto.getParameters());
        strategy.setStrategyType(dto.getStrategyType() == null ? "BACKTEST" : dto.getStrategyType());
        strategy.setStatus("DRAFT");
        strategy.setInitialCapital(dto.getInitialCapital() == null ? new BigDecimal("100000.00") : dto.getInitialCapital());
        strategy.setBenchmark(dto.getBenchmark() == null ? "000300.XSHG" : dto.getBenchmark());
        strategy.setStartDate(dto.getStartDate());
        strategy.setEndDate(dto.getEndDate());
        strategy.setFrequency(dto.getFrequency() == null ? "day" : dto.getFrequency());
        LocalDateTime now = LocalDateTime.now();
        strategy.setCreateTime(now);
        strategy.setUpdateTime(now);
        strategyMapper.insertStrategy(strategy);
        return toDto(strategy, account.getAccountName());
    }

    @Override
    public QuantStrategyDto updateStrategy(QuantStrategyDto dto) {
        QuantStrategy current = strategyMapper.findStrategyById(dto.getId());
        if (current == null) {
            throw new IllegalArgumentException("strategy not found: " + dto.getId());
        }
        if (dto.getAccountId() != null) {
            current.setAccountId(dto.getAccountId());
        }
        if (dto.getName() != null) {
            current.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            current.setDescription(dto.getDescription());
        }
        if (dto.getCode() != null) {
            current.setCode(dto.getCode());
        }
        if (dto.getParameters() != null) {
            current.setParameters(dto.getParameters());
        }
        if (dto.getStrategyType() != null) {
            current.setStrategyType(dto.getStrategyType());
        }
        if (dto.getInitialCapital() != null) {
            current.setInitialCapital(dto.getInitialCapital());
        }
        if (dto.getBenchmark() != null) {
            current.setBenchmark(dto.getBenchmark());
        }
        if (dto.getStartDate() != null) {
            current.setStartDate(dto.getStartDate());
        }
        if (dto.getEndDate() != null) {
            current.setEndDate(dto.getEndDate());
        }
        if (dto.getFrequency() != null) {
            current.setFrequency(dto.getFrequency());
        }
        current.setUpdateTime(LocalDateTime.now());
        strategyMapper.updateStrategy(current);
        QuantAccount account = accountMapper.findAccountById(current.getAccountId());
        return toDto(current, account == null ? null : account.getAccountName());
    }

    @Override
    public void deleteStrategy(String id) {
        strategyMapper.deleteStrategyById(id);
        logMapper.deleteLogsByStrategyId(id);
        metricMapper.deleteMetricsByStrategyId(id);
    }

    @Override
    public QuantStrategyDto findStrategyById(String id) {
        QuantStrategy strategy = strategyMapper.findStrategyById(id);
        if (strategy == null) {
            return null;
        }
        QuantAccount account = accountMapper.findAccountById(strategy.getAccountId());
        return toDto(strategy, account == null ? null : account.getAccountName());
    }

    @Override
    public QuantPageResult<QuantStrategyDto> pageStrategies(QuantStrategyQuery query) {
        long total = strategyMapper.countStrategies(toFilterMap(query));
        List<QuantStrategy> records = strategyMapper.findAllStrategies(toFilterMap(query));
        List<QuantStrategyDto> dtos = new ArrayList<>(records.size());
        for (QuantStrategy s : records) {
            QuantAccount account = accountMapper.findAccountById(s.getAccountId());
            dtos.add(toDto(s, account == null ? null : account.getAccountName()));
        }
        long current = query.getCurrent() == null ? 1 : query.getCurrent();
        long pageSize = query.getPageSize() == null ? 10 : query.getPageSize();
        return new QuantPageResult<>(dtos, total, current, pageSize);
    }

    @Override
    public List<QuantStrategyDto> findAllStrategies(QuantStrategyQuery query) {
        return pageStrategies(query).getRecords();
    }

    @Override
    public QuantStrategyDto runStrategy(String id) {
        QuantStrategy strategy = strategyMapper.findStrategyById(id);
        if (strategy == null) {
            throw new IllegalArgumentException("strategy not found: " + id);
        }
        QuantAccount account = accountMapper.findAccountById(strategy.getAccountId());
        if (account == null) {
            throw new IllegalArgumentException("account not found: " + strategy.getAccountId());
        }
        Map<String, Object> params = parseJson(strategy.getParameters());
        try {
            // authenticate first
            String plain = aesUtil.decrypt(account.getPassword());
            joinQuantInvoker.auth(account.getUsername(), plain);
            // submit
            JsonNode resp = joinQuantInvoker.submitStrategy(
                strategy.getCode(),
                params,
                strategy.getStrategyType() == null ? "BACKTEST" : strategy.getStrategyType(),
                strategy.getStartDate() == null ? null : strategy.getStartDate().toString(),
                strategy.getEndDate() == null ? null : strategy.getEndDate().toString(),
                strategy.getInitialCapital() == null ? 100000.0 : strategy.getInitialCapital().doubleValue(),
                strategy.getFrequency(),
                strategy.getBenchmark()
            );
            String runId = resp.path("runId").asText("");
            String status = resp.path("status").asText("SUBMITTED");
            String jqStrategyId = resp.path("jqStrategyId").asText(null);
            strategyMapper.updateJqIds(id, jqStrategyId, runId);
            strategyMapper.updateStatus(id, status, LocalDateTime.now(), null);
            appendLog(id, runId, "INFO", "strategy submitted: " + resp.toString());
        } catch (JoinQuantInvocationException ex) {
            log.warn("submit failed for {}: {}", id, ex.getMessage());
            strategyMapper.updateStatus(id, "ERROR", LocalDateTime.now(), ex.getMessage());
            appendLog(id, null, "ERROR", "submit failed: " + ex.getMessage());
            throw ex;
        }
        return findStrategyById(id);
    }

    @Override
    public QuantStrategyDto stopStrategy(String id) {
        QuantStrategy strategy = strategyMapper.findStrategyById(id);
        if (strategy == null) {
            throw new IllegalArgumentException("strategy not found: " + id);
        }
        if (strategy.getJqBacktestId() == null || strategy.getJqBacktestId().isEmpty()) {
            strategyMapper.updateStatus(id, "STOPPED", LocalDateTime.now(), null);
            return findStrategyById(id);
        }
        try {
            JsonNode resp = joinQuantInvoker.stopStrategy(strategy.getJqBacktestId());
            String status = resp.path("status").asText("STOPPED");
            strategyMapper.updateStatus(id, status, LocalDateTime.now(), null);
            appendLog(id, strategy.getJqBacktestId(), "INFO", "strategy stopped: " + resp.toString());
        } catch (JoinQuantInvocationException ex) {
            strategyMapper.updateStatus(id, "ERROR", LocalDateTime.now(), ex.getMessage());
            appendLog(id, strategy.getJqBacktestId(), "ERROR", "stop failed: " + ex.getMessage());
            throw ex;
        }
        return findStrategyById(id);
    }

    @Override
    public QuantStrategyDto refreshStatus(String id) {
        QuantStrategy strategy = strategyMapper.findStrategyById(id);
        if (strategy == null) {
            throw new IllegalArgumentException("strategy not found: " + id);
        }
        if (strategy.getJqBacktestId() == null || strategy.getJqBacktestId().isEmpty()) {
            return toDto(strategy, null);
        }
        try {
            JsonNode status = joinQuantInvoker.getRunStatus(strategy.getJqBacktestId());
            String statusText = status.path("status").asText(strategy.getStatus());
            strategyMapper.updateStatus(id, statusText, LocalDateTime.now(), null);
            if ("COMPLETED".equalsIgnoreCase(statusText) || "DONE".equalsIgnoreCase(statusText)) {
                persistMetrics(id, strategy.getJqBacktestId());
            }
        } catch (JoinQuantInvocationException ex) {
            log.warn("status refresh failed for {}: {}", id, ex.getMessage());
        }
        return findStrategyById(id);
    }

    @Override
    public JsonNode getAccountInfo(String strategyId) {
        authenticateForStrategy(strategyId);
        return joinQuantInvoker.getAccountInfo();
    }

    @Override
    public JsonNode getPositions(String strategyId) {
        authenticateForStrategy(strategyId);
        return joinQuantInvoker.getPositions(null);
    }

    @Override
    public JsonNode getOrders(String strategyId) {
        authenticateForStrategy(strategyId);
        return joinQuantInvoker.getOrders(null);
    }

    @Override
    public JsonNode getTrades(String strategyId) {
        authenticateForStrategy(strategyId);
        return joinQuantInvoker.getTrades(null);
    }

    // ---- helpers ----
    private void authenticateForStrategy(String strategyId) {
        QuantStrategy strategy = strategyMapper.findStrategyById(strategyId);
        if (strategy == null) {
            throw new IllegalArgumentException("strategy not found: " + strategyId);
        }
        QuantAccount account = accountMapper.findAccountById(strategy.getAccountId());
        if (account == null) {
            throw new IllegalArgumentException("account not found: " + strategy.getAccountId());
        }
        String plain = aesUtil.decrypt(account.getPassword());
        joinQuantInvoker.auth(account.getUsername(), plain);
    }

    private void persistMetrics(String strategyId, String runId) {
        try {
            JsonNode metrics = joinQuantInvoker.getMetrics(runId);
            QuantStrategyMetric m = new QuantStrategyMetric();
            m.setId(UUID.randomUUID().toString());
            m.setStrategyId(strategyId);
            m.setRunId(runId);
            m.setTotalReturn(decimalOrNull(metrics, "totalReturn"));
            m.setAnnualReturn(decimalOrNull(metrics, "annualReturn"));
            m.setSharpeRatio(decimalOrNull(metrics, "sharpeRatio"));
            m.setMaxDrawdown(decimalOrNull(metrics, "maxDrawdown"));
            m.setAlpha(decimalOrNull(metrics, "alpha"));
            m.setBeta(decimalOrNull(metrics, "beta"));
            m.setWinRate(decimalOrNull(metrics, "winRate"));
            m.setVolatility(decimalOrNull(metrics, "volatility"));
            m.setTotalTradeCount(metrics.has("totalTradeCount") ? metrics.get("totalTradeCount").asInt() : null);
            m.setRawJson(metrics.toString());
            m.setCreateTime(LocalDateTime.now());
            metricMapper.insertMetric(m);
            appendLog(strategyId, runId, "INFO", "metrics refreshed");
        } catch (Exception ex) {
            log.warn("metrics refresh failed for run {}: {}", runId, ex.getMessage());
        }
    }

    private BigDecimal decimalOrNull(JsonNode node, String key) {
        JsonNode v = node.get(key);
        if (v == null || v.isNull()) {
            return null;
        }
        try {
            return BigDecimal.valueOf(v.asDouble());
        } catch (Exception ex) {
            return null;
        }
    }

    private void appendLog(String strategyId, String runId, String level, String message) {
        try {
            QuantStrategyLog log = new QuantStrategyLog();
            log.setId(UUID.randomUUID().toString());
            log.setStrategyId(strategyId);
            log.setRunId(runId);
            log.setLevel(level);
            log.setMessage(truncate(message, 4000));
            log.setCreateTime(LocalDateTime.now());
            logMapper.insertLog(log);
        } catch (Exception ex) {
            QuantStrategyServiceImpl.log.warn("failed to persist log: {}", ex.getMessage());
        }
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private Map<String, Object> parseJson(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        try {
            return mapper.readValue(raw, Map.class);
        } catch (Exception ex) {
            log.warn("invalid parameters JSON, treating as empty: {}", ex.getMessage());
            return Map.of();
        }
    }

    private Map<String, Object> toFilterMap(QuantStrategyQuery q) {
        Map<String, Object> map = new HashMap<>();
        if (q == null) {
            return map;
        }
        map.put("name", q.getName());
        map.put("accountId", q.getAccountId());
        map.put("status", q.getStatus());
        map.put("strategyType", q.getStrategyType());
        return map;
    }

    private QuantStrategyDto toDto(QuantStrategy strategy, String accountName) {
        QuantStrategyDto dto = new QuantStrategyDto();
        dto.setId(strategy.getId());
        dto.setAccountId(strategy.getAccountId());
        dto.setAccountName(accountName);
        dto.setName(strategy.getName());
        dto.setDescription(strategy.getDescription());
        dto.setCode(strategy.getCode());
        dto.setParameters(strategy.getParameters());
        dto.setStrategyType(strategy.getStrategyType());
        dto.setStatus(strategy.getStatus());
        dto.setInitialCapital(strategy.getInitialCapital());
        dto.setBenchmark(strategy.getBenchmark());
        dto.setStartDate(strategy.getStartDate());
        dto.setEndDate(strategy.getEndDate());
        dto.setFrequency(strategy.getFrequency());
        dto.setJqStrategyId(strategy.getJqStrategyId());
        dto.setJqBacktestId(strategy.getJqBacktestId());
        dto.setLastRunTime(strategy.getLastRunTime());
        dto.setLastError(strategy.getLastError());
        dto.setCreateTime(strategy.getCreateTime());
        dto.setUpdateTime(strategy.getUpdateTime());
        return dto;
    }
}
