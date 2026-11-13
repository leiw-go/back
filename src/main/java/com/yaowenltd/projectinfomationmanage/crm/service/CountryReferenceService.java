/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.country.CrmCountryInfoDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.country.CrmCountryRefDto;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;

/**
 * CRM 国家参考服务.
 * <p>
 * 输入国家，返回该国的**时区、当前时间、主要货币、对美元与人民币的汇率**。
 * 国家与时区来自 {@code t_country_ref}（参考数据），汇率来自 {@code t_currency_rate}
 * （汇率同步任务写入，基准币 USD）—— 读路径**不实时调用任何外部接口**。
 *
 * @since 2026-11-09
 */
public interface CountryReferenceService {

    /**
     * 分页查询国家（可按国家码 / 英文名 / 中文名模糊搜索）.
     *
     * @param keyword 关键词；为空表示全部
     * @param page    页码，从 1 开始；为空取 1
     * @param size    每页条数；为空取 20，上限 200
     * @return 国家列表
     */
    PageResponse<CrmCountryRefDto> list(String keyword, Integer page, Integer size);

    /**
     * 查询单个国家的完整信息（时区 / 当前时间 / 主要货币 / 对美元与人民币的汇率）.
     *
     * @param country 国家码、英文名或中文名（支持模糊）
     * @return 国家信息
     */
    CrmCountryInfoDto getInfo(String country);

}
