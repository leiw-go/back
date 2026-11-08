/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductPriceDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductPriceRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.product.CrmProductRequest;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import java.util.List;

/**
 * CRM 产品目录服务.
 *
 * @since 2026-11-08
 */
public interface CrmProductService {

    /**
     * 新建产品.
     *
     * @param request 产品信息
     * @return 新建后的产品
     */
    CrmProductDto create(CrmProductRequest request);

    /**
     * 更新产品.
     *
     * @param id      产品 ID
     * @param request 产品信息
     * @return 更新后的产品
     */
    CrmProductDto update(String id, CrmProductRequest request);

    /**
     * 查询产品详情（含多币种参考价）.
     *
     * @param id 产品 ID
     * @return 产品
     */
    CrmProductDto getById(String id);

    /**
     * 逻辑删除产品（同时清理其多币种参考价）.
     *
     * @param id 产品 ID
     */
    void delete(String id);

    /**
     * 分页查询产品.
     *
     * @param query 筛选条件
     * @return 分页结果
     */
    PageResponse<CrmProductDto> list(CrmProductQueryRequest query);

    /**
     * 查询产品的多币种参考价.
     *
     * @param productId 产品 ID
     * @return 参考价列表
     */
    List<CrmProductPriceDto> listPrices(String productId);

    /**
     * 批量替换产品的多币种参考价（整批覆盖语义）.
     *
     * @param productId 产品 ID
     * @param prices    参考价列表；传空列表表示清空
     * @return 替换后的参考价列表
     */
    List<CrmProductPriceDto> replacePrices(String productId, List<CrmProductPriceRequest> prices);

}
