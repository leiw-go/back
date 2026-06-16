 /*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */
 
 package com.yaowenltd.projectinfomationmanage.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
 
 import java.util.List;
 import java.util.Map;
 
 /**
  * Response for multi-period statistics comparison.
  * frontAreaStats and backAreaStats are sorted by totalCount descending.
  */
 @Schema(description = "多时间段号码统计对比结果")
public class MultiplePeriodStatisticsResponse {
 
     @Schema(description = "各时间段概要信息")
    private List<PeriodSummary> periods;
 
     @Schema(description = "前区各号码多时间段统计（按总次数降序）")
    private List<MultiPeriodNumberStatistic> frontAreaStats;
 
     @Schema(description = "后区各号码多时间段统计（按总次数降序）")
    private List<MultiPeriodNumberStatistic> backAreaStats;
 
     public List<PeriodSummary> getPeriods() {
         return periods;
     }
 
     public void setPeriods(List<PeriodSummary> periods) {
         this.periods = periods;
     }
 
     public List<MultiPeriodNumberStatistic> getFrontAreaStats() {
         return frontAreaStats;
     }
 
     public void setFrontAreaStats(List<MultiPeriodNumberStatistic> frontAreaStats) {
         this.frontAreaStats = frontAreaStats;
     }
 
     public List<MultiPeriodNumberStatistic> getBackAreaStats() {
         return backAreaStats;
     }
 
     public void setBackAreaStats(List<MultiPeriodNumberStatistic> backAreaStats) {
         this.backAreaStats = backAreaStats;
     }
 
     /**
      * Summary of a single time range period.
      */
     @Schema(description = "时间段概要")
    public static class PeriodSummary {
 
         @Schema(description = "时间段标签", example = "2025上半年")
        private String label;
 
         @Schema(description = "总期数", example = "86")
        private long totalPeriods;
 
         public String getLabel() {
             return label;
         }
 
         public void setLabel(String label) {
             this.label = label;
         }
 
         public long getTotalPeriods() {
             return totalPeriods;
         }
 
         public void setTotalPeriods(long totalPeriods) {
             this.totalPeriods = totalPeriods;
         }
     }
 
     /**
      * A number's statistic across multiple time ranges.
      */
     @Schema(description = "号码多时间段统计")
    public static class MultiPeriodNumberStatistic implements Comparable<MultiPeriodNumberStatistic> {
 
         @Schema(description = "号码", example = "10")
        private String number;
 
         @Schema(description = "各时间段出现次数映射")
        private Map<String, Long> counts;
 
         @Schema(description = "总出现次数", example = "28")
        private long totalCount;
 
         public String getNumber() {
             return number;
         }
 
         public void setNumber(String number) {
             this.number = number;
         }
 
         public Map<String, Long> getCounts() {
             return counts;
         }
 
         public void setCounts(Map<String, Long> counts) {
             this.counts = counts;
         }
 
         public long getTotalCount() {
             return totalCount;
         }
 
         public void setTotalCount(long totalCount) {
             this.totalCount = totalCount;
         }
 
         @Override
         public int compareTo(MultiPeriodNumberStatistic o) {
             return Long.compare(o.totalCount, this.totalCount);
         }
     }
 }
