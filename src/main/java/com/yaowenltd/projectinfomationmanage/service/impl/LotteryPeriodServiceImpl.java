 /*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */
 
 package com.yaowenltd.projectinfomationmanage.service.impl;
 
 import com.yaowenltd.projectinfomationmanage.mapper.LotteryPeriodMapper;
 import com.yaowenltd.projectinfomationmanage.model.dto.*;
 import com.yaowenltd.projectinfomationmanage.model.entity.LotteryPeriod;
 import com.yaowenltd.projectinfomationmanage.service.LotteryPeriodService;
 import org.springframework.stereotype.Service;
 
 import java.math.BigDecimal;
 import java.math.RoundingMode;
 import java.time.LocalDate;
 import java.time.LocalDateTime;
 import java.util.*;
 import java.util.stream.Collectors;
 
 /**
  * Implementation of LotteryPeriodService.
  */
 @Service
 public class LotteryPeriodServiceImpl implements LotteryPeriodService {
 
     private static final int MAX_FRONT_NUMBER = 35;
 
     private static final int MAX_BACK_NUMBER = 12;
 
     private static final int FRONT_NUMBERS_PER_PERIOD = 5;
 
     private static final int BACK_NUMBERS_PER_PERIOD = 2;
 
     private final LotteryPeriodMapper lotteryPeriodMapper;
 
     public LotteryPeriodServiceImpl(LotteryPeriodMapper lotteryPeriodMapper) {
         this.lotteryPeriodMapper = lotteryPeriodMapper;
     }
 
     @Override
     public LotteryPeriodDto createLotteryPeriod(LotteryPeriodDto dto) {
         LotteryPeriod entity = new LotteryPeriod();
         String id = UUID.randomUUID().toString();
         entity.setId(id);
         entity.setPeriod(dto.getPeriod());
         entity.setDrawDate(dto.getDrawDate());
         entity.setFront1(dto.getFront1());
         entity.setFront2(dto.getFront2());
         entity.setFront3(dto.getFront3());
         entity.setFront4(dto.getFront4());
         entity.setFront5(dto.getFront5());
         entity.setBack1(dto.getBack1());
         entity.setBack2(dto.getBack2());
         LocalDateTime now = LocalDateTime.now();
         entity.setCreateTime(now);
         entity.setUpdateTime(now);
 
         lotteryPeriodMapper.insertLotteryPeriod(entity);
 
         dto.setId(id);
         dto.setCreateTime(now);
         dto.setUpdateTime(now);
         return dto;
     }
 
     @Override
     public LotteryPeriodDto updateLotteryPeriod(LotteryPeriodDto dto) {
         LotteryPeriod existing = lotteryPeriodMapper.findLotteryPeriodById(dto.getId());
         if (existing == null) {
             throw new IllegalArgumentException("开奖记录不存在");
         }
 
         LotteryPeriod entity = new LotteryPeriod();
         entity.setId(dto.getId());
         entity.setPeriod(dto.getPeriod());
         entity.setDrawDate(dto.getDrawDate());
         entity.setFront1(dto.getFront1());
         entity.setFront2(dto.getFront2());
         entity.setFront3(dto.getFront3());
         entity.setFront4(dto.getFront4());
         entity.setFront5(dto.getFront5());
         entity.setBack1(dto.getBack1());
         entity.setBack2(dto.getBack2());
 
         lotteryPeriodMapper.updateLotteryPeriod(entity);
         return dto;
     }
 
     @Override
     public void deleteLotteryPeriod(String id) {
         lotteryPeriodMapper.deleteLotteryPeriodById(id);
     }
 
     @Override
     public LotteryPeriodDto findLotteryPeriodById(String id) {
         LotteryPeriod entity = lotteryPeriodMapper.findLotteryPeriodById(id);
         if (entity == null) {
             throw new IllegalArgumentException("开奖记录不存在");
         }
         return convertToDto(entity);
     }
 
     @Override
     public List<LotteryPeriodDto> findAllLotteryPeriods() {
         return lotteryPeriodMapper.findAllLotteryPeriods().stream()
                 .map(this::convertToDto)
                 .collect(Collectors.toList());
     }
 
     @Override
     public SinglePeriodStatisticsResponse getSinglePeriodStatistics(LocalDate startDate, LocalDate endDate) {
         List<LotteryPeriod> periods = lotteryPeriodMapper.findLotteryPeriodsByDateRange(startDate, endDate);
         long totalPeriods = periods.size();
 
         List<NumberStatistic> frontStats = computeNumberStatistics(periods, true);
         List<NumberStatistic> backStats = computeNumberStatistics(periods, false);
 
         Collections.sort(frontStats);
         Collections.sort(backStats);
 
         SinglePeriodStatisticsResponse response = new SinglePeriodStatisticsResponse();
         response.setTotalPeriods(totalPeriods);
         response.setFrontAreaStats(frontStats);
         response.setBackAreaStats(backStats);
         return response;
     }
 
     @Override
     public MultiplePeriodStatisticsResponse getMultiplePeriodStatistics(
             List<MultiplePeriodStatisticsRequest.PeriodRange> ranges) {
 
         List<MultiplePeriodStatisticsResponse.PeriodSummary> periodSummaries = new ArrayList<>();
         Map<String, Long> periodTotalPeriodsMap = new LinkedHashMap<>();
         Map<String, Map<String, Long>> frontCountsByLabel = new LinkedHashMap<>();
         Map<String, Map<String, Long>> backCountsByLabel = new LinkedHashMap<>();
 
         for (MultiplePeriodStatisticsRequest.PeriodRange range : ranges) {
             String label = range.getLabel();
             List<LotteryPeriod> periods = lotteryPeriodMapper.findLotteryPeriodsByDateRange(
                     range.getStartDate(), range.getEndDate());
             long periodsInRange = periods.size();
             periodTotalPeriodsMap.put(label, periodsInRange);
 
             MultiplePeriodStatisticsResponse.PeriodSummary summary =
                     new MultiplePeriodStatisticsResponse.PeriodSummary();
             summary.setLabel(label);
             summary.setTotalPeriods(periodsInRange);
             periodSummaries.add(summary);
 
             Map<Integer, Long> frontFreq = new HashMap<>();
             Map<Integer, Long> backFreq = new HashMap<>();
             for (LotteryPeriod p : periods) {
                 increment(frontFreq, p.getFront1());
                 increment(frontFreq, p.getFront2());
                 increment(frontFreq, p.getFront3());
                 increment(frontFreq, p.getFront4());
                 increment(frontFreq, p.getFront5());
                 increment(backFreq, p.getBack1());
                 increment(backFreq, p.getBack2());
             }
 
             for (int i = 1; i <= MAX_FRONT_NUMBER; i++) {
                 String numStr = formatNumber(i);
                 frontCountsByLabel.computeIfAbsent(numStr, k -> new LinkedHashMap<>())
                         .put(label, frontFreq.getOrDefault(i, 0L));
             }
             for (int i = 1; i <= MAX_BACK_NUMBER; i++) {
                 String numStr = formatNumber(i);
                 backCountsByLabel.computeIfAbsent(numStr, k -> new LinkedHashMap<>())
                         .put(label, backFreq.getOrDefault(i, 0L));
             }
         }
 
         List<MultiplePeriodStatisticsResponse.MultiPeriodNumberStatistic> frontStats =
                 buildMultiPeriodStats(frontCountsByLabel, periodTotalPeriodsMap, FRONT_NUMBERS_PER_PERIOD);
         List<MultiplePeriodStatisticsResponse.MultiPeriodNumberStatistic> backStats =
                 buildMultiPeriodStats(backCountsByLabel, periodTotalPeriodsMap, BACK_NUMBERS_PER_PERIOD);
         Collections.sort(frontStats);
         Collections.sort(backStats);
 
         MultiplePeriodStatisticsResponse response = new MultiplePeriodStatisticsResponse();
         response.setPeriods(periodSummaries);
         response.setFrontAreaStats(frontStats);
         response.setBackAreaStats(backStats);
         return response;
     }
 
     private List<NumberStatistic> computeNumberStatistics(List<LotteryPeriod> periods, boolean isFront) {
         Map<Integer, Long> freq = new HashMap<>();
         for (LotteryPeriod p : periods) {
             if (isFront) {
                 increment(freq, p.getFront1());
                 increment(freq, p.getFront2());
                 increment(freq, p.getFront3());
                 increment(freq, p.getFront4());
                 increment(freq, p.getFront5());
             } else {
                 increment(freq, p.getBack1());
                 increment(freq, p.getBack2());
             }
         }
 
         int maxNumber = isFront ? MAX_FRONT_NUMBER : MAX_BACK_NUMBER;
         List<NumberStatistic> result = new ArrayList<>();
         for (int i = 1; i <= maxNumber; i++) {
             result.add(new NumberStatistic(formatNumber(i), freq.getOrDefault(i, 0L)));
         }
         return result;
     }
 
     private void increment(Map<Integer, Long> map, int key) {
         map.put(key, map.getOrDefault(key, 0L) + 1);
     }
 
     private String formatNumber(int num) {
         return num < 10 ? "0" + num : String.valueOf(num);
     }
 
     private List<MultiplePeriodStatisticsResponse.MultiPeriodNumberStatistic> buildMultiPeriodStats(
             Map<String, Map<String, Long>> countsByLabel,
             Map<String, Long> periodTotalPeriodsMap,
             int numbersPerPeriod) {
         List<MultiplePeriodStatisticsResponse.MultiPeriodNumberStatistic> result = new ArrayList<>();
 
         for (Map.Entry<String, Map<String, Long>> entry : countsByLabel.entrySet()) {
             MultiplePeriodStatisticsResponse.MultiPeriodNumberStatistic stat =
                     new MultiplePeriodStatisticsResponse.MultiPeriodNumberStatistic();
             stat.setNumber(entry.getKey());
             stat.setCounts(entry.getValue());
 
             Map<String, String> probabilities = new LinkedHashMap<>();
             long total = 0L;
 
             for (Map.Entry<String, Long> countEntry : entry.getValue().entrySet()) {
                 String label = countEntry.getKey();
                 long count = countEntry.getValue();
                 long totalPeriods = periodTotalPeriodsMap.getOrDefault(label, 0L);
                 total += count;
 
                 String probability = calculateProbability(count, totalPeriods, numbersPerPeriod);
                 probabilities.put(label, probability);
             }
 
             stat.setProbabilities(probabilities);
             stat.setTotalCount(total);
 
             result.add(stat);
         }
         return result;
     }
 
     /**
      * Calculates the probability as a percentage string with 2 decimal places rounded.
      *
      * @param appearances        number of times the number appeared in the period
      * @param totalPeriods       total number of periods in the time range
      * @param numbersPerPeriod  number of numbers drawn per period
      * @return probability as percentage string with % suffix, e.g., "5.60%"
      */
     private String calculateProbability(long appearances, long totalPeriods, int numbersPerPeriod) {
         long totalPossibleDraws = totalPeriods * numbersPerPeriod;
         if (totalPossibleDraws <= 0) {
             return "0.00%";
         }
         BigDecimal probability = BigDecimal.valueOf(appearances)
                 .multiply(BigDecimal.valueOf(100))
                 .divide(BigDecimal.valueOf(totalPossibleDraws), 2, RoundingMode.HALF_UP);
         return probability.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
     }
 
     private LotteryPeriodDto convertToDto(LotteryPeriod entity) {
         LotteryPeriodDto dto = new LotteryPeriodDto();
         dto.setId(entity.getId());
         dto.setPeriod(entity.getPeriod());
         dto.setDrawDate(entity.getDrawDate());
         dto.setFront1(entity.getFront1());
         dto.setFront2(entity.getFront2());
         dto.setFront3(entity.getFront3());
         dto.setFront4(entity.getFront4());
         dto.setFront5(entity.getFront5());
         dto.setBack1(entity.getBack1());
         dto.setBack2(entity.getBack2());
         dto.setCreateTime(entity.getCreateTime());
         dto.setUpdateTime(entity.getUpdateTime());
         return dto;
     }
 }
