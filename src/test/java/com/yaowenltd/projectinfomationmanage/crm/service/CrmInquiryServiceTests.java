/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmInquiryMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmQuotationMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerContactMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.inquiry.CrmInquiryDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.inquiry.CrmInquiryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmInquiryEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmQuotationEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CustomerContactEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CustomerEntity;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.CrmInquiryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CrmInquiryService} 的单元测试。
 * <p>
 * 契约来自需求文档 §3.4、§5.4：客户必须存在；联系人必须存在**且属于该客户**；
 * 目标价只作参考不参与定价；"超时未报价"= 创建超 24 小时 + 没回过价 + 名下无报价单。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class CrmInquiryServiceTests {

    @Mock
    private CrmInquiryMapper inquiryMapper;

    @Mock
    private CrmQuotationMapper quotationMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private CustomerContactMapper customerContactMapper;

    private CrmInquiryService crmInquiryService;

    @BeforeEach
    void setUp() {
        crmInquiryService = new CrmInquiryServiceImpl(inquiryMapper, quotationMapper, customerMapper,
                customerContactMapper);
    }

    private static CrmInquiryRequest request() {
        CrmInquiryRequest request = new CrmInquiryRequest();
        request.setCustomerId("C1");
        request.setSource("EMAIL");
        request.setOwnerId("U1");
        request.setSubject("询价 2L 水壶");
        request.setTargetPrice(new BigDecimal("12.00"));
        request.setQuantity(new BigDecimal("500"));
        return request;
    }

    private static CustomerEntity customer() {
        CustomerEntity entity = new CustomerEntity();
        entity.setId("C1");
        entity.setCompanyName("Acme Trading Co., Ltd.");
        entity.setCountryCode("US");
        entity.setSource("TRADE_SHOW");
        entity.setLevel("B");
        entity.setOwnerId("U1");
        entity.setDeleted(0L);
        return entity;
    }

    private static CustomerContactEntity contact(String customerId) {
        CustomerContactEntity entity = new CustomerContactEntity();
        entity.setId("CT1");
        entity.setCustomerId(customerId);
        entity.setName("John Smith");
        entity.setEmail("john@acme.com");
        entity.setPhone("+14155552671");
        entity.setDeleted(0L);
        return entity;
    }

    private static CrmInquiryEntity existing(LocalDateTime createTime) {
        CrmInquiryEntity entity = new CrmInquiryEntity();
        entity.setId("I1");
        entity.setCustomerId("C1");
        entity.setInquiryNo("INQ20261108000001");
        entity.setSource("EMAIL");
        entity.setCurrencyCode("USD");
        entity.setInquiryStatus("NEW");
        entity.setOwnerId("U1");
        entity.setCreateTime(createTime);
        entity.setDeleted(0L);
        return entity;
    }

    @Test
    @DisplayName("新建询盘会生成编号并落到 NEW 状态")
    void testCreateFillsDefaults() {
        when(customerMapper.selectById("C1")).thenReturn(customer());

        CrmInquiryDto dto = crmInquiryService.create(request());

        assertEquals("NEW", dto.getInquiryStatus());
        assertEquals("USD", dto.getCurrencyCode());
        assertTrue(dto.getInquiryNo().matches("INQ\\d{14}"));

        ArgumentCaptor<CrmInquiryEntity> captor = ArgumentCaptor.forClass(CrmInquiryEntity.class);
        verify(inquiryMapper).insert(captor.capture());
        assertEquals("C1", captor.getValue().getCustomerId());
    }

    @Test
    @DisplayName("新建询盘时客户与来源与负责人必填")
    void testCreateRejectsBlankFields() {
        CrmInquiryRequest noCustomer = request();
        noCustomer.setCustomerId(" ");
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> crmInquiryService.create(noCustomer)).getMessage().contains("customerId"));

        CrmInquiryRequest noSource = request();
        noSource.setSource(null);
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> crmInquiryService.create(noSource)).getMessage().contains("source"));

        CrmInquiryRequest noOwner = request();
        noOwner.setOwnerId(null);
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> crmInquiryService.create(noOwner)).getMessage().contains("ownerId"));
    }

    @Test
    @DisplayName("新建询盘时客户必须存在")
    void testCreateRejectsMissingCustomer() {
        when(customerMapper.selectById("C1")).thenReturn(null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmInquiryService.create(request()));
        assertTrue(error.getMessage().contains("customer not found"));
        verify(inquiryMapper, never()).insert(any());
    }

    @Test
    @DisplayName("联系人必须属于该客户")
    void testCreateRejectsForeignContact() {
        when(customerMapper.selectById("C1")).thenReturn(customer());
        when(customerContactMapper.selectById("CT1")).thenReturn(contact("C2"));
        CrmInquiryRequest request = request();
        request.setContactId("CT1");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmInquiryService.create(request));
        assertTrue(error.getMessage().contains("does not belong to the customer"));
    }

    @Test
    @DisplayName("联系人不存在时拒绝建单")
    void testCreateRejectsMissingContact() {
        when(customerMapper.selectById("C1")).thenReturn(customer());
        when(customerContactMapper.selectById("CT404")).thenReturn(null);
        CrmInquiryRequest request = request();
        request.setContactId("CT404");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmInquiryService.create(request));
        assertTrue(error.getMessage().contains("contact not found"));
    }

    @Test
    @DisplayName("详情里应带上关联报价单")
    void testGetByIdIncludesQuotations() {
        when(inquiryMapper.selectById("I1")).thenReturn(existing(LocalDateTime.now()));
        CrmQuotationEntity quotation = new CrmQuotationEntity();
        quotation.setId("Q1");
        quotation.setQuotationNo("QT20261108000001");
        quotation.setVersionNo(1);
        quotation.setCurrencyCode("USD");
        quotation.setTotalAmount(new BigDecimal("1260.00"));
        quotation.setQuotationStatus("DRAFT");
        when(quotationMapper.selectByInquiryId("I1")).thenReturn(Collections.singletonList(quotation));

        CrmInquiryDto dto = crmInquiryService.getById("I1");

        assertEquals(1, dto.getQuotations().size());
        assertEquals("QT20261108000001", dto.getQuotations().get(0).getQuotationNo());
        assertFalse(dto.isPendingQuote());
    }

    @Test
    @DisplayName("超过 24 小时且没有报价单的询盘算超时未报价")
    void testPendingQuoteFlag() {
        when(inquiryMapper.selectById("I1")).thenReturn(existing(LocalDateTime.now().minusHours(30)));
        when(quotationMapper.selectByInquiryId("I1")).thenReturn(Collections.emptyList());

        CrmInquiryDto dto = crmInquiryService.getById("I1");

        assertTrue(dto.isPendingQuote());
    }

    @Test
    @DisplayName("已经有报价单的询盘不算超时未报价")
    void testNotPendingWhenQuotationExists() {
        when(inquiryMapper.selectById("I1")).thenReturn(existing(LocalDateTime.now().minusHours(30)));
        CrmQuotationEntity quotation = new CrmQuotationEntity();
        quotation.setId("Q1");
        when(quotationMapper.selectByInquiryId("I1")).thenReturn(Collections.singletonList(quotation));

        assertFalse(crmInquiryService.getById("I1").isPendingQuote());
    }

    @Test
    @DisplayName("更新不存在的询盘应报错")
    void testUpdateRejectsMissingInquiry() {
        when(inquiryMapper.selectById("I404")).thenReturn(null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmInquiryService.update("I404", request()));
        assertTrue(error.getMessage().contains("inquiry not found"));
    }

    @Test
    @DisplayName("更新会保留原状态除非显式指定")
    void testUpdateKeepsStatusWhenUnchanged() {
        CrmInquiryEntity entity = existing(LocalDateTime.now());
        entity.setInquiryStatus("PROCESSING");
        when(inquiryMapper.selectById("I1")).thenReturn(entity);
        when(customerMapper.selectById("C1")).thenReturn(customer());

        CrmInquiryDto dto = crmInquiryService.update("I1", request());

        assertEquals("PROCESSING", dto.getInquiryStatus());
        verify(inquiryMapper).update(any());
    }

    @Test
    @DisplayName("分页查询应把结果映射成 DTO")
    void testListMapsRows() {
        when(inquiryMapper.selectPage(any())).thenReturn(Collections.singletonList(existing(LocalDateTime.now())));

        com.yaowenltd.projectinfomationmanage.crm.model.dto.inquiry.CrmInquiryQueryRequest query =
                new com.yaowenltd.projectinfomationmanage.crm.model.dto.inquiry.CrmInquiryQueryRequest();
        query.setPage(1);
        query.setSize(10);

        assertEquals(1, crmInquiryService.list(query).getData().size());
    }

    @Test
    @DisplayName("分页查询参数超出上限时拒绝")
    void testListRejectsTooDeepPaging() {
        com.yaowenltd.projectinfomationmanage.crm.model.dto.inquiry.CrmInquiryQueryRequest query =
                new com.yaowenltd.projectinfomationmanage.crm.model.dto.inquiry.CrmInquiryQueryRequest();
        query.setPage(1000);
        query.setSize(200);

        assertThrows(IllegalArgumentException.class, () -> crmInquiryService.list(query));
    }

}
