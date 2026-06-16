 /*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */
 
 package com.yaowenltd.projectinfomationmanage.model.dto;
 
 import java.util.List;
 import java.util.Map;
 
 /**
  * Response for multi-period statistics comparison.
  * frontAreaStats and backAreaStats are sorted by totalCount descending.
  */
 public class MultiplePeriodStatisticsResponse {
 
     private List<PeriodSummary> periods;
 
     private List<MultiPeriodNumberStatistic> frontAreaStats;
 
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
     public static class PeriodSummary {
 
         private String label;
 
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
     public static class MultiPeriodNumberStatistic implements Comparable<MultiPeriodNumberStatistic> {
 
         private String number;
 
         private Map<String, Long> counts;
 
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
