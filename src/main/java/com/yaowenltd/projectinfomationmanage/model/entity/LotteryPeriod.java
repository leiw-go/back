 /*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */
 
 package com.yaowenltd.projectinfomationmanage.model.entity;
 
 import java.time.LocalDate;
 import java.time.LocalDateTime;
 
 /**
  * Entity representing a lottery period (大乐透开奖记录).
  */
 public class LotteryPeriod {
 
     private String id;
 
     private String period;
 
     private LocalDate drawDate;
 
     private Integer front1;
 
     private Integer front2;
 
     private Integer front3;
 
     private Integer front4;
 
     private Integer front5;
 
     private Integer back1;
 
     private Integer back2;
 
     private LocalDateTime createTime;
 
     private LocalDateTime updateTime;
 
     public String getId() {
         return id;
     }
 
     public void setId(String id) {
         this.id = id;
     }
 
     public String getPeriod() {
         return period;
     }
 
     public void setPeriod(String period) {
         this.period = period;
     }
 
     public LocalDate getDrawDate() {
         return drawDate;
     }
 
     public void setDrawDate(LocalDate drawDate) {
         this.drawDate = drawDate;
     }
 
     public Integer getFront1() {
         return front1;
     }
 
     public void setFront1(Integer front1) {
         this.front1 = front1;
     }
 
     public Integer getFront2() {
         return front2;
     }
 
     public void setFront2(Integer front2) {
         this.front2 = front2;
     }
 
     public Integer getFront3() {
         return front3;
     }
 
     public void setFront3(Integer front3) {
         this.front3 = front3;
     }
 
     public Integer getFront4() {
         return front4;
     }
 
     public void setFront4(Integer front4) {
         this.front4 = front4;
     }
 
     public Integer getFront5() {
         return front5;
     }
 
     public void setFront5(Integer front5) {
         this.front5 = front5;
     }
 
     public Integer getBack1() {
         return back1;
     }
 
     public void setBack1(Integer back1) {
         this.back1 = back1;
     }
 
     public Integer getBack2() {
         return back2;
     }
 
     public void setBack2(Integer back2) {
         this.back2 = back2;
     }
 
     public LocalDateTime getCreateTime() {
         return createTime;
     }
 
     public void setCreateTime(LocalDateTime createTime) {
         this.createTime = createTime;
     }
 
     public LocalDateTime getUpdateTime() {
         return updateTime;
     }
 
     public void setUpdateTime(LocalDateTime updateTime) {
         this.updateTime = updateTime;
     }
 }
