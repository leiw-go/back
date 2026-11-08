/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.controller;

import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmAccessMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmDashboardMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmInquiryMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmOpportunityMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmProductMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmQuotationItemMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmQuotationMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmProductPriceMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CurrencyRateMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerContactMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerTagRelMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.MessageMapper;
import com.yaowenltd.projectinfomationmanage.crm.service.CurrencyCrossCheckService;
import com.yaowenltd.projectinfomationmanage.crm.service.CurrencyRateQueryService;
import com.yaowenltd.projectinfomationmanage.crm.service.MessageService;
import com.yaowenltd.projectinfomationmanage.crm.service.OpportunityStageService;
import com.yaowenltd.projectinfomationmanage.crm.service.OrderStatusService;
import com.yaowenltd.projectinfomationmanage.crm.service.QuotationPricingService;
import com.yaowenltd.projectinfomationmanage.crm.service.QuotationStatusService;
import com.yaowenltd.projectinfomationmanage.mapper.LotteryPeriodMapper;
import com.yaowenltd.projectinfomationmanage.mapper.PermissionMapper;
import com.yaowenltd.projectinfomationmanage.mapper.ProductMapper;
import com.yaowenltd.projectinfomationmanage.mapper.RoleMapper;
import com.yaowenltd.projectinfomationmanage.mapper.RolePermissionMapper;
import com.yaowenltd.projectinfomationmanage.mapper.UserMapper;
import com.yaowenltd.projectinfomationmanage.mapper.UserRoleMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * CRM 模块接入后的 Spring 上下文冒烟测试。
 * <p>
 * 目的：确保新增的 CRM Mapper 与 Service 不会破坏应用启动
 * （例如 Mapper 接口被 MyBatis 扫描但缺少 XML 语句导致装配失败）。
 * </p>
 * <p>
 * 不连接 MySQL：显式排除 DataSource 与 MyBatis 自动配置，全部 Mapper 用
 * {@code @MockBean} 占位；Flyway 在测试 profile 已关闭。
 * </p>
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration"
})
class CrmContextSmokeTests {

    @MockBean
    private UserMapper userMapper;

    @MockBean
    private RoleMapper roleMapper;

    @MockBean
    private PermissionMapper permissionMapper;

    @MockBean
    private UserRoleMapper userRoleMapper;

    @MockBean
    private RolePermissionMapper rolePermissionMapper;

    @MockBean
    private ProductMapper productMapper;

    @MockBean
    private LotteryPeriodMapper lotteryPeriodMapper;

    @MockBean
    private MessageMapper messageMapper;

    @MockBean
    private CurrencyRateMapper currencyRateMapper;

    @MockBean
    private CustomerMapper customerMapper;

    @MockBean
    private CustomerContactMapper customerContactMapper;

    @MockBean
    private CustomerTagRelMapper customerTagRelMapper;

    @MockBean
    private CrmAccessMapper crmAccessMapper;

    @MockBean
    private CrmProductMapper crmProductMapper;

    @MockBean
    private CrmProductPriceMapper crmProductPriceMapper;

    @MockBean
    private CrmOpportunityMapper crmOpportunityMapper;

    @MockBean
    private CrmDashboardMapper crmDashboardMapper;

    @MockBean
    private CrmInquiryMapper crmInquiryMapper;

    @MockBean
    private CrmQuotationMapper crmQuotationMapper;

    @MockBean
    private CrmQuotationItemMapper crmQuotationItemMapper;

    @Autowired
    private QuotationPricingService quotationPricingService;

    @Autowired
    private OrderStatusService orderStatusService;

    @Autowired
    private OpportunityStageService opportunityStageService;

    @Autowired
    private QuotationStatusService quotationStatusService;

    @Autowired
    private CurrencyCrossCheckService currencyCrossCheckService;

    @Autowired
    private CurrencyRateQueryService currencyRateQueryService;

    @Autowired
    private MessageService messageService;

    @Test
    @DisplayName("CRM 的 Service 应全部被装配")
    void testCrmServicesAreWired() {
        assertNotNull(quotationPricingService);
        assertNotNull(orderStatusService);
        assertNotNull(opportunityStageService);
        assertNotNull(quotationStatusService);
        assertNotNull(currencyCrossCheckService);
        assertNotNull(currencyRateQueryService);
        assertNotNull(messageService);
    }
}
