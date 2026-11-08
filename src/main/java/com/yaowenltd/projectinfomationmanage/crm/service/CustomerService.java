/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerRequest;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import java.util.List;

/**
 * 客户档案服务.
 *
 * @since 2026-11-08
 */
public interface CustomerService {

    /**
     * 新建客户.
     *
     * @param request 客户信息
     * @return 新建后的客户
     */
    CustomerDto create(CustomerRequest request);

    /**
     * 更新客户.
     *
     * @param id      客户 ID
     * @param request 客户信息
     * @return 更新后的客户
     */
    CustomerDto update(String id, CustomerRequest request);

    /**
     * 查询客户详情.
     *
     * @param id 客户 ID
     * @return 客户
     */
    CustomerDto getById(String id);

    /**
     * 逻辑删除客户（先清理联系人与标签关联）.
     *
     * @param id 客户 ID
     */
    void delete(String id);

    /**
     * 转交负责人.
     *
     * @param id          客户 ID
     * @param newOwnerId 新的负责人 ID
     * @return 转交后的客户
     */
    CustomerDto transfer(String id, String newOwnerId);

    /**
     * 分页查询客户.
     *
     * @param query 筛选条件
     * @return 分页结果
     */
    PageResponse<CustomerDto> list(CustomerQueryRequest query);

    /**
     * 查询导出用客户列表.
     *
     * @param query 筛选条件
     * @return 客户列表（上限 1 万行）
     */
    List<CustomerDto> exportList(CustomerQueryRequest query);

}
