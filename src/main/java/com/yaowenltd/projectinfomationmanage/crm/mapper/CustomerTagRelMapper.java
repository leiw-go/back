/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 客户-标签关联数据库操作的 Mapper 接口.
 *
 * @since 2026-11-08
 */
@Mapper
public interface CustomerTagRelMapper {

    /**
     * 物理删除某客户下的全部标签关联（删除客户时清理关联）.
     *
     * @param customerId 客户 ID
     * @return 受影响的行数
     */
    int deleteByCustomerId(@Param("customerId") String customerId);

}
