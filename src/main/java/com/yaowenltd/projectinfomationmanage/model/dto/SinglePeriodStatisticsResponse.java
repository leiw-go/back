 /*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */
 
 package com.yaowenltd.projectinfomationmanage.model.dto;
 
 import java.util.List;
 
 /**
  * Response for single period statistics.
  * frontAreaStats and backAreaStats are sorted by occurrence count descending.
  */
 public class SinglePeriodStatisticsResponse {
 
     private long totalPeriods;
 
     private List<NumberStatistic> frontAreaStats;
 
     private List<NumberStatistic> backAreaStats;
 
     public long getTotalPeriods() {
         return totalPeriods;
     }
 
     public void setTotalPeriods(long totalPeriods) {
         this.totalPeriods = totalPeriods;
     }
 
     public List<NumberStatistic> getFrontAreaStats() {
         return frontAreaStats;
     }
 
     public void setFrontAreaStats(List<NumberStatistic> frontAreaStats) {
         this.frontAreaStats = frontAreaStats;
     }
 
     public List<NumberStatistic> getBackAreaStats() {
         return backAreaStats;
     }
 
     public void setBackAreaStats(List<NumberStatistic> backAreaStats) {
         this.backAreaStats = backAreaStats;
     }
 }
