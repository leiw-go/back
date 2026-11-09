/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailCampaignDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailCampaignRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailLogDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailPreviewDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailTemplateDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailTemplateRequest;
import java.util.List;
import java.util.Map;

/**
 * CRM 邮件服务.
 * <p>
 * 按需求文档 §3.9：模板 + 群发 + 发送记录。
 * **本期只落库记录**（不接 SMTP），发送接口的行为与将来接 SMTP 后保持一致。
 *
 * @since 2026-11-08
 */
public interface CrmEmailService {

    /**
     * 查询模板列表.
     *
     * @return 模板列表
     */
    List<CrmEmailTemplateDto> listTemplates();

    /**
     * 新建或更新模板（带 id 为更新，不带 id 为新建）.
     *
     * @param request 模板内容
     * @return 保存后的模板
     */
    CrmEmailTemplateDto saveTemplate(CrmEmailTemplateRequest request);

    /**
     * 删除模板.
     *
     * @param id 模板 ID
     */
    void deleteTemplate(String id);

    /**
     * 预览渲染结果（替换 {{xxx}}）.
     *
     * @param id          模板 ID
     * @param placeholders 占位符键值对
     * @return 渲染结果
     */
    CrmEmailPreviewDto preview(String id, Map<String, String> placeholders);

    /**
     * 查询群发任务列表.
     *
     * @param ownerId 创建人；为空表示不限
     * @param status  状态；为空表示不限
     * @return 群发任务列表
     */
    List<CrmEmailCampaignDto> listCampaigns(String ownerId, String status);

    /**
     * 创建群发任务.
     *
     * @param request 群发任务内容
     * @param ownerId 创建人
     * @return 新建后的群发任务
     */
    CrmEmailCampaignDto createCampaign(CrmEmailCampaignRequest request, String ownerId);

    /**
     * 执行发送：为范围内每个有邮箱的联系人写一条发送记录.
     *
     * @param id 群发任务 ID
     * @return 发送后的群发任务
     */
    CrmEmailCampaignDto send(String id);

    /**
     * 查询某群发任务的发送明细.
     *
     * @param id 群发任务 ID
     * @return 发送记录列表
     */
    List<CrmEmailLogDto> listLogs(String id);

}
