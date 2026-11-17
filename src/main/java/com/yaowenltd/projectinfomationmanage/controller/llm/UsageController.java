/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller.llm;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.model.dto.UsageAggregateDto;
import com.yaowenltd.projectinfomationmanage.model.dto.UsageQueryRequest;
import com.yaowenltd.projectinfomationmanage.service.llm.UsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 用量查询接口（{@code GET /design/api/llm/usage}）.
 * <p>
 * ADMIN 可查询任意 user 的用量；普通用户强制只看自己的用量（请求参数里的 {@code userId} 被忽略）。
 * </p>
 *
 * @since 2026-08-04
 */
@RestController
@RequestMapping("/api/llm/usage")
@Tag(name = "LLM Usage Query", description = "LLM 调用用量聚合查询")
public class UsageController {

    private final UsageService usageService;

    public UsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    /**
     * 用量聚合查询.
     *
     * @param userId    指定 user_id（仅 ADMIN 生效；非 ADMIN 强制覆盖为当前用户）
     * @param provider  provider 过滤
     * @param model     model 过滤
     * @param startDate 时间区间起始（含）
     * @param endDate   时间区间结束（含）
     * @param groupBy   聚合维度：user / provider / model / day（默认 user）
     * @param http      HTTP 请求
     * @return 聚合结果列表
     */
    @GetMapping
    @Operation(summary = "用量聚合查询",
            description = "按时间 / provider / model 维度聚合调用量；普通用户强制只看自己")
    public ResponseResult<List<UsageAggregateDto>> queryUsage(
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "provider", required = false) String provider,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "groupBy", required = false, defaultValue = "user") String groupBy,
            HttpServletRequest http) {
        UsageQueryRequest req = new UsageQueryRequest();
        req.setUserId(userId);
        req.setProvider(provider);
        req.setModel(model);
        req.setStartDate(startDate);
        req.setEndDate(endDate);
        req.setGroupBy(groupBy);

        String currentUser = (String) http.getAttribute("username");
        boolean isAdmin = isAdmin(currentUser); // TODO: 接入 RoleController 后替换为真实角色判断
        return ResponseResult.success(usageService.aggregate(req, currentUser, isAdmin));
    }

    /**
     * 简易 ADMIN 判断 —— 当前项目所有非 admin 用户都视作 USER；后续接入 RoleController 后替换.
     */
    private boolean isAdmin(String username) {
        return "admin".equals(username);
    }
}