 /*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */
 
 package com.yaowenltd.projectinfomationmanage.model.dto;
 
 import jakarta.validation.Valid;
 import jakarta.validation.constraints.NotBlank;
 import jakarta.validation.constraints.NotEmpty;
 import jakarta.validation.constraints.NotNull;
 
 import java.time.LocalDate;
 import java.util.List;
 
 /**
  * Request for multi-period statistics comparison.
  */
 public class MultiplePeriodStatisticsRequest {
 
     @NotEmpty(message = "时间段列表不能为空")
     @Valid
     private List<PeriodRange> ranges;
 
     public List<PeriodRange> getRanges() {
         return ranges;
     }
 
     public void setRanges(List<PeriodRange> ranges) {
         this.ranges = ranges;
     }
 
     /**
      * A single time range with a label.
      */
     public static class PeriodRange {
 
         @NotBlank(message = "时间段标签不能为空")
         private String label;
 
         @NotNull(message = "开始日期不能为空")
         private LocalDate startDate;
 
         @NotNull(message = "结束日期不能为空")
         private LocalDate endDate;
 
         public String getLabel() {
             return label;
         }
 
         public void setLabel(String label) {
             this.label = label;
         }
 
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
 }
