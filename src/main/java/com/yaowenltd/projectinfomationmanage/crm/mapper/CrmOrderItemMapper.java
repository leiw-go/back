/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmOrderItemEntity;
import java.util.List;

/**
 * CRM 订单明细数据库操作的 Mapper 接口.
 *
 * @since 2026-11-08
 */
@Mapper
public interface CrmOrderItemMapper {

    /**
     * 查询订单下的全部明细.
     *
     * @param orderId 订单 ID
     * @return 明细列表
     *     */
    List<CrmOrderItemEntity> selectByOrderId(@Param("orderId") String orderId);

    /**
     * 新增明细.
     *
     * @param item 明细实体
     * @return 受影响的行数
     *     */
    int insert(CrmOrderItemEntity item);

    /**
     * 逻辑删除订单下的全部明细.
     *
     * @param orderId   订单 ID
     * @param deletedAt 删除时刻的毫秒时间戳
     * @return 受影响的行数
     *     */
    int softDeleteByOrderId(@Param("orderId") String orderId, @Param("deletedAt") Long deletedAt);

}
