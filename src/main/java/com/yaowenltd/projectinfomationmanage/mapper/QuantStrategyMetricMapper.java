/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.mapper;

import com.yaowenltd.projectinfomationmanage.model.entity.QuantStrategyMetric;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis mapper for QuantStrategyMetric.
 */
@Mapper
public interface QuantStrategyMetricMapper {

    int insertMetric(QuantStrategyMetric metric);

    int deleteMetricsByStrategyId(@Param("strategyId") String strategyId);

    List<QuantStrategyMetric> findMetricsByStrategyId(@Param("strategyId") String strategyId);

    QuantStrategyMetric findLatestMetric(@Param("strategyId") String strategyId);
}
