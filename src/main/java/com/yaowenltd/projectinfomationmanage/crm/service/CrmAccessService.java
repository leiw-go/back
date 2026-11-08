/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

/**
 * CRM 访问权限判定服务.
 *
 * @since 2026-11-08
 */
public interface CrmAccessService {

    /**
     * 判断用户是否拥有 CRM 使用权限.
     *
     * @param username 用户名
     * @return 拥有返回 true
     */
    boolean hasCrmAccess(String username);

}
