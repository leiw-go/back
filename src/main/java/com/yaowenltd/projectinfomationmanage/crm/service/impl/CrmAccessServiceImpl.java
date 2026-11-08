/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmAccessMapper;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmAccessService;
import org.springframework.stereotype.Service;

/**
 * CRM 访问权限判定服务实现.
 *
 * @since 2026-11-08
 */
@Service
public class CrmAccessServiceImpl implements CrmAccessService {

    /** CRM 权限点编码：第一版只做这一个权限点. */
    public static final String CRM_ACCESS_PERMISSION = "crm:access";

    private final CrmAccessMapper crmAccessMapper;

    /**
     * 使用指定的 Mapper 构造 CrmAccessServiceImpl.
     *
     * @param crmAccessMapper CRM 权限 Mapper
     */
    public CrmAccessServiceImpl(CrmAccessMapper crmAccessMapper) {
        this.crmAccessMapper = crmAccessMapper;
    }

    @Override
    public boolean hasCrmAccess(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return crmAccessMapper.countUserWithPermission(username, CRM_ACCESS_PERMISSION) > 0;
    }
}
