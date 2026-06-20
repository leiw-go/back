/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for quantitative trading strategy.
 */
@Schema(description = "量化策略")
public class QuantStrategyDto {

    @Schema(description = "策略ID")
    private String id;

    @NotBlank(message = "accountId cannot be empty")
    @Schema(description = "所属聚宽账户ID")
    private String accountId;

    private String accountName;

    @NotBlank(message = "strategy name cannot be empty")
    @Schema(description = "策略名称", example = "双均线策略")
    @Size(max = 128, message = "strategy name length must not exceed 128")
    private String name;

    @Schema(description = "策略描述", example = "基于 5 日与 20 日均线的趋势跟踪")
    @Size(max = 512, message = "description length must not exceed 512")
    private String description;

    @Schema(description = "策略 Python 源码")
    private String code;

    @Schema(description = "JSON 格式的策略参数")
    private String parameters;

    @Schema(description = "策略类型: BACKTEST=回测, SIM=模拟, LIVE=实盘", example = "BACKTEST")
    @Size(max = 16, message = "strategy type length must not exceed 16")
    private String strategyType;

    @Schema(description = "状态: DRAFT/RUNNING/STOPPED/ERROR/COMPLETED", example = "DRAFT")
    @Size(max = 16, message = "status length must not exceed 16")
    private String status;

    @Schema(description = "初始资金", example = "100000.00")
    private BigDecimal initialCapital;

    @Schema(description = "基准指数", example = "000300.XSHG")
    @Size(max = 32, message = "benchmark length must not exceed 32")
    private String benchmark;

    @Schema(description = "回测/运行开始日期", example = "2024-01-01")
    private LocalDate startDate;

    @Schema(description = "回测结束日期", example = "2025-01-01")
    private LocalDate endDate;

    @Schema(description = "运行频率: day/minute/5m/15m/30m/60m", example = "day")
    @Size(max = 8, message = "frequency length must not exceed 8")
    private String frequency;

    @Schema(description = "聚宽平台返回的策略ID")
    private String jqStrategyId;

    @Schema(description = "聚宽平台返回的回测ID")
    private String jqBacktestId;

    private LocalDateTime lastRunTime;

    private String lastError;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getParameters() {
        return parameters;
    }

    public void setParameters(String parameters) {
        this.parameters = parameters;
    }

    public String getStrategyType() {
        return strategyType;
    }

    public void setStrategyType(String strategyType) {
        this.strategyType = strategyType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getInitialCapital() {
        return initialCapital;
    }

    public void setInitialCapital(BigDecimal initialCapital) {
        this.initialCapital = initialCapital;
    }

    public String getBenchmark() {
        return benchmark;
    }

    public void setBenchmark(String benchmark) {
        this.benchmark = benchmark;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getJqStrategyId() {
        return jqStrategyId;
    }

    public void setJqStrategyId(String jqStrategyId) {
        this.jqStrategyId = jqStrategyId;
    }

    public String getJqBacktestId() {
        return jqBacktestId;
    }

    public void setJqBacktestId(String jqBacktestId) {
        this.jqBacktestId = jqBacktestId;
    }

    public LocalDateTime getLastRunTime() {
        return lastRunTime;
    }

    public void setLastRunTime(LocalDateTime lastRunTime) {
        this.lastRunTime = lastRunTime;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
