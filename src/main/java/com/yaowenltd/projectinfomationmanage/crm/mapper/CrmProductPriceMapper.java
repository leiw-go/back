/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmProductPriceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * CRM 产品多币种参考价数据库操作的 Mapper 接口.
 *
 * @since 2026-11-08
 */
@Mapper
public interface CrmProductPriceMapper {

    /**
     * 查询产品下的全部参考价.
     *
     * @param productId 产品 ID
     * @return 参考价列表
     */
    List<CrmProductPriceEntity> selectByProductId(@Param("productId") String productId);

    /**
     * 新增参考价.
     *
     * @param price 参考价实体
     * @return 受影响的行数
     */
    int insert(CrmProductPriceEntity price);

    /**
     * 物理删除产品下的全部参考价.
     * <p>
     * 参考价不被其他表引用，且"整批替换"语义下软删会因同一批次 deleted 时间戳
     * 相同而撞唯一索引，故走物理删除。
     * </p>
     *
     * @param productId 产品 ID
     * @return 受影响的行数
     */
    int deleteByProductId(@Param("productId") String productId);

}
