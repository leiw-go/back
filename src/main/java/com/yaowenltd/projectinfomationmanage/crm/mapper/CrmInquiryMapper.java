/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.inquiry.CrmInquiryQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmInquiryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CRM 询盘数据库操作的 Mapper 接口.
 *
 * @since 2026-11-08
 */
@Mapper
public interface CrmInquiryMapper {

    /**
     * 按主键查询未删除的询盘.
     *
     * @param id 询盘 ID
     * @return 询盘；不存在返回 null
     */
    CrmInquiryEntity selectById(@Param("id") String id);

    /**
     * 新增询盘.
     *
     * @param inquiry 询盘实体
     * @return 受影响的行数
     */
    int insert(CrmInquiryEntity inquiry);

    /**
     * 更新询盘.
     *
     * @param inquiry 询盘实体
     * @return 受影响的行数
     */
    int update(CrmInquiryEntity inquiry);

    /**
     * 逻辑删除询盘.
     *
     * @param id        询盘 ID
     * @param deletedAt 删除时刻的毫秒时间戳
     * @return 受影响的行数
     */
    int softDelete(@Param("id") String id, @Param("deletedAt") Long deletedAt);

    /**
     * 更新询盘状态.
     *
     * @param id     询盘 ID
     * @param status 目标状态
     * @return 受影响的行数
     */
    int updateStatus(@Param("id") String id, @Param("status") String status);

    /**
     * 回填首次回复时间（报价单创建时调用）.
     *
     * @param id           询盘 ID
     * @param firstReplyAt 首次回复时间
     * @return 受影响的行数
     */
    int updateFirstReplyAt(@Param("id") String id, @Param("firstReplyAt") LocalDateTime firstReplyAt);

    /**
     * 分页查询询盘.
     *
     * @param query 筛选条件
     * @return 询盘列表
     */
    List<CrmInquiryEntity> selectPage(CrmInquiryQueryRequest query);

}
