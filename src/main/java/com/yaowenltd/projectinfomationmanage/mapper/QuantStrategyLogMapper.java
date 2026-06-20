/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.mapper;

import com.yaowenltd.projectinfomationmanage.model.entity.QuantStrategyLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis mapper for QuantStrategyLog.
 */
@Mapper
public interface QuantStrategyLogMapper {

    int insertLog(QuantStrategyLog log);

    int deleteLogsByStrategyId(@Param("strategyId") String strategyId);

    List<QuantStrategyLog> findLogsByStrategyId(@Param("strategyId") String strategyId,
                                                  @Param("limit") Integer limit);
}
