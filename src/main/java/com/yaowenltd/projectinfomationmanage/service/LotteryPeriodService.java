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
  * Service interface for lottery period operations.
  */
 public interface LotteryPeriodService {
 
     LotteryPeriodDto createLotteryPeriod(LotteryPeriodDto dto);
 
     LotteryPeriodDto updateLotteryPeriod(LotteryPeriodDto dto);
 
     void deleteLotteryPeriod(String id);
 
     LotteryPeriodDto findLotteryPeriodById(String id);
 
     List<LotteryPeriodDto> findAllLotteryPeriods();

    /**
     * Find all lottery periods with pagination.
     * Results are sorted by drawDate descending.
     */
    PageResponse<LotteryPeriodDto> findLotteryPeriodsPaginated(PageRequest pageRequest);
 
     /**
      * Get statistics for a single time range.
      * frontAreaStats and backAreaStats are sorted by occurrence count descending.
      */
     SinglePeriodStatisticsResponse getSinglePeriodStatistics(LocalDate startDate, LocalDate endDate);
 
     /**
      * Get statistics for multiple time ranges and compare.
      * frontAreaStats and backAreaStats are sorted by total count descending.
      */
     MultiplePeriodStatisticsResponse getMultiplePeriodStatistics(List<MultiplePeriodStatisticsRequest.PeriodRange> ranges);
 }
