/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmQuotationItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * CRM 报价明细数据库操作的 Mapper 接口.
 *
 * @since 2026-11-08
 */
@Mapper
public interface CrmQuotationItemMapper {

    /**
     * 查询报价单下的全部明细.
     *
     * @param quotationId 报价单 ID
     * @return 明细列表
     */
    List<CrmQuotationItemEntity> selectByQuotationId(@Param("quotationId") String quotationId);

    /**
     * 新增明细.
     *
     * @param item 明细实体
     * @return 受影响的行数
     */
    int insert(CrmQuotationItemEntity item);

    /**
     * 逻辑删除报价单下的全部明细（重算 / 更新时整批替换）.
     *
     * @param quotationId 报价单 ID
     * @param deletedAt   删除时刻的毫秒时间戳
     * @return 受影响的行数
     */
    int softDeleteByQuotationId(@Param("quotationId") String quotationId, @Param("deletedAt") Long deletedAt);

}
