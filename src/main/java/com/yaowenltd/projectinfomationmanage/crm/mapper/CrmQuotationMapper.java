/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmQuotationEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * CRM 报价单数据库操作的 Mapper 接口.
 *
 * @since 2026-11-08
 */
@Mapper
public interface CrmQuotationMapper {

    /**
     * 按主键查询未删除的报价单.
     *
     * @param id 报价单 ID
     * @return 报价单；不存在返回 null
     */
    CrmQuotationEntity selectById(@Param("id") String id);

    /**
     * 按报价单号查询未删除的报价单.
     *
     * @param quotationNo 报价单号
     * @return 报价单；不存在返回 null
     */
    CrmQuotationEntity selectByNo(@Param("quotationNo") String quotationNo);

    /**
     * 新增报价单.
     *
     * @param quotation 报价单实体
     * @return 受影响的行数
     */
    int insert(CrmQuotationEntity quotation);

    /**
     * 更新报价单.
     *
     * @param quotation 报价单实体
     * @return 受影响的行数
     */
    int update(CrmQuotationEntity quotation);

    /**
     * 逻辑删除报价单.
     *
     * @param id        报价单 ID
     * @param deletedAt 删除时刻的毫秒时间戳
     * @return 受影响的行数
     */
    int softDelete(@Param("id") String id, @Param("deletedAt") Long deletedAt);

    /**
     * 分页查询报价单.
     *
     * @param query 筛选条件
     * @return 报价单列表
     */
    List<CrmQuotationEntity> selectPage(CrmQuotationQueryRequest query);

    /**
     * 查询询盘下的全部报价单（按版本倒序）.
     *
     * @param inquiryId 询盘 ID
     * @return 报价单列表
     */
    List<CrmQuotationEntity> selectByInquiryId(@Param("inquiryId") String inquiryId);

    /**
     * 查询同一询盘 + 币种下的最大版本号.
     *
     * @param inquiryId    询盘 ID
     * @param currencyCode 币种
     * @return 最大版本号；没有则返回 0
     */
    int selectMaxVersion(@Param("inquiryId") String inquiryId, @Param("currencyCode") String currencyCode);

}
