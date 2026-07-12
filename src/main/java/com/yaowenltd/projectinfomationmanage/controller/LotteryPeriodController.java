/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.model.dto.*;
import com.yaowenltd.projectinfomationmanage.model.dto.PageRequest;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import com.yaowenltd.projectinfomationmanage.service.LotteryPeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 负责大乐透开奖记录管理与统计分析的 HTTP 接口.
 */
@RestController
@RequestMapping("/api/lottery")
@Tag(name = "Lottery Period Management", description = "大乐透开奖记录管理与统计分析")
public class LotteryPeriodController {

    private final LotteryPeriodService lotteryPeriodService;

    /**
     * 构造 LotteryPeriodController.
     *
     * @param lotteryPeriodService 抽奖期数 / 统计相关的业务服务，由 Spring 注入
     */
    public LotteryPeriodController(LotteryPeriodService lotteryPeriodService) {
        this.lotteryPeriodService = lotteryPeriodService;
    }

    /**
     * 新增一条大乐透开奖记录.
     *
     * @param dto 新增的开奖记录数据，前/后区号码、开奖日期等字段会按 DTO 上的 {@code @Valid} 与
     *           {@code @NotNull}/{@code @NotBlank} 注解进行校验
     * @return 包含已持久化的新记录的响应体，HTTP 状态码 201
     */
    @PostMapping("/periods")
    @Operation(summary = "创建开奖记录", description = "新增一条大乐透开奖记录")
    public ResponseResult<LotteryPeriodDto> createPeriod(@Valid @RequestBody LotteryPeriodDto dto) {
        LotteryPeriodDto created = lotteryPeriodService.createLotteryPeriod(dto);
        return ResponseResult.created(created);
    }

    /**
     * 更新指定 ID 的大乐透开奖记录.
     *
     * @param id  待更新记录的 ID，由 URL 路径提供
     * @param dto 待更新的字段（DTO 中的 {@code id} 会被 URL 上的 {@code id} 覆盖）
     * @return 更新后的开奖记录
     */
    @PutMapping("/periods/{id}")
    @Operation(summary = "更新开奖记录", description = "更新指定开奖记录")
    public ResponseResult<LotteryPeriodDto> updatePeriod(@PathVariable String id,
                                                         @Valid @RequestBody LotteryPeriodDto dto) {
        dto.setId(id);
        LotteryPeriodDto updated = lotteryPeriodService.updateLotteryPeriod(dto);
        return ResponseResult.success(updated);
    }

    /**
     * 按 ID 删除一条大乐透开奖记录.
     *
     * @param id 待删除记录的 ID
     * @return 空成功体
     */
    @DeleteMapping("/periods/{id}")
    @Operation(summary = "删除开奖记录", description = "删除指定开奖记录")
    public ResponseResult<Void> deletePeriod(@PathVariable String id) {
        lotteryPeriodService.deleteLotteryPeriod(id);
        return ResponseResult.success();
    }

    /**
     * 按 ID 查询大乐透开奖记录.
     *
     * @param id 待查询记录的 ID
     * @return 对应的开奖记录；记录不存在时由 {@code GlobalExceptionHandler} 统一处理
     */
    @GetMapping("/periods/{id}")
    @Operation(summary = "查询开奖记录", description = "根据ID查询开奖记录")
    public ResponseResult<LotteryPeriodDto> getPeriodById(@PathVariable String id) {
        LotteryPeriodDto dto = lotteryPeriodService.findLotteryPeriodById(id);
        return ResponseResult.success(dto);
    }

    /**
     * 分页查询所有大乐透开奖记录（按开奖日期倒序）.
     *
     * @param page 页码，从 1 开始，默认 1
     * @param size 每页大小，默认 10
     * @return 分页结果，按开奖日期倒序排列
     */
    @GetMapping("/periods")
    @Operation(summary = "查询所有开奖记录", description = "获取所有开奖记录")
    public ResponseResult<PageResponse<LotteryPeriodDto>> getAllPeriods(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        PageRequest pageRequest = new PageRequest(page, size);
        PageResponse<LotteryPeriodDto> result = lotteryPeriodService.findLotteryPeriodsPaginated(pageRequest);
        return ResponseResult.success(result);
    }

    /**
     * 单时间段号码统计.
     * <p>
     * 统计指定时间区间内前区（1-35）与后区（1-12）各号码的出现次数，按出现次数降序排列，
     * 并返回该区间内的总期数，方便在前端做"冷热号"分析.
     * </p>
     *
     * @param startDate 区间起始日期（含），格式 yyyy-MM-dd
     * @param endDate   区间结束日期（含），格式 yyyy-MM-dd
     * @return 单期统计响应体
     */
    @GetMapping("/statistics/single")
    @Operation(summary = "单时间段号码统计", description = "输入时间段，返回该时间段内前区/后区各号码出现次数（按次数降序排列）及总期数")
    public ResponseResult<SinglePeriodStatisticsResponse> getSinglePeriodStatistics(
            @RequestParam("startDate") LocalDate startDate,
            @RequestParam("endDate") LocalDate endDate) {
        SinglePeriodStatisticsResponse response =
                lotteryPeriodService.getSinglePeriodStatistics(startDate, endDate);
        return ResponseResult.success(response);
    }

    /**
     * 多时间段号码统计对比.
     * <p>
     * 输入若干独立的时间段，对比每个号码在所有时间段内的出现次数，并按总次数降序排列，
     * 用于回答"过去 N 期里，这个号码相比上一阶段是变热还是变冷"一类问题.
     * </p>
     *
     * @param request 时间段列表（至少一个），各段时间区间不能重叠
     * @return 多期统计响应体，包含各时间段概要以及前/后区号码统计
     */
    @PostMapping("/statistics/multiple")
    @Operation(summary = "多时间段号码统计对比", description = "输入多个时间段，对比各号码在每个时间段内的出现次数，并按总次数降序排列")
    public ResponseResult<MultiplePeriodStatisticsResponse> getMultiplePeriodStatistics(
            @Valid @RequestBody MultiplePeriodStatisticsRequest request) {
        MultiplePeriodStatisticsResponse response =
                lotteryPeriodService.getMultiplePeriodStatistics(request.getRanges());
        return ResponseResult.success(response);
    }
}
