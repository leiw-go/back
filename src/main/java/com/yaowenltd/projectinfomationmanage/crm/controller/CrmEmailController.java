/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.crm.common.CurrentUserResolver;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailCampaignDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailCampaignRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailLogDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailPreviewDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailTemplateDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailTemplateRequest;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmEmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * CRM 邮件 HTTP 接口.
 *
 * @since 2026-11-08
 */
@RestController
@RequestMapping("/api/crm/email")
@Tag(name = "CRM Email", description = "CRM email template and campaign APIs")
public class CrmEmailController {

    private final CrmEmailService crmEmailService;
    private final CurrentUserResolver currentUserResolver;

    /**
     * 使用指定依赖构造 CrmEmailController.
     *
     * @param crmEmailService      邮件服务
     * @param currentUserResolver 当前用户解析器
     */
    public CrmEmailController(CrmEmailService crmEmailService, CurrentUserResolver currentUserResolver) {
        this.crmEmailService = crmEmailService;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * 模板列表.
     *
     * @return 模板列表
     */
    @GetMapping("/templates")
    @Operation(summary = "List email templates", description = "邮件模板列表")
    public ResponseResult<List<CrmEmailTemplateDto>> listTemplates() {
        return ResponseResult.success(crmEmailService.listTemplates());
    }

    /**
     * 新建 / 更新模板.
     *
     * @param request 模板内容
     * @return 保存后的模板
     */
    @PostMapping("/templates")
    @Operation(summary = "Save email template", description = "新建或更新邮件模板")
    public ResponseResult<CrmEmailTemplateDto> saveTemplate(@RequestBody CrmEmailTemplateRequest request) {
        return ResponseResult.success(crmEmailService.saveTemplate(request));
    }

    /**
     * 删除模板.
     *
     * @param id 模板 ID
     * @return 操作结果
     */
    @DeleteMapping("/templates/{id}")
    @Operation(summary = "Delete email template", description = "删除邮件模板")
    public ResponseResult<Void> deleteTemplate(@PathVariable String id) {
        crmEmailService.deleteTemplate(id);
        return ResponseResult.success();
    }

    /**
     * 预览渲染结果.
     *
     * @param id           模板 ID
     * @param placeholders 占位符键值对（形如 customer.company_name）
     * @return 渲染结果
     */
    @PostMapping("/templates/{id}/preview")
    @Operation(summary = "Preview email template", description = "预览模板渲染结果")
    public ResponseResult<CrmEmailPreviewDto> preview(@PathVariable String id,
                                                      @RequestBody(required = false)
                                                      Map<String, String> placeholders) {
        return ResponseResult.success(crmEmailService.preview(id,
                placeholders == null ? Collections.emptyMap() : placeholders));
    }

    /**
     * 群发任务列表.
     *
     * @param status 状态；为空表示不限
     * @param request HTTP 请求
     * @return 群发任务列表
     */
    @GetMapping("/campaigns")
    @Operation(summary = "List email campaigns", description = "邮件群发任务列表")
    public ResponseResult<List<CrmEmailCampaignDto>> listCampaigns(@RequestParam(required = false) String status,
                                                                    HttpServletRequest request) {
        String ownerId = currentUserResolver.currentUserId(request);
        return ResponseResult.success(crmEmailService.listCampaigns(ownerId, status));
    }

    /**
     * 创建群发任务.
     *
     * @param body    群发任务内容
     * @param request HTTP 请求
     * @return 新建后的群发任务
     */
    @PostMapping("/campaigns")
    @Operation(summary = "Create email campaign", description = "创建邮件群发任务")
    public ResponseResult<CrmEmailCampaignDto> createCampaign(@RequestBody CrmEmailCampaignRequest body,
                                                                HttpServletRequest request) {
        String ownerId = currentUserResolver.currentUserId(request);
        return ResponseResult.created(crmEmailService.createCampaign(body, ownerId));
    }

    /**
     * 执行发送.
     *
     * @param id 群发任务 ID
     * @return 发送后的群发任务
     */
    @PostMapping("/campaigns/{id}/send")
    @Operation(summary = "Send email campaign", description = "执行邮件群发（本期只落库发送记录）")
    public ResponseResult<CrmEmailCampaignDto> send(@PathVariable String id) {
        return ResponseResult.success(crmEmailService.send(id));
    }

    /**
     * 发送明细.
     *
     * @param id 群发任务 ID
     * @return 发送记录列表
     */
    @GetMapping("/campaigns/{id}/logs")
    @Operation(summary = "List email campaign logs", description = "邮件群发发送明细")
    public ResponseResult<List<CrmEmailLogDto>> listLogs(@PathVariable String id) {
        return ResponseResult.success(crmEmailService.listLogs(id));
    }

}
