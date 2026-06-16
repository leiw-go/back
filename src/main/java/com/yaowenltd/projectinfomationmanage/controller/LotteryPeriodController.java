 /*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */
 
 package com.yaowenltd.projectinfomationmanage.controller;
 
 import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
 import com.yaowenltd.projectinfomationmanage.model.dto.*;
 import com.yaowenltd.projectinfomationmanage.service.LotteryPeriodService;
 import io.swagger.v3.oas.annotations.Operation;
 import io.swagger.v3.oas.annotations.tags.Tag;
 import jakarta.validation.Valid;
 import org.springframework.web.bind.annotation.*;
 
 import java.time.LocalDate;
 import java.util.List;
 
 /**
  * Controller for lottery period (大乐透) operations and statistics.
  */
 @RestController
 @RequestMapping("/api/lottery")
 @Tag(name = "Lottery Period Management", description = "大乐透开奖记录管理与统计分析")
 public class LotteryPeriodController {
 
     private final LotteryPeriodService lotteryPeriodService;
 
     public LotteryPeriodController(LotteryPeriodService lotteryPeriodService) {
         this.lotteryPeriodService = lotteryPeriodService;
     }
 
     @PostMapping("/periods")
     @Operation(summary = "创建开奖记录", description = "新增一条大乐透开奖记录")
     public ResponseResult<LotteryPeriodDto> createPeriod(@Valid @RequestBody LotteryPeriodDto dto) {
         LotteryPeriodDto created = lotteryPeriodService.createLotteryPeriod(dto);
         return ResponseResult.created(created);
     }
 
     @PutMapping("/periods/{id}")
     @Operation(summary = "更新开奖记录", description = "更新指定开奖记录")
     public ResponseResult<LotteryPeriodDto> updatePeriod(@PathVariable String id,
                                                          @Valid @RequestBody LotteryPeriodDto dto) {
         dto.setId(id);
         LotteryPeriodDto updated = lotteryPeriodService.updateLotteryPeriod(dto);
         return ResponseResult.success(updated);
     }
 
     @DeleteMapping("/periods/{id}")
     @Operation(summary = "删除开奖记录", description = "删除指定开奖记录")
     public ResponseResult<Void> deletePeriod(@PathVariable String id) {
         lotteryPeriodService.deleteLotteryPeriod(id);
         return ResponseResult.success();
     }
 
     @GetMapping("/periods/{id}")
     @Operation(summary = "查询开奖记录", description = "根据ID查询开奖记录")
     public ResponseResult<LotteryPeriodDto> getPeriodById(@PathVariable String id) {
         LotteryPeriodDto dto = lotteryPeriodService.findLotteryPeriodById(id);
         return ResponseResult.success(dto);
     }
 
     @GetMapping("/periods")
     @Operation(summary = "查询所有开奖记录", description = "获取所有开奖记录")
     public ResponseResult<List<LotteryPeriodDto>> getAllPeriods() {
         List<LotteryPeriodDto> list = lotteryPeriodService.findAllLotteryPeriods();
         return ResponseResult.success(list);
     }
 
     @GetMapping("/statistics/single")
     @Operation(summary = "单时间段号码统计", description = "输入时间段，返回该时间段内前区/后区各号码出现次数（按次数降序排列）及总期数")
     public ResponseResult<SinglePeriodStatisticsResponse> getSinglePeriodStatistics(
             @RequestParam("startDate") LocalDate startDate,
             @RequestParam("endDate") LocalDate endDate) {
         SinglePeriodStatisticsResponse response =
                 lotteryPeriodService.getSinglePeriodStatistics(startDate, endDate);
         return ResponseResult.success(response);
     }
 
     @PostMapping("/statistics/multiple")
     @Operation(summary = "多时间段号码统计对比", description = "输入多个时间段，对比各号码在每个时间段内的出现次数，并按总次数降序排列")
     public ResponseResult<MultiplePeriodStatisticsResponse> getMultiplePeriodStatistics(
             @Valid @RequestBody MultiplePeriodStatisticsRequest request) {
         MultiplePeriodStatisticsResponse response =
                 lotteryPeriodService.getMultiplePeriodStatistics(request.getRanges());
         return ResponseResult.success(response);
     }
 }
