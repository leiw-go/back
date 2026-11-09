/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableAgingDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableWriteOffRequest;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import java.util.List;

/**
 * CRM 应收服务.
 *
 * @since 2026-11-08
 */
public interface CrmReceivableService {

    /**
     * 登记收款（核销）.
     *
     * @param id      应收 ID
     * @param request 核销信息
     * @return 核销后的应收
     */
    CrmReceivableDto writeOff(String id, CrmReceivableWriteOffRequest request);

    /**
     * 查询订单下的全部应收.
     *
     * @param orderId 订单 ID
     * @return 应收列表
     */
    List<CrmReceivableDto> listByOrder(String orderId);

    /**
     * 分页查询应收.
     *
     * @param query 筛选条件
     * @return 分页结果
     */
    PageResponse<CrmReceivableDto> list(CrmReceivableQueryRequest query);

    /**
     * 账龄分析：0-30 / 31-60 / 61-90 / 90+ 天的未回款金额与笔数.
     *
     * @return 账龄分析
     */
    CrmReceivableAgingDto aging();

    /**
     * 订单已交付且关联应收全部收清时，把订单置 COMPLETED.
     * <p>
     * 状态机**不允许跨级**：只有 {@code DELIVERED} 之后的订单才可能转 {@code COMPLETED}；
     * 条件不满足时静默跳过（不影响核销本身）。
     *
     * @param orderId 订单 ID
     */
    void tryCompleteOrder(String orderId);

}
