/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * CRM 权限点查询 Mapper 接口.
 *
 * @since 2026-11-08
 */
@Mapper
public interface CrmAccessMapper {

    /**
     * 统计某用户通过角色拥有的指定权限点数量.
     *
     * @param username       用户名
     * @param permissionCode 权限码
     * @return 命中的关联行数
     */
    int countUserWithPermission(@Param("username") String username,
                                @Param("permissionCode") String permissionCode);

}
