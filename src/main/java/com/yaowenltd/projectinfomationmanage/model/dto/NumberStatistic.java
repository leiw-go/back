 /*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */
 
 package com.yaowenltd.projectinfomationmanage.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
 
 /**
  * Statistic result for a single number within one time range.
  * Sorted by count descending.
  */
 @Schema(description = "单号码统计结果")
public class NumberStatistic implements Comparable<NumberStatistic> {
 
     @Schema(description = "号码", example = "10")
    private String number;
 
     @Schema(description = "出现次数", example = "28")
    private long count;
 
     public NumberStatistic() {
     }
 
     public NumberStatistic(String number, long count) {
         this.number = number;
         this.count = count;
     }
 
     public String getNumber() {
         return number;
     }
 
     public void setNumber(String number) {
         this.number = number;
     }
 
     public long getCount() {
         return count;
     }
 
     public void setCount(long count) {
         this.count = count;
     }
 
     @Override
     public int compareTo(NumberStatistic o) {
         return Long.compare(o.count, this.count);
     }
 }
