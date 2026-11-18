/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailEngineRewriteRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailGenerationDto;
import com.yaowenltd.projectinfomationmanage.crm.service.email.CrmEmailEngineGenerated;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;

/**
 * CRM 邮件生成历史服务.
 * <p>
 * 邮件引擎每成功生成一封就落一份 —— 便于回看生成参数、把上一版拿回来继续改，
 * 以及回溯 token 消耗。
 *
 * @since 2026-11-18
 */
public interface CrmEmailEngineHistoryService {

    /**
     * 保存一次生成记录.
     *
     * @param generated 生成产物
     * @param request   原始请求（提供草稿与生成参数）
     * @param ownerId   生成人
     */
    void save(CrmEmailEngineGenerated generated, CrmEmailEngineRewriteRequest request, String ownerId);

    /**
     * 分页查询生成历史，按生成时间倒排.
     *
     * @param page 页码，从 1 开始
     * @param size 每页条数
     * @return 分页结果
     */
    PageResponse<CrmEmailGenerationDto> list(int page, int size);

    /**
     * 查询一条生成历史.
     *
     * @param id 记录 ID
     * @return 记录；不存在返回 null
     */
    CrmEmailGenerationDto getById(String id);

    /**
     * 删除一条生成历史（逻辑删除）.
     *
     * @param id 记录 ID
     * @return 是否删掉了
     */
    boolean delete(String id);
}
