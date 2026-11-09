/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmOrderQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmOrderEntity;
import java.util.List;

/**
 * CRM 订单数据库操作的 Mapper 接口.
 *
 * @since 2026-11-08
 */
@Mapper
public interface CrmOrderMapper {

    /**
     * 按主键查询未删除的订单.
     *
     * @param id 订单 ID
     * @return 订单；不存在返回 null
     *     */
    CrmOrderEntity selectById(@Param("id") String id);

    /**
     * 新增订单.
     *
     * @param order 订单实体
     * @return 受影响的行数
     *     */
    int insert(CrmOrderEntity order);

    /**
     * 更新订单.
     *
     * @param order 订单实体
     * @return 受影响的行数
     *     */
    int update(CrmOrderEntity order);

    /**
     * 逻辑删除订单.
     *
     * @param id        订单 ID
     * @param deletedAt 删除时刻的毫秒时间戳
     * @return 受影响的行数
     *     */
    int softDelete(@Param("id") String id, @Param("deletedAt") Long deletedAt);

    /**
     * 分页查询订单.
     *
     * @param query 筛选条件
     * @return 订单列表
     *     */
    List<CrmOrderEntity> selectPage(CrmOrderQueryRequest query);

}
