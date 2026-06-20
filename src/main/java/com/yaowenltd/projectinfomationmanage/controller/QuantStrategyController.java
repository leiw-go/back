/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.model.dto.QuantPageResult;
import com.yaowenltd.projectinfomationmanage.model.dto.QuantStrategyDto;
import com.yaowenltd.projectinfomationmanage.model.dto.QuantStrategyQuery;
import com.yaowenltd.projectinfomationmanage.service.QuantStrategyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for quantitative strategy management.
 */
@RestController
@RequestMapping("/api/quant/strategies")
@Tag(name = "Quant Strategy Management", description = "量化策略管理 API")
public class QuantStrategyController {

    private final QuantStrategyService strategyService;

    public QuantStrategyController(QuantStrategyService strategyService) {
        this.strategyService = strategyService;
    }

    @PostMapping
    @Operation(summary = "Create strategy")
    public ResponseResult<QuantStrategyDto> create(@Valid @RequestBody QuantStrategyDto dto) {
        return ResponseResult.created(strategyService.createStrategy(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update strategy")
    public ResponseResult<QuantStrategyDto> update(@PathVariable String id,
                                                   @Valid @RequestBody QuantStrategyDto dto) {
        dto.setId(id);
        return ResponseResult.success(strategyService.updateStrategy(dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete strategy")
    public ResponseResult<Void> delete(@PathVariable String id) {
        strategyService.deleteStrategy(id);
        return ResponseResult.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get strategy by id")
    public ResponseResult<QuantStrategyDto> get(@PathVariable String id) {
        QuantStrategyDto dto = strategyService.findStrategyById(id);
        if (dto == null) {
            return ResponseResult.notFound("strategy not found");
        }
        return ResponseResult.success(dto);
    }

    @GetMapping
    @Operation(summary = "Page list strategies")
    public ResponseResult<QuantPageResult<QuantStrategyDto>> page(QuantStrategyQuery query) {
        return ResponseResult.success(strategyService.pageStrategies(query));
    }

    @PostMapping("/{id}/run")
    @Operation(summary = "Submit strategy to JoinQuant for backtest / sim / live")
    public ResponseResult<QuantStrategyDto> run(@PathVariable String id) {
        return ResponseResult.success(strategyService.runStrategy(id));
    }

    @PostMapping("/{id}/stop")
    @Operation(summary = "Stop a running strategy")
    public ResponseResult<QuantStrategyDto> stop(@PathVariable String id) {
        return ResponseResult.success(strategyService.stopStrategy(id));
    }

    @PostMapping("/{id}/refresh")
    @Operation(summary = "Refresh status from JoinQuant")
    public ResponseResult<QuantStrategyDto> refresh(@PathVariable String id) {
        return ResponseResult.success(strategyService.refreshStatus(id));
    }

    @GetMapping("/{id}/account")
    @Operation(summary = "Live account info for the strategy's account")
    public ResponseResult<JsonNode> accountInfo(@PathVariable String id) {
        return ResponseResult.success(strategyService.getAccountInfo(id));
    }

    @GetMapping("/{id}/positions")
    @Operation(summary = "Live positions for the strategy")
    public ResponseResult<JsonNode> positions(@PathVariable String id) {
        return ResponseResult.success(strategyService.getPositions(id));
    }

    @GetMapping("/{id}/orders")
    @Operation(summary = "Live orders for the strategy")
    public ResponseResult<JsonNode> orders(@PathVariable String id) {
        return ResponseResult.success(strategyService.getOrders(id));
    }

    @GetMapping("/{id}/trades")
    @Operation(summary = "Live trades for the strategy")
    public ResponseResult<JsonNode> trades(@PathVariable String id) {
        return ResponseResult.success(strategyService.getTrades(id));
    }
}
