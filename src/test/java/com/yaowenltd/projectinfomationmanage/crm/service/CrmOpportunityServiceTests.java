/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.common.MessageNotifier;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmOpportunityMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityPipelineDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityStageCountDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityStageRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmOpportunityEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CustomerEntity;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.CrmOpportunityServiceImpl;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.OpportunityStageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.message.MessageDraft;

/**
 * {@link CrmOpportunityService} 的单元测试。
 * <p>
 * 契约来自需求文档 §3.3、§5.3.4、§5.4：
 * 阶段机是唯一的合法流转依据；置 LOST 必须填丢单原因；
 * 商机必须挂在一个存在的客户上（逻辑外键由 Service 层校验）；
 * 赢单率按当前 stage 快照统计；金额要按当日汇率折出本位币快照。
 * </p>
 */
@ExtendWith(MockitoExtension.class)
class CrmOpportunityServiceTests {

    @Mock
    private CrmOpportunityMapper opportunityMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private CurrencyRateQueryService currencyRateQueryService;

    @Mock
    private MessageService messageService;

    private CrmOpportunityService crmOpportunityService;

    @BeforeEach
    void setUp() {
        crmOpportunityService = new CrmOpportunityServiceImpl(opportunityMapper, customerMapper,
                new OpportunityStageServiceImpl(), currencyRateQueryService, new MessageNotifier(messageService));
    }

    private static CrmOpportunityRequest request() {
        CrmOpportunityRequest request = new CrmOpportunityRequest();
        request.setCustomerId("C1");
        request.setName("2026 展会订单");
        request.setAmount(new BigDecimal("50000.00"));
        request.setCurrencyCode("USD");
        request.setOwnerId("U1");
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

    private static CrmOpportunityEntity existing(String stage) {
        CrmOpportunityEntity entity = new CrmOpportunityEntity();
        entity.setId("O1");
        entity.setCustomerId("C1");
        entity.setName("2026 展会订单");
        entity.setStage(stage);
        entity.setAmount(new BigDecimal("50000.00"));
        entity.setCurrencyCode("USD");
        entity.setAmountBase(new BigDecimal("50000.00"));
        entity.setWinRate(10);
        entity.setOwnerId("U1");
        entity.setDeleted(0L);
        return entity;
    }

    @Test
    @DisplayName("新建商机默认落在初步接触阶段并取阶段默认赢率")
    void testCreateUsesDefaultStageAndWinRate() {
        when(customerMapper.selectById("C1")).thenReturn(customer());

        CrmOpportunityDto dto = crmOpportunityService.create(request());

        ArgumentCaptor<CrmOpportunityEntity> captor = ArgumentCaptor.forClass(CrmOpportunityEntity.class);
        verify(opportunityMapper).insert(captor.capture());
        assertEquals("INITIAL_CONTACT", captor.getValue().getStage());
        assertEquals(10, captor.getValue().getWinRate());
        assertEquals("INITIAL_CONTACT", dto.getStage());
        assertNotNull(captor.getValue().getId());
    }

    @Test
    @DisplayName("新建商机时客户必须存在")
    void testCreateRejectsMissingCustomer() {
        when(customerMapper.selectById("C1")).thenReturn(null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmOpportunityService.create(request()));
        assertTrue(error.getMessage().contains("customer not found"));
        verify(opportunityMapper, never()).insert(any());
    }

    @Test
    @DisplayName("新建商机时客户与负责人不能为空")
    void testCreateRejectsBlankFields() {
        CrmOpportunityRequest noCustomer = request();
        noCustomer.setCustomerId(" ");
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> crmOpportunityService.create(noCustomer)).getMessage().contains("customerId"));

        CrmOpportunityRequest noName = request();
        noName.setName(null);
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> crmOpportunityService.create(noName)).getMessage().contains("name"));

        CrmOpportunityRequest noOwner = request();
        noOwner.setOwnerId(null);
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> crmOpportunityService.create(noOwner)).getMessage().contains("ownerId"));
    }

    @Test
    @DisplayName("新建商机时金额不能为负、赢率必须在 0-100 之间")
    void testCreateValidatesNumbers() {
        CrmOpportunityRequest negativeAmount = request();
        negativeAmount.setAmount(new BigDecimal("-1"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> crmOpportunityService.create(negativeAmount)).getMessage().contains("amount"));

        CrmOpportunityRequest badWinRate = request();
        badWinRate.setWinRate(101);
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> crmOpportunityService.create(badWinRate)).getMessage().contains("winRate"));
    }

    @Test
    @DisplayName("新建商机时阶段码非法应报错")
    void testCreateRejectsUnknownStage() {
        when(customerMapper.selectById("C1")).thenReturn(customer());

        CrmOpportunityRequest badStage = request();
        badStage.setStage("NOT_A_STAGE");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmOpportunityService.create(badStage));
        assertTrue(error.getMessage().contains("unknown opportunity stage"));
    }

    @Test
    @DisplayName("原币为 USD 时本位币快照等于原金额")
    void testCreateSnapshotsBaseAmountForBaseCurrency() {
        when(customerMapper.selectById("C1")).thenReturn(customer());

        crmOpportunityService.create(request());

        ArgumentCaptor<CrmOpportunityEntity> captor = ArgumentCaptor.forClass(CrmOpportunityEntity.class);
        verify(opportunityMapper).insert(captor.capture());
        assertEquals(new BigDecimal("50000.00"), captor.getValue().getAmountBase());
    }

    @Test
    @DisplayName("原币不是 USD 时按当日汇率折算本位币")
    void testCreateConvertsBaseAmount() {
        when(customerMapper.selectById("C1")).thenReturn(customer());
        CrmOpportunityRequest request = request();
        request.setCurrencyCode("EUR");
        request.setAmount(new BigDecimal("1000.00"));
        when(currencyRateQueryService.convert(eq(new BigDecimal("1000.00")), eq("EUR"), eq("USD"), any()))
                .thenReturn(new BigDecimal("1080.00"));

        crmOpportunityService.create(request);

        ArgumentCaptor<CrmOpportunityEntity> captor = ArgumentCaptor.forClass(CrmOpportunityEntity.class);
        verify(opportunityMapper).insert(captor.capture());
        assertEquals(new BigDecimal("1080.00"), captor.getValue().getAmountBase());
    }

    @Test
    @DisplayName("取不到汇率时本位币快照置 0")
    void testCreateFallsBackToZeroWhenRateMissing() {
        when(customerMapper.selectById("C1")).thenReturn(customer());
        CrmOpportunityRequest request = request();
        request.setCurrencyCode("EUR");
        when(currencyRateQueryService.convert(any(), eq("EUR"), eq("USD"), any())).thenReturn(null);

        crmOpportunityService.create(request);

        ArgumentCaptor<CrmOpportunityEntity> captor = ArgumentCaptor.forClass(CrmOpportunityEntity.class);
        verify(opportunityMapper).insert(captor.capture());
        assertEquals(0, captor.getValue().getAmountBase().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("推进阶段必须遵循阶段机，非法流转应报错")
    void testChangeStageRejectsIllegalTransition() {
        when(opportunityMapper.selectById("O1")).thenReturn(existing("INITIAL_CONTACT"));

        CrmOpportunityStageRequest request = new CrmOpportunityStageRequest();
        request.setStage("WON");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmOpportunityService.changeStage("O1", request));
        assertTrue(error.getMessage().contains("illegal opportunity stage transition"));
        verify(opportunityMapper, never()).update(any());
    }

    @Test
    @DisplayName("推进阶段成功时写入新阶段并按阶段默认值刷新赢率")
    void testChangeStageUpdatesStageAndWinRate() {
        when(opportunityMapper.selectById("O1")).thenReturn(existing("INITIAL_CONTACT"));

        CrmOpportunityStageRequest request = new CrmOpportunityStageRequest();
        request.setStage("QUALIFIED");

        CrmOpportunityDto dto = crmOpportunityService.changeStage("O1", request);

        verify(opportunityMapper).update(any());
        assertEquals("QUALIFIED", dto.getStage());
        assertEquals(30, dto.getWinRate());
    }

    @Test
    @DisplayName("置 LOST 必须填丢单原因")
    void testChangeStageRequiresLostReason() {
        when(opportunityMapper.selectById("O1")).thenReturn(existing("INITIAL_CONTACT"));

        CrmOpportunityStageRequest request = new CrmOpportunityStageRequest();
        request.setStage("LOST");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmOpportunityService.changeStage("O1", request));
        assertTrue(error.getMessage().contains("lostReason"));

        request.setLostReason("价格谈不拢");
        CrmOpportunityDto dto = crmOpportunityService.changeStage("O1", request);
        assertEquals("LOST", dto.getStage());
        assertEquals("价格谈不拢", dto.getLostReason());
    }

    @Test
    @DisplayName("更新商机时顺带推进阶段也必须走阶段机")
    void testUpdateValidatesStageTransition() {
        when(opportunityMapper.selectById("O1")).thenReturn(existing("INITIAL_CONTACT"));
        when(customerMapper.selectById("C1")).thenReturn(customer());

        CrmOpportunityRequest request = request();
        request.setStage("NEGOTIATION");

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> crmOpportunityService.update("O1", request));
        assertTrue(error.getMessage().contains("illegal opportunity stage transition"));
        verify(opportunityMapper, never()).update(any());
    }

    @Test
    @DisplayName("更新商机时阶段不变则不做流转校验")
    void testUpdateKeepsStageWhenUnchanged() {
        when(opportunityMapper.selectById("O1")).thenReturn(existing("QUALIFIED"));
        when(customerMapper.selectById("C1")).thenReturn(customer());

        CrmOpportunityRequest request = request();
        request.setStage("QUALIFIED");
        request.setName("新名字");

        CrmOpportunityDto dto = crmOpportunityService.update("O1", request);

        verify(opportunityMapper).update(any());
        assertEquals("QUALIFIED", dto.getStage());
        assertEquals("新名字", dto.getName());
    }

    @Test
    @DisplayName("删除不存在的商机应报错")
    void testDeleteRejectsMissingOpportunity() {
        when(opportunityMapper.selectById("O1")).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> crmOpportunityService.delete("O1"));
        verify(opportunityMapper, never()).softDelete(anyString(), anyLong());
    }

    @Test
    @DisplayName("删除商机走逻辑删除")
    void testDeleteSoftDeletes() {
        when(opportunityMapper.selectById("O1")).thenReturn(existing("QUALIFIED"));

        crmOpportunityService.delete("O1");

        verify(opportunityMapper).softDelete(eq("O1"), anyLong());
    }

    @Test
    @DisplayName("漏斗统计按阶段快照聚合并计算转化率")
    void testPipelineAggregatesStages() {
        when(opportunityMapper.countByStage()).thenReturn(Arrays.asList(
                stageCount("INITIAL_CONTACT", 10, "100000.00"),
                stageCount("QUALIFIED", 6, "60000.00"),
                stageCount("QUOTED", 3, "30000.00"),
                stageCount("NEGOTIATION", 2, "20000.00"),
                stageCount("WON", 2, "25000.00"),
                stageCount("LOST", 2, "15000.00")));

        CrmOpportunityPipelineDto pipeline = crmOpportunityService.pipeline();

        assertEquals(6, pipeline.getStages().size());
        assertEquals("初步接触", pipeline.getStages().get(0).getStageName());
        assertEquals(10, pipeline.getStages().get(0).getCount());
        assertEquals(0, new BigDecimal("100").compareTo(pipeline.getStages().get(0).getConversionRate()));
        assertEquals(0, new BigDecimal("60").compareTo(pipeline.getStages().get(1).getConversionRate()));
        assertEquals(0, new BigDecimal("50").compareTo(pipeline.getStages().get(2).getConversionRate()));
        assertEquals(0, new BigDecimal("66.67").compareTo(pipeline.getStages().get(3).getConversionRate()));
        assertEquals(21, pipeline.getTotalCount());
        assertEquals(0, new BigDecimal("210000.00").compareTo(pipeline.getTotalAmountBase()));
        assertEquals(0, new BigDecimal("50").compareTo(pipeline.getWinRate()));
    }

    @Test
    @DisplayName("没有已决商机时赢单率为 0")
    void testPipelineWinRateIsZeroWithoutDecidedOpportunities() {
        when(opportunityMapper.countByStage()).thenReturn(Collections.singletonList(
                stageCount("INITIAL_CONTACT", 4, "40000.00")));

        CrmOpportunityPipelineDto pipeline = crmOpportunityService.pipeline();

        assertEquals(0, pipeline.getWinRate().compareTo(BigDecimal.ZERO));
        assertEquals(4, pipeline.getTotalCount());
        assertEquals(6, pipeline.getStages().size());
    }

    @Test
    @DisplayName("分页查询应把结果映射成 DTO")
    void testListMapsRows() {
        when(opportunityMapper.selectPage(any())).thenReturn(Collections.singletonList(existing("QUALIFIED")));

        CrmOpportunityQueryRequest query = new CrmOpportunityQueryRequest();
        query.setPage(1);
        query.setSize(10);

        assertEquals(1, crmOpportunityService.list(query).getData().size());
    }

    @Test
    @DisplayName("分页查询参数超出上限时拒绝")
    void testListRejectsTooDeepPaging() {
        CrmOpportunityQueryRequest query = new CrmOpportunityQueryRequest();
        query.setPage(1000);
        query.setSize(200);

        assertThrows(IllegalArgumentException.class, () -> crmOpportunityService.list(query));
    }

    private static CrmOpportunityStageCountDto stageCount(String stage, long count, String amountBase) {
        CrmOpportunityStageCountDto dto = new CrmOpportunityStageCountDto();
        dto.setStage(stage);
        dto.setStageCount(count);
        dto.setAmountBase(new BigDecimal(amountBase));
        return dto;
    }

    @Test
    @DisplayName("商机阶段变更后给负责人发站内信")
    void testChangeStageSendsOpportunityStageMessage() {
        when(opportunityMapper.selectById("O1")).thenReturn(existing("INITIAL_CONTACT"));
        CrmOpportunityStageRequest request = new CrmOpportunityStageRequest();
        request.setStage("QUALIFIED");

        crmOpportunityService.changeStage("O1", request);

        ArgumentCaptor<MessageDraft> captor = ArgumentCaptor.forClass(MessageDraft.class);
        verify(messageService).sendToReceivers(captor.capture(), eq(Collections.singletonList("U1")));
        MessageDraft draft = captor.getValue();
        assertEquals("OPPORTUNITY_STAGE", draft.getMessageType());
        assertTrue(draft.getContent().contains("QUALIFIED"));
    }

}
