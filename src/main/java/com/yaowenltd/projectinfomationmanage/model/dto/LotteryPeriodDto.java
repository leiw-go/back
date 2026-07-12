 /*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */
 
 package com.yaowenltd.projectinfomationmanage.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
 
 import jakarta.validation.constraints.NotBlank;
 import jakarta.validation.constraints.NotNull;
 
 import java.time.LocalDate;
 import java.time.LocalDateTime;
 
 /**
  * 彩票期数 CRUD 操作的 DTO.
  */
 @Schema(description = "大乐透开奖记录")
public class LotteryPeriodDto {
 
     @Schema(description = "记录ID")
    private String id;
 
     @NotBlank(message = "期数不能为空")
    @Schema(description = "期号", example = "25001")
     private String period;
 
     @NotNull(message = "开奖日期不能为空")
    @Schema(description = "开奖日期", example = "2025-01-04")
     private LocalDate drawDate;
 
     @NotNull(message = "前区一号不能为空")
    @Schema(description = "前区号码1", example = "5")
    private Integer front1;
 
     @NotNull(message = "前区二号不能为空")
    @Schema(description = "前区号码2", example = "12")
    private Integer front2;
 
     @NotNull(message = "前区三号不能为空")
    @Schema(description = "前区号码3", example = "23")
    private Integer front3;
 
     @NotNull(message = "前区四号不能为空")
    @Schema(description = "前区号码4", example = "28")
    private Integer front4;
 
     @NotNull(message = "前区五号不能为空")
    @Schema(description = "前区号码5", example = "34")
    private Integer front5;
 
     @NotNull(message = "后区一号不能为空")
    @Schema(description = "后区号码1", example = "3")
    private Integer back1;
 
     @NotNull(message = "后区二号不能为空")
    @Schema(description = "后区号码2", example = "10")
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
