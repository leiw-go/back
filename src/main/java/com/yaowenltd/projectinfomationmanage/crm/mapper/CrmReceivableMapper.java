/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableAgingBucket;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmReceivableEntity;
import java.time.LocalDate;
import java.util.List;

/**
 * CRM 应收数据库操作的 Mapper 接口.
 *
 * @since 2026-11-08
 */
@Mapper
public interface CrmReceivableMapper {

    /**
     * 按主键查询未删除的应收.
     *
     * @param id 应收 ID
     * @return 应收；不存在返回 null
     *     */
    CrmReceivableEntity selectById(@Param("id") String id);

    /**
     * 查询订单下的全部应收.
     *
     * @param orderId 订单 ID
     * @return 应收列表
     *     */
    List<CrmReceivableEntity> selectByOrderId(@Param("orderId") String orderId);

    /**
     * 新增应收.
     *
     * @param receivable 应收实体
     * @return 受影响的行数
     *     */
    int insert(CrmReceivableEntity receivable);

    /**
     * 更新应收.
     *
     * @param receivable 应收实体
     * @return 受影响的行数
     *     */
    int update(CrmReceivableEntity receivable);

    /**
     * 分页查询应收.
     *
     * @param query 筛选条件
     * @return 应收列表
     *     */
    List<CrmReceivableEntity> selectPage(CrmReceivableQueryRequest query);

    /**
     * 账龄分析：按逾期天数分桶统计未回款金额与笔数.
     *
     * @return 账龄分桶列表
     *     */
    List<CrmReceivableAgingBucket> selectAging();

    /**
     * 查询已过到期日且尚未收齐的应收（定时逾期扫描用）.
     *
     * @param today 判定基准日（当天）
     * @return 逾期应收列表
     */
    List<CrmReceivableEntity> selectOverdueList(@Param("today") LocalDate today);

}
