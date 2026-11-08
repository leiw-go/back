/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.inquiry.CrmInquiryDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.inquiry.CrmInquiryQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.inquiry.CrmInquiryRequest;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;

/**
 * CRM 询盘服务.
 *
 * @since 2026-11-08
 */
public interface CrmInquiryService {

    /**
     * 新建询盘.
     *
     * @param request 询盘信息
     * @return 新建后的询盘
     */
    CrmInquiryDto create(CrmInquiryRequest request);

    /**
     * 更新询盘.
     *
     * @param id      询盘 ID
     * @param request 询盘信息
     * @return 更新后的询盘
     */
    CrmInquiryDto update(String id, CrmInquiryRequest request);

    /**
     * 查询询盘详情（含关联报价单）.
     *
     * @param id 询盘 ID
     * @return 询盘
     */
    CrmInquiryDto getById(String id);

    /**
     * 分页查询询盘.
     *
     * @param query 筛选条件
     * @return 分页结果
     */
    PageResponse<CrmInquiryDto> list(CrmInquiryQueryRequest query);

}
