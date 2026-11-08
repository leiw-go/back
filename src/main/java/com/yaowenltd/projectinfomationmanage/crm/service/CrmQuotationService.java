/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationRequest;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;

/**
 * CRM 报价单服务.
 * <p>
 * 按需求文档 §3.5：单价由服务端按成本价 + 加价率 / 折扣率算出（§3.5.1），
 * 前端传来的 {@code unitPrice} / {@code amount} 一律不信任，只有
 * {@code manualPrice = true} 的行才接受前端单价。
 *
 * @since 2026-11-08
 */
public interface CrmQuotationService {

    /**
     * 新建报价单.
     *
     * @param request 报价单信息
     * @return 新建后的报价单
     */
    CrmQuotationDto create(CrmQuotationRequest request);

    /**
     * 基于询盘新建报价单（同询盘 + 同币种下版本号递增）.
     *
     * @param inquiryId 询盘 ID
     * @param request   报价单信息
     * @return 新建后的报价单
     */
    CrmQuotationDto createFromInquiry(String inquiryId, CrmQuotationRequest request);

    /**
     * 更新报价单（仅 DRAFT 可改，改完服务端重算）.
     *
     * @param id      报价单 ID
     * @param request 报价单信息
     * @return 更新后的报价单
     */
    CrmQuotationDto update(String id, CrmQuotationRequest request);

    /**
     * 查询报价单详情（明细 + 金额汇总 + 利润）.
     *
     * @param id 报价单 ID
     * @return 报价单
     */
    CrmQuotationDto getById(String id);

    /**
     * 分页查询报价单.
     *
     * @param query 筛选条件
     * @return 分页结果
     */
    PageResponse<CrmQuotationDto> list(CrmQuotationQueryRequest query);

    /**
     * 试算：只算不落库.
     *
     * @param request 报价单信息
     * @return 算价结果（每行单价 / 金额与单头汇总）
     */
    CrmQuotationDto calculate(CrmQuotationRequest request);

    /**
     * 按当前加价率 / 折扣重算整单（覆盖非手工改价的行）.
     *
     * @param id 报价单 ID
     * @return 重算后的报价单
     */
    CrmQuotationDto recalculate(String id);

    /**
     * 提交发送：DRAFT → SENT.
     *
     * @param id 报价单 ID
     * @return 更新后的报价单
     */
    CrmQuotationDto submit(String id);

    /**
     * 客户确认：SENT → CONFIRMED.
     *
     * @param id 报价单 ID
     * @return 更新后的报价单
     */
    CrmQuotationDto confirm(String id);

    /**
     * 客户拒绝：SENT → REJECTED，必须填原因.
     *
     * @param id           报价单 ID
     * @param rejectReason 拒绝原因
     * @return 更新后的报价单
     */
    CrmQuotationDto reject(String id, String rejectReason);

}
