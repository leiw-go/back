/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import com.yaowenltd.projectinfomationmanage.crm.model.entity.CustomerContactEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 客户联系人数据库操作的 Mapper 接口.
 *
 * @since 2026-11-08
 */
@Mapper
public interface CustomerContactMapper {

    /**
     * 按主键查询未删除的联系人.
     *
     * @param id 联系人 ID
     * @return 联系人；不存在返回 null
     */
    CustomerContactEntity selectById(@Param("id") String id);

    /**
     * 按客户与邮箱查询未删除的联系人.
     *
     * @param customerId 客户 ID
     * @param email      邮箱
     * @return 联系人；不存在返回 null
     */
    CustomerContactEntity selectByEmail(@Param("customerId") String customerId, @Param("email") String email);

    /**
     * 按客户与电话号码查询未删除的联系人.
     *
     * @param customerId 客户 ID
     * @param phone       电话号码（E.164）
     * @return 联系人；不存在返回 null
     */
    CustomerContactEntity selectByPhone(@Param("customerId") String customerId, @Param("phone") String phone);

    /**
     * 按客户查询联系人列表.
     *
     * @param customerId 客户 ID
     * @return 联系人列表
     */
    List<CustomerContactEntity> selectByCustomerId(@Param("customerId") String customerId);

    /**
     * 统计某客户下的联系人数量.
     *
     * @param customerId 客户 ID
     * @return 联系人数量
     */
    long countByCustomerId(@Param("customerId") String customerId);

    /**
     * 新增联系人.
     *
     * @param contact 联系人实体
     * @return 受影响的行数
     */
    int insert(CustomerContactEntity contact);

    /**
     * 更新联系人.
     *
     * @param contact 联系人实体
     * @return 受影响的行数
     */
    int update(CustomerContactEntity contact);

    /**
     * 逻辑删除联系人.
     *
     * @param id        联系人 ID
     * @param deletedAt 删除时刻的毫秒时间戳
     * @return 受影响的行数
     */
    int softDelete(@Param("id") String id, @Param("deletedAt") Long deletedAt);

    /**
     * 物理删除某客户下的全部联系人（删除客户时清理关联）.
     *
     * @param customerId 客户 ID
     * @return 受影响的行数
     */
    int deleteByCustomerId(@Param("customerId") String customerId);

}
