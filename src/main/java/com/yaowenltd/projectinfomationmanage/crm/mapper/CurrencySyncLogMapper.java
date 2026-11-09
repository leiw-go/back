/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmCurrencySyncLogEntity;

/**
 * CRM 汇率同步批次日志数据库操作的 Mapper 接口.
 *
 * @since 2026-11-08
 */
@Mapper
public interface CurrencySyncLogMapper {

    /**
     * 写入同步批次日志.
     *
     * @param syncLog 批次日志实体
     * @return 受影响的行数
     *     */
    int insert(CrmCurrencySyncLogEntity syncLog);

    /**
     * 查询最近一次同步批次.
     *
     * @return 批次日志；没有则返回 null
     *     */
    CrmCurrencySyncLogEntity selectLatest();

    /**
     * 按批次 ID 查询.
     *
     * @param id 批次 ID
     * @return 批次日志；不存在返回 null
     *     */
    CrmCurrencySyncLogEntity selectById(@Param("id") String id);

}
