 /*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */
 
 package com.yaowenltd.projectinfomationmanage.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
 
 import java.util.List;
 
 /**
  * 单期统计的响应.
  * frontAreaStats 与 backAreaStats 按出现次数降序排列.
  */
 @Schema(description = "单时间段号码统计结果")
public class SinglePeriodStatisticsResponse {
 
     @Schema(description = "总期数", example = "153")
    private long totalPeriods;
 
     @Schema(description = "前区号码统计（按次数降序）")
    private List<NumberStatistic> frontAreaStats;
 
     @Schema(description = "后区号码统计（按次数降序）")
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
