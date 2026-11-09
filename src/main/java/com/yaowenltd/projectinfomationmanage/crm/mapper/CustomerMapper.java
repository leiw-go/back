/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CustomerEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 客户数据库操作的 Mapper 接口.
 *
 * @since 2026-11-08
 */
@Mapper
public interface CustomerMapper {

    /**
     * 按主键查询未删除的客户.
     *
     * @param id 客户 ID
     * @return 客户；不存在返回 null
     */
    CustomerEntity selectById(@Param("id") String id);

    /**
     * 按国家码与归一化名称查询未删除的客户，用于建档查重.
     *
     * @param countryCode     国家码
     * @param companyNameNorm 归一化名称
     * @param excludeId       需要排除的客户 ID（更新时排除自身）；可为空
     * @return 客户；不存在返回 null
     */
    CustomerEntity selectByCompanyNorm(@Param("countryCode") String countryCode,
                                       @Param("companyNameNorm") String companyNameNorm,
                                       @Param("excludeId") String excludeId);

    /**
     * 新增客户.
     *
     * @param customer 客户实体
     * @return 受影响的行数
     */
    int insert(CustomerEntity customer);

    /**
     * 更新客户.
     *
     * @param customer 客户实体
     * @return 受影响的行数
     */
    int update(CustomerEntity customer);

    /**
     * 逻辑删除客户.
     *
     * @param id        客户 ID
     * @param deletedAt 删除时刻的毫秒时间戳
     * @return 受影响的行数
     */
    int softDelete(@Param("id") String id, @Param("deletedAt") Long deletedAt);

    /**
     * 查询某负责人名下的全部客户.
     *
     * @param ownerId 负责人 ID
     * @return 客户列表
     */
    List<CustomerEntity> selectByOwner(@Param("ownerId") String ownerId);

    /**
     * 查询带某标签的全部客户.
     *
     * @param tagId 标签 ID
     * @return 客户列表
     */
    List<CustomerEntity> selectByTagId(@Param("tagId") String tagId);

    /**
     * 分页查询客户.
     *
     * @param query 筛选条件
     * @return 客户列表
     */
    List<CustomerEntity> selectPage(CustomerQueryRequest query);

    /**
     * 查询导出用客户（不分页，上限由 Service 层控制）.
     *
     * @param query 筛选条件
     * @return 客户列表
     */
    List<CustomerEntity> selectForExport(CustomerQueryRequest query);

}
