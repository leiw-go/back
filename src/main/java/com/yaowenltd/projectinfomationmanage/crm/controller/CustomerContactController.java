/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerContactDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerContactRequest;
import com.yaowenltd.projectinfomationmanage.crm.service.CustomerContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * CRM 联系人 HTTP 接口.
 * <p>
 * 端点清单见需求文档 §4.3：新建时校验邮箱唯一 + 电话符合 E.164 + 号码客户内唯一。
 *
 * @since 2026-11-08
 */
@RestController
@RequestMapping("/api/crm")
@Tag(name = "CRM Contact", description = "CRM customer contact APIs")
public class CustomerContactController {

    private final CustomerContactService customerContactService;

    /**
     * 使用指定依赖构造 CustomerContactController.
     *
     * @param customerContactService 联系人服务
     */
    public CustomerContactController(CustomerContactService customerContactService) {
        this.customerContactService = customerContactService;
    }

    /**
     * 某客户下的联系人列表.
     *
     * @param customerId 客户 ID
     * @return 联系人列表
     */
    @GetMapping("/customers/{customerId}/contacts")
    @Operation(summary = "List customer contacts", description = "查询某客户下的联系人列表")
    public ResponseResult<List<CustomerContactDto>> listByCustomer(@PathVariable String customerId) {
        return ResponseResult.success(customerContactService.listByCustomer(customerId));
    }

    /**
     * 新建联系人.
     *
     * @param customerId 所属客户 ID
     * @param request    联系人信息
     * @return 新建后的联系人
     */
    @PostMapping("/customers/{customerId}/contacts")
    @Operation(summary = "Create customer contact", description = "新建联系人，校验邮箱唯一、E.164 电话与客户内号码唯一")
    public ResponseResult<CustomerContactDto> create(@PathVariable String customerId,
                                                     @RequestBody CustomerContactRequest request) {
        return ResponseResult.created(customerContactService.create(customerId, request));
    }

    /**
     * 联系人详情.
     *
     * @param contactId 联系人 ID
     * @return 联系人
     */
    @GetMapping("/contacts/{contactId}")
    @Operation(summary = "Get customer contact", description = "查询联系人详情")
    public ResponseResult<CustomerContactDto> getById(@PathVariable String contactId) {
        return ResponseResult.success(customerContactService.getById(contactId));
    }

    /**
     * 更新联系人.
     *
     * @param contactId 联系人 ID
     * @param request   联系人信息
     * @return 更新后的联系人
     */
    @PutMapping("/contacts/{contactId}")
    @Operation(summary = "Update customer contact", description = "更新联系人")
    public ResponseResult<CustomerContactDto> update(@PathVariable String contactId,
                                                     @RequestBody CustomerContactRequest request) {
        return ResponseResult.success(customerContactService.update(contactId, request));
    }

    /**
     * 删除联系人（逻辑删除）.
     *
     * @param contactId 联系人 ID
     * @return 操作结果
     */
    @DeleteMapping("/contacts/{contactId}")
    @Operation(summary = "Delete customer contact", description = "删除联系人（逻辑删除）")
    public ResponseResult<Void> delete(@PathVariable String contactId) {
        customerContactService.delete(contactId);
        return ResponseResult.success();
    }

}
