/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.mapper;

import com.yaowenltd.projectinfomationmanage.model.entity.QuantStrategy;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MyBatis mapper for QuantStrategy.
 */
@Mapper
public interface QuantStrategyMapper {

    int insertStrategy(QuantStrategy strategy);

    int updateStrategy(QuantStrategy strategy);

    int updateStatus(@Param("id") String id,
                     @Param("status") String status,
                     @Param("lastRunTime") LocalDateTime lastRunTime,
                     @Param("lastError") String lastError);

    int updateJqIds(@Param("id") String id,
                    @Param("jqStrategyId") String jqStrategyId,
                    @Param("jqBacktestId") String jqBacktestId);

    int deleteStrategyById(@Param("id") String id);

    QuantStrategy findStrategyById(@Param("id") String id);

    List<QuantStrategy> findAllStrategies(@Param("filter") Map<String, Object> filter);

    long countStrategies(@Param("filter") Map<String, Object> filter);
}
