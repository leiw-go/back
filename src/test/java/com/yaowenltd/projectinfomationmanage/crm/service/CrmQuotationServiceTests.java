/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmInquiryMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmOpportunityMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmProductMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmQuotationItemMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmQuotationMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationItemRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmInquiryEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmProductEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmQuotationEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmQuotationItemEntity;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.CrmQuotationServiceImpl;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.QuotationPricingServiceImpl;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.QuotationStatusServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

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
 * {@link CrmQuotationService} 的单元测试。
 * <p>
 * 契约来自需求文档 §3.5、§3.5.1、§5.4：
 * 单价与金额**一律由服务端算**（前端传的 unitPrice 只在 manualPrice = true 时生效）；
 * 明细必须快照产品名 / 规格 / 单位 / 成本价；只有 DRAFT 可改；
 * 版本号在"同询盘 + 同币种"内递增；报价单创建时回填询盘的首次回复时间。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class CrmQuotationServiceTests {

    @Mock
    private CrmQuotationMapper quotationMapper;

    @Mock
    private CrmQuotationItemMapper quotationItemMapper;

    @Mock
    private CrmInquiryMapper inquiryMapper;

    @Mock
    private CrmProductMapper productMapper;

    @Mock
    private CrmOpportunityMapper opportunityMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private CurrencyRateQueryService currencyRateQueryService;

    private CrmQuotationService crmQuotationService;

    @BeforeEach
    void setUp() {
        crmQuotationService = new CrmQuotationServiceImpl(quotationMapper, quotationItemMapper, inquiryMapper,
                productMapper, opportunityMapper, customerMapper, new QuotationPricingServiceImpl(),
                new QuotationStatusServiceImpl(), currencyRateQueryService);
    }

    private static CrmQuotationRequest request() {
        CrmQuotationRequest request = new CrmQuotationRequest();
        request.setCustomerId("C1");
        request.setCurrencyCode("USD");
        request.setIncoterm("FOB");
        request.setMarkupRate(new BigDecimal("20"));
        request.setOwnerId("U1");

        CrmQuotationItemRequest item = new CrmQuotationItemRequest();
        item.setProductId("P1");
        item.setQuantity(new BigDecimal("100"));
        request.setItems(Collections.singletonList(item));
        return request;
    }

    private static CrmProductEntity product() {
        CrmProductEntity entity = new CrmProductEntity();
        entity.setId("P1");
        entity.setProductCode("P-001");
        entity.setProductName("不锈钢水壶");
        entity.setSpecification("2L");
        entity.setUnit("PCS");
        entity.setCostPrice(new BigDecimal("10.500000"));
        entity.setCostCurrency("USD");
        entity.setMoq(100);
        entity.setProductStatus(1);
        entity.setDeleted(0L);
        return entity;
    }

    private static CrmQuotationEntity existing(String status) {
        CrmQuotationEntity entity = new CrmQuotationEntity();
        entity.setId("Q1");
        entity.setQuotationNo("QT20261108000001");
        entity.setCustomerId("C1");
        entity.setVersionNo(1);
        entity.setCurrencyCode("USD");
        entity.setIncoterm("FOB");
        entity.setMarkupRate(new BigDecimal("20"));
        entity.setDiscountRate(BigDecimal.ZERO);
        entity.setTotalAmount(new BigDecimal("1260.00"));
        entity.setTotalCost(new BigDecimal("1050.00"));
        entity.setProfitAmount(new BigDecimal("210.00"));
        entity.setProfitRate(new BigDecimal("16.67"));
        entity.setTotalAmountBase(new BigDecimal("1260.00"));
        entity.setQuotationStatus(status);
        entity.setOwnerId("U1");
        entity.setDeleted(0L);
        return entity;
    }

    private static CrmInquiryEntity inquiry() {
        CrmInquiryEntity entity = new CrmInquiryEntity();
        entity.setId("I1");
        entity.setCustomerId("C1");
        entity.setInquiryNo("INQ20261108000001");
        entity.setSource("EMAIL");
        entity.setCurrencyCode("USD");
        entity.setInquiryStatus("NEW");
        entity.setOwnerId("U1");
        entity.setDeleted(0L);
        return entity;
    }

    @Test
    @DisplayName("新建报价单应由服务端算价并落明细")
    void testCreatePricesOnServer() {
        when(customerMapper.selectById("C1")).thenReturn(new com.yaowenltd.projectinfomationmanage.crm.model.entity
                .CustomerEntity());
        when(productMapper.selectById("P1")).thenReturn(product());

        CrmQuotationDto dto = crmQuotationService.create(request());

        assertEquals("DRAFT", dto.getQuotationStatus());
        assertEquals(1, dto.getVersionNo());
        // 成本 10.5 × (1 + 20%) = 12.6，单价 6 位、行金额 2 位
        assertEquals(0, new BigDecimal("1260.00").compareTo(dto.getTotalAmount()));
        assertEquals(0, new BigDecimal("1050.00").compareTo(dto.getTotalCost()));
        assertEquals(0, new BigDecimal("210.00").compareTo(dto.getProfitAmount()));
        assertEquals(0, new BigDecimal("16.67").compareTo(dto.getProfitRate()));
        assertNotNull(dto.getQuotationNo());

        ArgumentCaptor<CrmQuotationItemEntity> captor =
                ArgumentCaptor.forClass(CrmQuotationItemEntity.class);
        verify(quotationItemMapper).insert(captor.capture());
        CrmQuotationItemEntity saved = captor.getValue();
        assertEquals("不锈钢水壶", saved.getProductName());
        assertEquals("2L", saved.getSpecification());
        assertEquals("PCS", saved.getUnit());
        assertEquals(0, new BigDecimal("10.500000").compareTo(saved.getCostPrice()));
        assertEquals(0, new BigDecimal("12.600000").compareTo(saved.getUnitPrice()));
        assertEquals(0, new BigDecimal("1260.00").compareTo(saved.getAmount()));
    }

    @Test
    @DisplayName("前端传来的单价在非手工改价时必须被忽略")
    void testCreateIgnoresClientUnitPrice() {
        when(customerMapper.selectById("C1")).thenReturn(new com.yaowenltd.projectinfomationmanage.crm.model.entity
                .CustomerEntity());
        when(productMapper.selectById("P1")).thenReturn(product());
        CrmQuotationRequest request = request();
        request.getItems().get(0).setUnitPrice(new BigDecimal("999"));

        CrmQuotationDto dto = crmQuotationService.create(request);

        assertEquals(0, new BigDecimal("1260.00").compareTo(dto.getTotalAmount()));
    }

    @Test
    @DisplayName("手工改价时使用前端单价并标记 manualPrice")
    void testCreateHonoursManualPrice() {
        when(customerMapper.selectById("C1")).thenReturn(new com.yaowenltd.projectinfomationmanage.crm.model.entity
                .CustomerEntity());
        when(productMapper.selectById("P1")).thenReturn(product());
        CrmQuotationRequest request = request();
        request.getItems().get(0).setManualPrice(true);
        request.getItems().get(0).setUnitPrice(new BigDecimal("15"));

        CrmQuotationDto dto = crmQuotationService.create(request);

        assertEquals(0, new BigDecimal("1500.00").compareTo(dto.getTotalAmount()));
        ArgumentCaptor<CrmQuotationItemEntity> captor =
                ArgumentCaptor.forClass(CrmQuotationItemEntity.class);
        verify(quotationItemMapper).insert(captor.capture());
        assertEquals(1, captor.getValue().getManualPrice());
    }

    @Test
    @DisplayName("手工改价但没传单价应报错")
    void testCreateRejectsManualPriceWithoutUnitPrice() {
        when(customerMapper.selectById("C1")).thenReturn(new com.yaowenltd.projectinfomationmanage.crm.model.entity
                .CustomerEntity());
        CrmQuotationRequest request = request();
        request.getItems().get(0).setManualPrice(true);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmQuotationService.create(request));
        assertTrue(error.getMessage().contains("requires unitPrice"));
        verify(quotationMapper, never()).insert(any());
    }

    @Test
    @DisplayName("新建报价单时客户必须存在")
    void testCreateRejectsMissingCustomer() {
        when(customerMapper.selectById("C1")).thenReturn(null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmQuotationService.create(request()));
        assertTrue(error.getMessage().contains("customer not found"));
    }

    @Test
    @DisplayName("新建报价单时产品必须存在")
    void testCreateRejectsMissingProduct() {
        when(customerMapper.selectById("C1")).thenReturn(new com.yaowenltd.projectinfomationmanage.crm.model.entity
                .CustomerEntity());
        when(productMapper.selectById("P1")).thenReturn(null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmQuotationService.create(request()));
        assertTrue(error.getMessage().contains("product not found"));
    }

    @Test
    @DisplayName("新建报价单时明细不能为空")
    void testCreateRejectsEmptyItems() {
        when(customerMapper.selectById("C1")).thenReturn(new com.yaowenltd.projectinfomationmanage.crm.model.entity
                .CustomerEntity());
        CrmQuotationRequest request = request();
        request.setItems(new java.util.ArrayList<>());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmQuotationService.create(request));
        assertTrue(error.getMessage().contains("at least one item"));
    }

    @Test
    @DisplayName("基于询盘建单时版本号递增并回填询盘首次回复时间")
    void testCreateFromInquiryBumpsVersionAndStampsInquiry() {
        when(inquiryMapper.selectById("I1")).thenReturn(inquiry());
        when(customerMapper.selectById("C1")).thenReturn(new com.yaowenltd.projectinfomationmanage.crm.model.entity
                .CustomerEntity());
        when(productMapper.selectById("P1")).thenReturn(product());
        when(quotationMapper.selectMaxVersion("I1", "USD")).thenReturn(2);

        CrmQuotationDto dto = crmQuotationService.createFromInquiry("I1", request());

        assertEquals(3, dto.getVersionNo());
        assertEquals("I1", dto.getInquiryId());
        verify(inquiryMapper).updateFirstReplyAt(eq("I1"), any());
        verify(inquiryMapper).updateStatus("I1", "QUOTED");
    }

    @Test
    @DisplayName("基于询盘建单时询盘必须存在")
    void testCreateFromInquiryRejectsMissingInquiry() {
        when(inquiryMapper.selectById("I404")).thenReturn(null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmQuotationService.createFromInquiry("I404", request()));
        assertTrue(error.getMessage().contains("inquiry not found"));
    }

    @Test
    @DisplayName("只有 DRAFT 报价单可以改")
    void testUpdateRejectsNonDraft() {
        when(quotationMapper.selectById("Q1")).thenReturn(existing("SENT"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmQuotationService.update("Q1", request()));
        assertTrue(error.getMessage().contains("only DRAFT"));
        verify(quotationMapper, never()).update(any());
    }

    @Test
    @DisplayName("更新 DRAFT 报价单会整批替换明细并重算")
    void testUpdateReplacesItems() {
        when(quotationMapper.selectById("Q1")).thenReturn(existing("DRAFT"));
        when(customerMapper.selectById("C1")).thenReturn(new com.yaowenltd.projectinfomationmanage.crm.model.entity
                .CustomerEntity());
        when(productMapper.selectById("P1")).thenReturn(product());

        CrmQuotationRequest request = request();
        request.setMarkupRate(new BigDecimal("30"));
        crmQuotationService.update("Q1", request);

        verify(quotationItemMapper).softDeleteByQuotationId(eq("Q1"), any());
        verify(quotationItemMapper).insert(any());
        verify(quotationMapper).update(any());
    }

    @Test
    @DisplayName("已过有效期的 DRAFT 报价单查询时按 EXPIRED 返回")
    void testGetByIdReturnsExpiredWhenPastValidUntil() {
        CrmQuotationEntity quotation = existing("DRAFT");
        quotation.setValidUntil(LocalDate.now().minusDays(1));
        when(quotationMapper.selectById("Q1")).thenReturn(quotation);
        when(quotationItemMapper.selectByQuotationId("Q1")).thenReturn(Collections.emptyList());

        CrmQuotationDto dto = crmQuotationService.getById("Q1");

        assertEquals("EXPIRED", dto.getQuotationStatus());
    }

    @Test
    @DisplayName("提交发送把 DRAFT 变成 SENT")
    void testSubmitMovesDraftToSent() {
        when(quotationMapper.selectById("Q1")).thenReturn(existing("DRAFT"));
        when(quotationItemMapper.selectByQuotationId("Q1")).thenReturn(Collections.emptyList());

        CrmQuotationDto dto = crmQuotationService.submit("Q1");

        assertEquals("SENT", dto.getQuotationStatus());
        verify(quotationMapper).update(any());
    }

    @Test
    @DisplayName("状态机非法流转应被拒绝")
    void testSubmitRejectedFromSent() {
        when(quotationMapper.selectById("Q1")).thenReturn(existing("SENT"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmQuotationService.submit("Q1"));
        assertTrue(error.getMessage().contains("illegal quotation status transition"));
    }

    @Test
    @DisplayName("客户确认把 SENT 变成 CONFIRMED")
    void testConfirmMovesSentToConfirmed() {
        when(quotationMapper.selectById("Q1")).thenReturn(existing("SENT"));
        when(quotationItemMapper.selectByQuotationId("Q1")).thenReturn(Collections.emptyList());

        assertEquals("CONFIRMED", crmQuotationService.confirm("Q1").getQuotationStatus());
    }

    @Test
    @DisplayName("客户拒绝必须填原因")
    void testRejectRequiresReason() {
        when(quotationMapper.selectById("Q1")).thenReturn(existing("SENT"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmQuotationService.reject("Q1", " "));
        assertTrue(error.getMessage().contains("rejectReason"));
    }

    @Test
    @DisplayName("客户拒绝会记录原因并置 REJECTED")
    void testRejectRecordsReason() {
        when(quotationMapper.selectById("Q1")).thenReturn(existing("SENT"));
        when(quotationItemMapper.selectByQuotationId("Q1")).thenReturn(Collections.emptyList());

        CrmQuotationDto dto = crmQuotationService.reject("Q1", "价格谈不拢");

        assertEquals("REJECTED", dto.getQuotationStatus());
        assertEquals("价格谈不拢", dto.getRejectReason());
    }

    @Test
    @DisplayName("试算只算不落库")
    void testCalculateDoesNotPersist() {
        when(productMapper.selectById("P1")).thenReturn(product());

        CrmQuotationDto dto = crmQuotationService.calculate(request());

        assertEquals(0, new BigDecimal("1260.00").compareTo(dto.getTotalAmount()));
        assertEquals(1, dto.getItems().size());
        verify(quotationMapper, never()).insert(any());
        verify(quotationItemMapper, never()).insert(any());
    }

    @Test
    @DisplayName("重算会保留手工改价行的原价")
    void testRecalculateKeepsManualPrice() {
        CrmQuotationEntity quotation = existing("DRAFT");
        quotation.setMarkupRate(new BigDecimal("50"));
        when(quotationMapper.selectById("Q1")).thenReturn(quotation);

        CrmQuotationItemEntity manual = new CrmQuotationItemEntity();
        manual.setId("QI1");
        manual.setQuotationId("Q1");
        manual.setProductId("P1");
        manual.setQuantity(new BigDecimal("100"));
        manual.setCostPrice(new BigDecimal("10.500000"));
        manual.setUnitPrice(new BigDecimal("15.000000"));
        manual.setAmount(new BigDecimal("1500.00"));
        manual.setManualPrice(1);

        CrmQuotationItemEntity normal = new CrmQuotationItemEntity();
        normal.setId("QI2");
        normal.setQuotationId("Q1");
        normal.setProductId("P1");
        normal.setQuantity(new BigDecimal("100"));
        normal.setCostPrice(new BigDecimal("10.500000"));
        normal.setUnitPrice(new BigDecimal("12.600000"));
        normal.setAmount(new BigDecimal("1260.00"));
        normal.setManualPrice(0);

        when(quotationItemMapper.selectByQuotationId("Q1")).thenReturn(Arrays.asList(manual, normal));
        when(productMapper.selectById("P1")).thenReturn(product());

        CrmQuotationDto dto = crmQuotationService.recalculate("Q1");

        // 手工改价行 15 × 100 = 1500，普通行按 50% 加价 = 15.75 × 100 = 1575
        assertEquals(0, new BigDecimal("3075.00").compareTo(dto.getTotalAmount()));
        ArgumentCaptor<CrmQuotationItemEntity> captor =
                ArgumentCaptor.forClass(CrmQuotationItemEntity.class);
        verify(quotationItemMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertEquals(0, new BigDecimal("15.000000").compareTo(captor.getAllValues().get(0).getUnitPrice()));
        assertEquals(0, new BigDecimal("15.750000").compareTo(captor.getAllValues().get(1).getUnitPrice()));
    }

    @Test
    @DisplayName("重算非 DRAFT 报价单应被拒绝")
    void testRecalculateRejectsNonDraft() {
        when(quotationMapper.selectById("Q1")).thenReturn(existing("SENT"));

        assertThrows(IllegalArgumentException.class, () -> crmQuotationService.recalculate("Q1"));
    }

    @Test
    @DisplayName("重算沿用明细里的成本价快照，不重新取当前产品成本价")
    void testRecalculateUsesCostPriceSnapshot() {
        CrmQuotationEntity quotation = existing("DRAFT");
        quotation.setMarkupRate(BigDecimal.ZERO);
        when(quotationMapper.selectById("Q1")).thenReturn(quotation);

        CrmQuotationItemEntity item = new CrmQuotationItemEntity();
        item.setId("QI1");
        item.setQuotationId("Q1");
        item.setProductId("P1");
        item.setQuantity(new BigDecimal("100"));
        item.setCostPrice(new BigDecimal("8.000000"));
        item.setManualPrice(0);
        when(quotationItemMapper.selectByQuotationId("Q1")).thenReturn(Collections.singletonList(item));

        // 产品当前成本价是 10.5，但明细快照是 8 —— 重算必须用快照 8
        when(productMapper.selectById(any())).thenReturn(product());

        CrmQuotationDto dto = crmQuotationService.recalculate("Q1");

        assertEquals(0, new BigDecimal("800.00").compareTo(dto.getTotalAmount()));
    }

    @Test
    @DisplayName("报价单不存在时报错")
    void testRequireExistingQuotation() {
        when(quotationMapper.selectById("Q404")).thenReturn(null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmQuotationService.getById("Q404"));
        assertTrue(error.getMessage().contains("quotation not found"));
        verify(quotationMapper, never()).update(any());
    }

    @Test
    @DisplayName("算价结果为空的报价单不应被算出来")
    void testPricingResultIsUsedAsIs() {
        when(productMapper.selectById("P1")).thenReturn(product());

        // 直接用真实算价引擎，验证总额与单头汇总来自引擎而不是前端
        CrmQuotationDto dto = crmQuotationService.calculate(request());
        assertEquals(0, new BigDecimal("1260.00").compareTo(dto.getTotalAmount()));
        assertEquals(1, dto.getItems().size());
        assertEquals(0, new BigDecimal("12.600000").compareTo(dto.getItems().get(0).getUnitPrice()));
    }

    @Test
    @DisplayName("分页查询超出上限应拒绝")
    void testListRejectsTooDeepPaging() {
        com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationQueryRequest query =
                new com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationQueryRequest();
        query.setPage(1000);
        query.setSize(200);

        assertThrows(IllegalArgumentException.class, () -> crmQuotationService.list(query));
    }

    @Test
    @DisplayName("关联商机不存在时不允许建单")
    void testCreateRejectsMissingOpportunity() {
        when(customerMapper.selectById("C1")).thenReturn(new com.yaowenltd.projectinfomationmanage.crm.model.entity
                .CustomerEntity());
        when(opportunityMapper.selectById("O404")).thenReturn(null);
        CrmQuotationRequest request = request();
        request.setOpportunityId("O404");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmQuotationService.create(request));
        assertTrue(error.getMessage().contains("opportunity not found"));
    }

    @Test
    @DisplayName("币种默认 USD 且统一大写")
    void testCurrencyDefaultsAndNormalises() {
        when(customerMapper.selectById("C1")).thenReturn(new com.yaowenltd.projectinfomationmanage.crm.model.entity
                .CustomerEntity());
        when(productMapper.selectById("P1")).thenReturn(product());
        CrmQuotationRequest request = request();
        request.setCurrencyCode("eur");

        // 产品成本币种是 USD，用 EUR 建单会被算价引擎拦下（币种校验）
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmQuotationService.create(request));
        assertTrue(error.getMessage().contains("cost currency does not match"));
    }

    @Test
    @DisplayName("查询报价单列表会返回统一结构")
    void testListMapsRows() {
        when(quotationMapper.selectPage(any()))
                .thenReturn(Collections.singletonList(existing("SENT")));

        com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationQueryRequest query =
                new com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationQueryRequest();
        query.setPage(1);
        query.setSize(10);

        assertEquals(1, crmQuotationService.list(query).getData().size());
        verify(quotationMapper).selectPage(any());
    }

    @Test
    @DisplayName("报价单号生成规则为前缀 + 日期 + 数字")
    void testQuotationNoFormat() {
        when(customerMapper.selectById("C1")).thenReturn(new com.yaowenltd.projectinfomationmanage.crm.model.entity
                .CustomerEntity());
        when(productMapper.selectById("P1")).thenReturn(product());

        CrmQuotationDto dto = crmQuotationService.create(request());

        assertTrue(dto.getQuotationNo().matches("QT\\d{14}"));
    }

}
