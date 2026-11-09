/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmCurrencyEntity;
import java.util.List;

/**
 * CRM 币种字典数据库操作的 Mapper 接口.
 *
 * @since 2026-11-08
 */
@Mapper
public interface CurrencyMapper {

    /**
     * 查询全部币种.
     *
     * @return 币种列表
     *     */
    List<CrmCurrencyEntity> selectAll();

    /**
     * 按币种码查询.
     *
     * @param currencyCode 币种码
     * @return 币种；不存在返回 null
     *     */
    CrmCurrencyEntity selectByCode(@Param("currencyCode") String currencyCode);

    /**
     * 新增币种.
     *
     * @param currency 币种实体
     * @return 受影响的行数
     *     */
    int insert(CrmCurrencyEntity currency);

    /**
     * 更新币种.
     *
     * @param currency 币种实体
     * @return 受影响的行数
     *     */
    int update(CrmCurrencyEntity currency);

}
