/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CustomerEntity;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmOpportunityMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerContactMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerTagRelMapper;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.CustomerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CustomerService} 的单元测试。
 * <p>
 * 契约来自需求文档 §3.1、§5.3.1：
 * 公司名归一化（去空格 + 转大写）与国家码组成唯一键，重复建档必须被拦下；
 * 必填项缺失应拒绝；删除客户前必须先清理联系人与标签关联；转交只改负责人。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class CustomerServiceTests {

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private CustomerContactMapper customerContactMapper;

    @Mock
    private CustomerTagRelMapper customerTagRelMapper;

    @Mock
    private CrmOpportunityMapper opportunityMapper;

    private CustomerService customerService;

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(customerMapper, customerContactMapper,
                customerTagRelMapper, opportunityMapper);
    }

    private static CustomerRequest request() {
        CustomerRequest request = new CustomerRequest();
        request.setCompanyName("  Acme   Trading Co., Ltd. ");
        request.setCountryCode("US");
        request.setSource("TRADE_SHOW");
        request.setLevel("B");
        request.setOwnerId("U1");
        return request;
    }

    private static CustomerEntity existing() {
        CustomerEntity entity = new CustomerEntity();
        entity.setId("C1");
        entity.setCompanyName("Acme Trading Co., Ltd.");
        entity.setCompanyNameNorm("ACMETRADINGCO.,LTD.");
        entity.setCountryCode("US");
        entity.setSource("TRADE_SHOW");
        entity.setLevel("B");
        entity.setCustomerStatus("POTENTIAL");
        entity.setOwnerId("U1");
        entity.setDeleted(0L);
        return entity;
    }

    @Test
    @DisplayName("公司名应归一化为去空格并转大写")
    void testCompanyNameIsNormalized() {
        when(customerMapper.selectByCompanyNorm(eq("US"), any(), any())).thenReturn(null);
        when(customerMapper.insert(any())).thenReturn(1);

        customerService.create(request());

        ArgumentCaptor<CustomerEntity> captor = ArgumentCaptor.forClass(CustomerEntity.class);
        verify(customerMapper).insert(captor.capture());
        assertEquals("ACMETRADINGCO.,LTD.", captor.getValue().getCompanyNameNorm(),
                "company_name_norm should strip all blanks and upper-case");
    }

    @Test
    @DisplayName("同一国家下同名客户不允许重复建档")
    void testDuplicateCustomerIsRejected() {
        when(customerMapper.selectByCompanyNorm(eq("US"), eq("ACMETRADINGCO.,LTD."), any()))
                .thenReturn(existing());

        assertThrows(IllegalArgumentException.class, () -> customerService.create(request()));
        verify(customerMapper, never()).insert(any());
    }

    @Test
    @DisplayName("必填项缺失应被拒绝")
    void testMissingRequiredFieldsAreRejected() {
        CustomerRequest noName = request();
        noName.setCompanyName(null);
        assertThrows(IllegalArgumentException.class, () -> customerService.create(noName));

        CustomerRequest noCountry = request();
        noCountry.setCountryCode("  ");
        assertThrows(IllegalArgumentException.class, () -> customerService.create(noCountry));

        CustomerRequest noOwner = request();
        noOwner.setOwnerId(null);
        assertThrows(IllegalArgumentException.class, () -> customerService.create(noOwner));
    }

    @Test
    @DisplayName("新建客户默认处于潜在状态且未删除")
    void testNewCustomerDefaults() {
        when(customerMapper.selectByCompanyNorm(eq("US"), any(), any())).thenReturn(null);
        when(customerMapper.insert(any())).thenReturn(1);

        customerService.create(request());

        ArgumentCaptor<CustomerEntity> captor = ArgumentCaptor.forClass(CustomerEntity.class);
        verify(customerMapper).insert(captor.capture());
        CustomerEntity saved = captor.getValue();
        assertEquals("POTENTIAL", saved.getCustomerStatus());
        assertEquals(0L, saved.getDeleted());
        assertNotNull(saved.getId());
    }

    @Test
    @DisplayName("更新不存在的客户应被拒绝")
    void testUpdateMissingCustomerIsRejected() {
        when(customerMapper.selectById("C404")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> customerService.update("C404", request()));
    }

    @Test
    @DisplayName("更新时改公司名需要重新查重")
    void testUpdateRechecksDuplicate() {
        when(customerMapper.selectById("C1")).thenReturn(existing());
        when(customerMapper.selectByCompanyNorm(eq("US"), eq("ACMETRADINGCO.,LTD."), eq("C1")))
                .thenReturn(null);
        when(customerMapper.update(any())).thenReturn(1);

        customerService.update("C1", request());

        verify(customerMapper).selectByCompanyNorm(eq("US"), eq("ACMETRADINGCO.,LTD."), eq("C1"));
    }

    @Test
    @DisplayName("更新时查到重名客户应被拒绝")
    void testUpdateDuplicateIsRejected() {
        when(customerMapper.selectById("C1")).thenReturn(existing());
        CustomerEntity other = existing();
        other.setId("C2");
        when(customerMapper.selectByCompanyNorm(eq("US"), eq("ACMETRADINGCO.,LTD."), eq("C1")))
                .thenReturn(other);

        assertThrows(IllegalArgumentException.class, () -> customerService.update("C1", request()));
        verify(customerMapper, never()).update(any());
    }

    @Test
    @DisplayName("删除客户前必须先清理联系人与标签关联")
    void testDeleteClearsRelationsFirst() {
        when(customerMapper.selectById("C1")).thenReturn(existing());
        when(opportunityMapper.countByCustomer("C1")).thenReturn(0L);
        when(customerMapper.softDelete(eq("C1"), any())).thenReturn(1);

        customerService.delete("C1");

        verify(customerContactMapper).deleteByCustomerId("C1");
        verify(customerTagRelMapper).deleteByCustomerId("C1");
        verify(customerMapper).softDelete(eq("C1"), any());
    }

    @Test
    @DisplayName("删除不存在的客户应被拒绝")
    void testDeleteMissingCustomerIsRejected() {
        when(customerMapper.selectById("C404")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> customerService.delete("C404"));
        verify(customerMapper, never()).softDelete(any(), any());
    }

    @Test
    @DisplayName("转交负责人只改 owner_id")
    void testTransferChangesOwnerOnly() {
        when(customerMapper.selectById("C1")).thenReturn(existing());
        when(customerMapper.update(any())).thenReturn(1);

        CustomerDto result = customerService.transfer("C1", "U2");

        ArgumentCaptor<CustomerEntity> captor = ArgumentCaptor.forClass(CustomerEntity.class);
        verify(customerMapper).update(captor.capture());
        assertEquals("U2", captor.getValue().getOwnerId());
        assertEquals("C1", captor.getValue().getId(), "transfer must not change the customer id");
        assertEquals("U2", result.getOwnerId());
    }

    @Test
    @DisplayName("转交给空负责人应被拒绝")
    void testTransferToBlankOwnerIsRejected() {
        when(customerMapper.selectById("C1")).thenReturn(existing());

        assertThrows(IllegalArgumentException.class, () -> customerService.transfer("C1", "  "));
        verify(customerMapper, never()).update(any());
    }

    @Test
    @DisplayName("查询详情应返回 DTO")
    void testGetByIdReturnsDto() {
        when(customerMapper.selectById("C1")).thenReturn(existing());
        when(customerContactMapper.countByCustomerId("C1")).thenReturn(3L);
        when(opportunityMapper.countByCustomer("C1")).thenReturn(2L);

        CustomerDto dto = customerService.getById("C1");

        assertEquals("C1", dto.getId());
        assertEquals(3L, dto.getContactCount());
        assertEquals(2L, dto.getOpportunityCount());
        assertEquals("Acme Trading Co., Ltd.", dto.getCompanyName());
    }

    @Test
    @DisplayName("查询不存在的客户应被拒绝")
    void testGetMissingCustomerIsRejected() {
        when(customerMapper.selectById("C404")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> customerService.getById("C404"));
    }

    @Test
    @DisplayName("客户有关联商机时不允许删除")
    void testDeleteRejectedWhenCustomerHasOpportunities() {
        when(customerMapper.selectById("C1")).thenReturn(existing());
        when(opportunityMapper.countByCustomer("C1")).thenReturn(2L);

        IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> customerService.delete("C1"));
        assertTrue(error.getMessage().contains("related opportunities"));
        verify(customerMapper, never()).softDelete(any(), any());
    }

    @Test
    @DisplayName("导出列表应走专用查询并返回全部行")
    void testExportListReturnsAllRows() {
        when(customerMapper.selectForExport(any())).thenReturn(java.util.List.of(existing()));

        java.util.List<CustomerDto> rows = customerService.exportList(new com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerQueryRequest());

        assertEquals(1, rows.size());
        assertEquals("C1", rows.get(0).getId());
        assertTrue(rows.size() <= 10000, "export must not exceed the 10000 row limit");
    }
}
