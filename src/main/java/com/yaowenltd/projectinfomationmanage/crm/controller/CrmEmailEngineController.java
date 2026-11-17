/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.crm.common.CurrentUserResolver;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailEngineOptionsDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailEngineRewriteRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailTemplateDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailTemplateRequest;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmEmailEngineService;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmEmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * CRM 邮件引擎 HTTP 接口（AI 商务邮件改写）.
 * <p>
 * 改写走 SSE 流式返回；改写结果可一键存为邮件模板，直接进入现有的「邮件管理 → 模板」，
 * 再由邮件群发发送。
 *
 * @since 2026-11-17
 */
@RestController
@RequestMapping("/api/crm/email-engine")
@Tag(name = "CRM Email Engine", description = "AI 商务邮件改写接口")
public class CrmEmailEngineController {

    private final CrmEmailEngineService crmEmailEngineService;
    private final CrmEmailService crmEmailService;
    private final CurrentUserResolver currentUserResolver;

    /**
     * 使用指定的依赖构造 CrmEmailEngineController.
     *
     * @param crmEmailEngineService 邮件引擎服务
     * @param crmEmailService        邮件模板服务
     * @param currentUserResolver    当前用户解析器
     */
    public CrmEmailEngineController(CrmEmailEngineService crmEmailEngineService,
                                    CrmEmailService crmEmailService,
                                    CurrentUserResolver currentUserResolver) {
        this.crmEmailEngineService = crmEmailEngineService;
        this.crmEmailService = crmEmailService;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * AI 改写（流式）.
     * <p>
     * 响应为 {@code text/event-stream}，事件协议见 {@link CrmEmailEngineService}。
     *
     * @param request 改写请求
     * @param http    HTTP 请求（取当前登录用户，用量按人记账）
     * @return SSE 输出
     */
    @PostMapping(value = "/rewrite", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 改写邮件（流式）",
            description = "把基础邮件改写成专业商务邮件，SSE 流式返回 meta/reasoning/delta/done/error 事件")
    public SseEmitter rewrite(@RequestBody CrmEmailEngineRewriteRequest request, HttpServletRequest http) {
        return crmEmailEngineService.rewrite(request, currentUserResolver.currentUsername(http));
    }

    /**
     * 查询可选项（语言 / 语气 / 场景 / 模型）.
     *
     * @return 可选项
     */
    @GetMapping("/options")
    @Operation(summary = "查询改写可选项", description = "返回目标语言、语气、场景与可用模型")
    public ResponseResult<CrmEmailEngineOptionsDto> options() {
        return ResponseResult.success(crmEmailEngineService.options());
    }

    /**
     * 把改写结果保存为邮件模板.
     *
     * @param request 模板内容
     * @return 保存后的模板
     */
    @PostMapping("/templates")
    @Operation(summary = "保存为邮件模板", description = "把改写结果存进 t_email_template，可在邮件管理里群发")
    public ResponseResult<CrmEmailTemplateDto> saveTemplate(@RequestBody CrmEmailTemplateRequest request) {
        return ResponseResult.success(crmEmailService.saveTemplate(request));
    }
}
