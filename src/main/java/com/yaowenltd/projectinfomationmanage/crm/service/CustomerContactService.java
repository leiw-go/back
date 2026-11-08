/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerContactDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerContactRequest;
import java.util.List;

/**
 * 客户联系人服务.
 *
 * @since 2026-11-08
 */
public interface CustomerContactService {

    /**
     * 新建联系人.
     *
     * @param customerId 所属客户 ID
     * @param request    联系人信息
     * @return 新建后的联系人
     */
    CustomerContactDto create(String customerId, CustomerContactRequest request);

    /**
     * 更新联系人.
     *
     * @param contactId 联系人 ID
     * @param request   联系人信息
     * @return 更新后的联系人
     */
    CustomerContactDto update(String contactId, CustomerContactRequest request);

    /**
     * 查询联系人详情.
     *
     * @param contactId 联系人 ID
     * @return 联系人
     */
    CustomerContactDto getById(String contactId);

    /**
     * 逻辑删除联系人.
     *
     * @param contactId 联系人 ID
     */
    void delete(String contactId);

    /**
     * 查询某客户下的联系人列表.
     *
     * @param customerId 客户 ID
     * @return 联系人列表
     */
    List<CustomerContactDto> listByCustomer(String customerId);

}
