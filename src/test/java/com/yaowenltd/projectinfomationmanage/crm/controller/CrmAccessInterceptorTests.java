/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.common.GlobalExceptionHandler;
import com.yaowenltd.projectinfomationmanage.crm.config.CrmAccessInterceptor;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link CrmAccessInterceptor} 的单元测试。
 * <p>
 * 契约来自需求文档 §2.2：CRM 路径统一校验 {@code crm:access} 一个权限点；
 * 持有权限放行，不持有返回 403（{@code ForbiddenException}）；非 CRM 路径不受影响。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class CrmAccessInterceptorTests {

    @Mock
    private CrmAccessService crmAccessService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CrmAccessInterceptor interceptor = new CrmAccessInterceptor(crmAccessService);
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addMappedInterceptors(new String[] {"/api/crm/**"}, interceptor)
                .build();
    }

    /** 仅供测试使用的探针控制器：一个 CRM 路径 + 一个非 CRM 路径. */
    @RestController
    static class ProbeController {

        /**
         * CRM 路径下的探针接口.
         *
         * @return 固定字符串
         */
        @GetMapping("/api/crm/probe")
        public String crmProbe() {
            return "ok";
        }

        /**
         * 非 CRM 路径下的探针接口.
         *
         * @return 固定字符串
         */
        @GetMapping("/api/other/probe")
        public String otherProbe() {
            return "ok";
        }
    }

    @Test
    @DisplayName("持有 crm:access 权限时放行")
    void testRequestIsAllowedWithPermission() throws Exception {
        when(crmAccessService.hasCrmAccess("admin")).thenReturn(true);

        mockMvc.perform(get("/api/crm/probe").requestAttr("username", "admin"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("没有 crm:access 权限时返回 403")
    void testRequestIsForbiddenWithoutPermission() throws Exception {
        when(crmAccessService.hasCrmAccess("guest")).thenReturn(false);

        mockMvc.perform(get("/api/crm/probe").requestAttr("username", "guest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("无 CRM 使用权限"));
    }

    @Test
    @DisplayName("没有登录用户时按无权限处理")
    void testAnonymousRequestIsForbidden() throws Exception {
        mockMvc.perform(get("/api/crm/probe"))
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("拦截器只作用于 CRM 路径")
    void testNonCrmPathIsNotBlocked() throws Exception {
        mockMvc.perform(get("/api/other/probe"))
                .andExpect(status().isOk());
    }
}
