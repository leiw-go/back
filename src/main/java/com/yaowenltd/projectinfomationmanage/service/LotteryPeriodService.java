/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

 package com.yaowenltd.projectinfomationmanage.service;

 import com.yaowenltd.projectinfomationmanage.model.dto.LotteryPeriodDto;
 import com.yaowenltd.projectinfomationmanage.model.dto.MultiplePeriodStatisticsRequest;
 import com.yaowenltd.projectinfomationmanage.model.dto.MultiplePeriodStatisticsResponse;
 import com.yaowenltd.projectinfomationmanage.model.dto.SinglePeriodStatisticsResponse;
import com.yaowenltd.projectinfomationmanage.model.dto.PageRequest;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;

 import java.time.LocalDate;

 import java.util.List;

 /**
  * 服务接口，定义彩票期次业务能力.
  */
 public interface LotteryPeriodService {

     LotteryPeriodDto createLotteryPeriod(LotteryPeriodDto dto);

     LotteryPeriodDto updateLotteryPeriod(LotteryPeriodDto dto);

     void deleteLotteryPeriod(String id);

     LotteryPeriodDto findLotteryPeriodById(String id);

     List<LotteryPeriodDto> findAllLotteryPeriods();

    /**
     * 分页查找所有彩票期次.
     * 结果按开奖日期降序排列.
     */
    PageResponse<LotteryPeriodDto> findLotteryPeriodsPaginated(PageRequest pageRequest);

     /**
      * 获取单个时间范围的统计.
      * 前区与后区统计按出现次数降序排列.
      */
     SinglePeriodStatisticsResponse getSinglePeriodStatistics(LocalDate startDate, LocalDate endDate);

     /**
      * 获取多个时间范围的统计并进行对比.
      * 前区与后区统计按总次数降序排列.
      */
     MultiplePeriodStatisticsResponse getMultiplePeriodStatistics(List<MultiplePeriodStatisticsRequest.PeriodRange> ranges);
 }