/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.yaowenltd.projectinfomationmanage.model.dto.QuantPageResult;
import com.yaowenltd.projectinfomationmanage.model.dto.QuantStrategyDto;
import com.yaowenltd.projectinfomationmanage.model.dto.QuantStrategyQuery;

import java.util.List;

/**
 * Service for managing quantitative trading strategies.
 */
public interface QuantStrategyService {

    QuantStrategyDto createStrategy(QuantStrategyDto dto);

    QuantStrategyDto updateStrategy(QuantStrategyDto dto);

    void deleteStrategy(String id);

    QuantStrategyDto findStrategyById(String id);

    QuantPageResult<QuantStrategyDto> pageStrategies(QuantStrategyQuery query);

    List<QuantStrategyDto> findAllStrategies(QuantStrategyQuery query);

    /**
     * Submit the strategy to JoinQuant for backtest / sim / live. Updates the
     * strategy status and persists the returned run id.
     */
    QuantStrategyDto runStrategy(String id);

    /**
     * Stop a running strategy.
     */
    QuantStrategyDto stopStrategy(String id);

    /**
     * Query the latest run status from JoinQuant and persist metrics.
     */
    QuantStrategyDto refreshStatus(String id);

    /**
     * Live data: account, positions, orders, trades for a strategy's
     * currently-active run.
     */
    JsonNode getAccountInfo(String strategyId);

    JsonNode getPositions(String strategyId);

    JsonNode getOrders(String strategyId);

    JsonNode getTrades(String strategyId);
}
