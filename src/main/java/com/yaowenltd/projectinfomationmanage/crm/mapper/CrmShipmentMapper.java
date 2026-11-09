/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmShipmentQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmShipmentEntity;
import java.util.List;

/**
 * CRM 发货单数据库操作的 Mapper 接口.
 *
 * @since 2026-11-08
 */
@Mapper
public interface CrmShipmentMapper {

    /**
     * 按主键查询未删除的发货单.
     *
     * @param id 发货单 ID
     * @return 发货单；不存在返回 null
     *     */
    CrmShipmentEntity selectById(@Param("id") String id);

    /**
     * 查询订单下的全部发货单.
     *
     * @param orderId 订单 ID
     * @return 发货单列表
     *     */
    List<CrmShipmentEntity> selectByOrderId(@Param("orderId") String orderId);

    /**
     * 新增发货单.
     *
     * @param shipment 发货单实体
     * @return 受影响的行数
     *     */
    int insert(CrmShipmentEntity shipment);

    /**
     * 更新发货单.
     *
     * @param shipment 发货单实体
     * @return 受影响的行数
     *     */
    int update(CrmShipmentEntity shipment);

    /**
     * 逻辑删除发货单.
     *
     * @param id        发货单 ID
     * @param deletedAt 删除时刻的毫秒时间戳
     * @return 受影响的行数
     *     */
    int softDelete(@Param("id") String id, @Param("deletedAt") Long deletedAt);

    /**
     * 统计订单下的发货单数量.
     *
     * @param orderId 订单 ID
     * @return 发货单数量
     *     */
    long countByOrderId(@Param("orderId") String orderId);

    /**
     * 分页查询发货单.
     *
     * @param query 筛选条件
     * @return 发货单列表
     *     */
    List<CrmShipmentEntity> selectPage(CrmShipmentQueryRequest query);

}
