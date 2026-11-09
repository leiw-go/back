/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmCurrencyRateCheckEntity;
import java.util.List;

/**
 * CRM 汇率交叉比对明细数据库操作的 Mapper 接口.
 *
 * @since 2026-11-08
 */
@Mapper
public interface CurrencyRateCheckMapper {

    /**
     * 批量写入交叉比对明细.
     *
     * @param checks 明细列表
     * @return 受影响的行数
     *     */
    int insertBatch(@Param("checks") List<CrmCurrencyRateCheckEntity> checks);

    /**
     * 查询某批次的全部比对明细.
     *
     * @param syncId 批次 ID
     * @return 明细列表
     *     */
    List<CrmCurrencyRateCheckEntity> selectBySyncId(@Param("syncId") String syncId);

    /**
     * 分页查询交叉比对明细.
     *
     * @param quoteCurrency 目标币；为空表示不限
     * @param checkResult   比对结果；为空表示不限
     * @return 明细列表
     *     */
    List<CrmCurrencyRateCheckEntity> selectPage(@Param("quoteCurrency") String quoteCurrency,
                                                  @Param("checkResult") String checkResult);

}
