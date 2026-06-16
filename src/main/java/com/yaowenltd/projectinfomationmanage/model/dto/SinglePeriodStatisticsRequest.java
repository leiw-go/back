 /*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */
 
 package com.yaowenltd.projectinfomationmanage.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
 
 import java.time.LocalDate;
 
 /**
  * DTO for single period statistics request.
  */
 @Schema(description = "单时间段号码统计请求")
public class SinglePeriodStatisticsRequest {
 
     private LocalDate startDate;
 
     private LocalDate endDate;
 
     public LocalDate getStartDate() {
         return startDate;
     }
 
     public void setStartDate(LocalDate startDate) {
         this.startDate = startDate;
     }
 
     public LocalDate getEndDate() {
         return endDate;
     }
 
     public void setEndDate(LocalDate endDate) {
         this.endDate = endDate;
     }
 }
