 /*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */
 
 package com.yaowenltd.projectinfomationmanage.model.dto;
 
 /**
  * Statistic result for a single number within one time range.
  * Sorted by count descending.
  */
 public class NumberStatistic implements Comparable<NumberStatistic> {
 
     private String number;
 
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
