 /*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */
/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.mapper;

import com.yaowenltd.projectinfomationmanage.model.entity.LotteryPeriod;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Mapper interface for LotteryPeriod database operations.
 */
@Mapper
public interface LotteryPeriodMapper {

    @Insert("INSERT INTO t_lottery_period (id, period, draw_date, front_1, front_2, front_3, front_4, " +
            "front_5, back_1, back_2, create_time, update_time) " +
            "VALUES (#{id}, #{period}, #{drawDate}, #{front1}, #{front2}, #{front3}, #{front4}, " +
            "#{front5}, #{back1}, #{back2}, #{createTime}, #{updateTime})")
    int insertLotteryPeriod(LotteryPeriod lotteryPeriod);

    @Update("<script>UPDATE t_lottery_period " +
            "<set>" +
            "<if test='period != null'>period = #{period},</if>" +
            "<if test='drawDate != null'>draw_date = #{drawDate},</if>" +
            "<if test='front1 != null'>front_1 = #{front1},</if>" +
            "<if test='front2 != null'>front_2 = #{front2},</if>" +
            "<if test='front3 != null'>front_3 = #{front3},</if>" +
            "<if test='front4 != null'>front_4 = #{front4},</if>" +
            "<if test='front5 != null'>front_5 = #{front5},</if>" +
            "<if test='back1 != null'>back_1 = #{back1},</if>" +
            "<if test='back2 != null'>back_2 = #{back2},</if>" +
            "update_time = CURRENT_TIMESTAMP" +
            "</set>" +
            "WHERE id = #{id}</script>")
    int updateLotteryPeriod(LotteryPeriod lotteryPeriod);

    @Delete("DELETE FROM t_lottery_period WHERE id = #{id}")
    int deleteLotteryPeriodById(@Param("id") String id);

    @Select("SELECT id, period, draw_date, front_1, front_2, front_3, front_4, front_5, " +
            "back_1, back_2, create_time, update_time FROM t_lottery_period WHERE id = #{id}")
    @Results(id = "lotteryPeriodMap", value = {
            @Result(column = "id", property = "id"),
            @Result(column = "period", property = "period"),
            @Result(column = "draw_date", property = "drawDate"),
            @Result(column = "front_1", property = "front1"),
            @Result(column = "front_2", property = "front2"),
            @Result(column = "front_3", property = "front3"),
            @Result(column = "front_4", property = "front4"),
            @Result(column = "front_5", property = "front5"),
            @Result(column = "back_1", property = "back1"),
            @Result(column = "back_2", property = "back2"),
            @Result(column = "create_time", property = "createTime"),
            @Result(column = "update_time", property = "updateTime")
    })
    LotteryPeriod findLotteryPeriodById(@Param("id") String id);

    @Select("SELECT id, period, draw_date, front_1, front_2, front_3, front_4, front_5, " +
            "back_1, back_2, create_time, update_time FROM t_lottery_period WHERE period = #{period}")
    @ResultMap("lotteryPeriodMap")
    LotteryPeriod findLotteryPeriodByPeriod(@Param("period") String period);

    @Select("SELECT id, period, draw_date, front_1, front_2, front_3, front_4, front_5, " +
            "back_1, back_2, create_time, update_time FROM t_lottery_period ORDER BY draw_date DESC")
    @ResultMap("lotteryPeriodMap")
    List<LotteryPeriod> findAllLotteryPeriods();

    @Select("SELECT id, period, draw_date, front_1, front_2, front_3, front_4, front_5, " +
            "back_1, back_2, create_time, update_time FROM t_lottery_period " +
            "WHERE draw_date BETWEEN #{startDate} AND #{endDate} ORDER BY draw_date ASC")
    @ResultMap("lotteryPeriodMap")
    List<LotteryPeriod> findLotteryPeriodsByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Select("SELECT COUNT(*) FROM t_lottery_period WHERE draw_date BETWEEN #{startDate} AND #{endDate}")
    long countByDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Select("SELECT COUNT(*) FROM t_lottery_period")
    long countAllLotteryPeriods();

    @Select("SELECT id, period, draw_date, front_1, front_2, front_3, front_4, front_5, " +
            "back_1, back_2, create_time, update_time FROM t_lottery_period " +
            "ORDER BY draw_date DESC LIMIT #{size} OFFSET #{offset}")
    @ResultMap("lotteryPeriodMap")
    List<LotteryPeriod> findAllLotteryPeriodsPaginated(
            @Param("offset") int offset,
            @Param("size") int size);
}
