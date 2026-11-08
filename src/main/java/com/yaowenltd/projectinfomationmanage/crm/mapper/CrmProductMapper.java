/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmProductEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * CRM 产品数据库操作的 Mapper 接口.
 *
 * @since 2026-11-08
 */
@Mapper
public interface CrmProductMapper {

    /**
     * 按主键查询未删除的产品.
     *
     * @param id 产品 ID
     * @return 产品；不存在返回 null
     */
    CrmProductEntity selectById(@Param("id") String id);

    /**
     * 按产品编码查询未删除的产品，用于查重.
     *
     * @param productCode 产品编码
     * @param excludeId   需要排除的产品 ID（更新时排除自身）；可为空
     * @return 产品；不存在返回 null
     */
    CrmProductEntity selectByCode(@Param("productCode") String productCode,
                                   @Param("excludeId") String excludeId);

    /**
     * 新增产品.
     *
     * @param product 产品实体
     * @return 受影响的行数
     */
    int insert(CrmProductEntity product);

    /**
     * 更新产品.
     *
     * @param product 产品实体
     * @return 受影响的行数
     */
    int update(CrmProductEntity product);

    /**
     * 逻辑删除产品.
     *
     * @param id        产品 ID
     * @param deletedAt 删除时刻的毫秒时间戳
     * @return 受影响的行数
     */
    int softDelete(@Param("id") String id, @Param("deletedAt") Long deletedAt);

    /**
     * 分页查询产品.
     *
     * @param query 筛选条件
     * @return 产品列表
     */
    List<CrmProductEntity> selectPage(CrmProductQueryRequest query);

}
