/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmOpportunityMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityPipelineDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityStageCountDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity.CrmOpportunityStageRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmOpportunityEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.OpportunityStage;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmOpportunityService;
import com.yaowenltd.projectinfomationmanage.crm.service.CurrencyRateQueryService;
import com.yaowenltd.projectinfomationmanage.crm.service.OpportunityStageService;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * CRM 商机服务实现.
 *
 * @since 2026-11-08
 */
@Service
public class CrmOpportunityServiceImpl implements CrmOpportunityService {

    /** 列表默认每页条数. */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 列表最大每页条数. */
    private static final int MAX_PAGE_SIZE = 200;

    /** 单次查询允许的最大偏移量. */
    private static final int MAX_PAGE_OFFSET = 10000;

    /** 本位币（金额快照的目标币种）. */
    private static final String BASE_CURRENCY = "USD";

    /** 默认币种. */
    private static final String DEFAULT_CURRENCY = "USD";

    /** 金额折算后保留的小数位. */
    private static final int AMOUNT_SCALE = 2;

    private final CrmOpportunityMapper opportunityMapper;
    private final CustomerMapper customerMapper;
    private final OpportunityStageService opportunityStageService;
    private final CurrencyRateQueryService currencyRateQueryService;

    /**
     * 使用指定依赖构造 CrmOpportunityServiceImpl.
     *
     * @param opportunityMapper      商机 Mapper
     * @param customerMapper         客户 Mapper（逻辑外键校验）
     * @param opportunityStageService 商机阶段机
     * @param currencyRateQueryService 汇率折算服务
     */
    public CrmOpportunityServiceImpl(CrmOpportunityMapper opportunityMapper,
                                     CustomerMapper customerMapper,
                                     OpportunityStageService opportunityStageService,
                                     CurrencyRateQueryService currencyRateQueryService) {
        this.opportunityMapper = opportunityMapper;
        this.customerMapper = customerMapper;
        this.opportunityStageService = opportunityStageService;
        this.currencyRateQueryService = currencyRateQueryService;
    }

    @Override
    public CrmOpportunityDto create(CrmOpportunityRequest request) {
        validate(request);
        requireCustomer(request.getCustomerId());

        OpportunityStage stage = request.getStage() == null || request.getStage().trim().isEmpty()
                ? OpportunityStage.INITIAL_CONTACT : OpportunityStage.fromCode(request.getStage());

        CrmOpportunityEntity entity = new CrmOpportunityEntity();
        entity.setId(UUID.randomUUID().toString());
        applyRequest(entity, request, stage);
        entity.setStage(stage.name());
        if (request.getWinRate() == null) {
            entity.setWinRate(stage.getDefaultWinRate());
        }
        entity.setAmountBase(snapshotBaseAmount(entity.getAmount(), entity.getCurrencyCode()));
        entity.setDeleted(0L);
        opportunityMapper.insert(entity);
        return toDto(entity);
    }

    @Override
    public CrmOpportunityDto update(String id, CrmOpportunityRequest request) {
        validate(request);
        CrmOpportunityEntity existing = requireOpportunity(id);
        requireCustomer(request.getCustomerId());

        OpportunityStage current = OpportunityStage.fromCode(existing.getStage());
        OpportunityStage target = request.getStage() == null || request.getStage().trim().isEmpty()
                ? current : OpportunityStage.fromCode(request.getStage());
        if (target != current) {
            opportunityStageService.assertCanTransit(current, target, request.getLostReason());
        }

        applyRequest(existing, request, target);
        existing.setStage(target.name());
        if (request.getWinRate() == null) {
            existing.setWinRate(target.getDefaultWinRate());
        }
        existing.setAmountBase(snapshotBaseAmount(existing.getAmount(), existing.getCurrencyCode()));
        opportunityMapper.update(existing);
        return toDto(existing);
    }

    @Override
    public CrmOpportunityDto getById(String id) {
        return toDto(requireOpportunity(id));
    }

    @Override
    public void delete(String id) {
        requireOpportunity(id);
        opportunityMapper.softDelete(id, System.currentTimeMillis());
    }

    @Override
    public PageResponse<CrmOpportunityDto> list(CrmOpportunityQueryRequest query) {
        CrmOpportunityQueryRequest actual = query == null ? new CrmOpportunityQueryRequest() : query;
        int page = actual.getPage() == null || actual.getPage() < 1 ? 1 : actual.getPage();
        int size = actual.getSize() == null || actual.getSize() < 1
                ? DEFAULT_PAGE_SIZE : Math.min(actual.getSize(), MAX_PAGE_SIZE);
        if ((long) page * size > MAX_PAGE_OFFSET) {
            throw new IllegalArgumentException("page * size must not exceed " + MAX_PAGE_OFFSET);
        }
        PageHelper.startPage(page, size);
        try {
            PageInfo<CrmOpportunityEntity> info = new PageInfo<>(opportunityMapper.selectPage(actual));

            List<CrmOpportunityDto> rows = new ArrayList<>(info.getList().size());
            for (CrmOpportunityEntity entity : info.getList()) {
                rows.add(toDto(entity));
            }
            PageResponse<CrmOpportunityDto> response = new PageResponse<>();
            response.setData(rows);
            response.setTotal(info.getTotal());
            response.setPage(info.getPageNum());
            response.setSize(info.getPageSize());
            return response;
        } finally {
            PageHelper.clearPage();
        }
    }

    @Override
    public CrmOpportunityDto changeStage(String id, CrmOpportunityStageRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("stage request must not be null");
        }
        CrmOpportunityEntity existing = requireOpportunity(id);
        OpportunityStage current = OpportunityStage.fromCode(existing.getStage());
        OpportunityStage target = OpportunityStage.fromCode(request.getStage());
        opportunityStageService.assertCanTransit(current, target, request.getLostReason());

        existing.setStage(target.name());
        existing.setWinRate(target.getDefaultWinRate());
        if (target == OpportunityStage.LOST) {
            existing.setLostReason(request.getLostReason().trim());
        }
        opportunityMapper.update(existing);
        return toDto(existing);
    }

    @Override
    public CrmOpportunityPipelineDto pipeline() {
        List<CrmOpportunityStageCountDto> counts = opportunityMapper.countByStage();
        Map<String, CrmOpportunityStageCountDto> byStage = new HashMap<>();
        for (CrmOpportunityStageCountDto count : counts) {
            if (count != null && count.getStage() != null) {
                byStage.put(count.getStage(), count);
            }
        }

        CrmOpportunityPipelineDto pipeline = new CrmOpportunityPipelineDto();
        List<CrmOpportunityPipelineDto.StageBucket> buckets = new ArrayList<>();
        long previousCount = 0;
        long wonCount = 0;
        long lostCount = 0;

        for (OpportunityStage stage : OpportunityStage.values()) {
            CrmOpportunityStageCountDto count = byStage.get(stage.name());
            long stageCount = count == null ? 0L : count.getStageCount();
            BigDecimal amountBase = count == null || count.getAmountBase() == null
                    ? BigDecimal.ZERO : count.getAmountBase();

            CrmOpportunityPipelineDto.StageBucket bucket = new CrmOpportunityPipelineDto.StageBucket();
            bucket.setStage(stage.name());
            bucket.setStageName(stage.getDisplayName());
            bucket.setCount(stageCount);
            bucket.setAmountBase(amountBase);
            if (stage == OpportunityStage.INITIAL_CONTACT) {
                bucket.setConversionRate(BigDecimal.valueOf(100));
            } else if (previousCount <= 0) {
                bucket.setConversionRate(BigDecimal.ZERO);
            } else {
                bucket.setConversionRate(BigDecimal.valueOf(stageCount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(previousCount), 2, RoundingMode.HALF_UP));
            }
            buckets.add(bucket);

            if (stage == OpportunityStage.WON) {
                wonCount = stageCount;
            } else if (stage == OpportunityStage.LOST) {
                lostCount = stageCount;
            } else {
                pipeline.setTotalCount(pipeline.getTotalCount() + stageCount);
                pipeline.setTotalAmountBase(pipeline.getTotalAmountBase().add(amountBase));
            }
            previousCount = stageCount;
        }

        long decided = wonCount + lostCount;
        pipeline.setWinRate(decided <= 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(wonCount).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(decided), 2, RoundingMode.HALF_UP));
        pipeline.setStages(buckets);
        return pipeline;
    }

    private void validate(CrmOpportunityRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("opportunity request must not be null");
        }
        requireText(request.getCustomerId(), "customerId");
        requireText(request.getName(), "name");
        requireText(request.getOwnerId(), "ownerId");
        if (request.getAmount() != null && request.getAmount().signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative");
        }
        if (request.getWinRate() != null && (request.getWinRate() < 0 || request.getWinRate() > 100)) {
            throw new IllegalArgumentException("winRate must be between 0 and 100");
        }
    }

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    private void requireCustomer(String customerId) {
        if (customerMapper.selectById(customerId) == null) {
            throw new IllegalArgumentException("customer not found: " + customerId);
        }
    }

    private CrmOpportunityEntity requireOpportunity(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("opportunity id must not be blank");
        }
        CrmOpportunityEntity entity = opportunityMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("opportunity not found: " + id);
        }
        return entity;
    }

    private void applyRequest(CrmOpportunityEntity entity, CrmOpportunityRequest request,
                              OpportunityStage stage) {
        entity.setCustomerId(request.getCustomerId().trim());
        entity.setName(request.getName().trim());
        entity.setAmount(request.getAmount() == null ? BigDecimal.ZERO : request.getAmount());
        entity.setCurrencyCode(request.getCurrencyCode() == null || request.getCurrencyCode().trim().isEmpty()
                ? DEFAULT_CURRENCY : request.getCurrencyCode().trim().toUpperCase());
        if (request.getWinRate() != null) {
            entity.setWinRate(request.getWinRate());
        }
        entity.setExpectCloseDate(request.getExpectCloseDate());
        entity.setCompetitor(request.getCompetitor());
        entity.setLostReason(request.getLostReason());
        entity.setOwnerId(request.getOwnerId().trim());
        entity.setRemark(request.getRemark());
        if (stage == OpportunityStage.LOST && request.getLostReason() != null) {
            entity.setLostReason(request.getLostReason().trim());
        }
    }

    /**
     * 折算本位币快照：无汇率可用时置 0（汇率模块在 M5 落地后才有数据）.
     */
    private BigDecimal snapshotBaseAmount(BigDecimal amount, String currencyCode) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (BASE_CURRENCY.equalsIgnoreCase(currencyCode)) {
            return amount.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        }
        BigDecimal converted = currencyRateQueryService.convert(
                amount, currencyCode, BASE_CURRENCY, LocalDate.now());
        if (converted == null) {
            return BigDecimal.ZERO;
        }
        return converted.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    private CrmOpportunityDto toDto(CrmOpportunityEntity entity) {
        CrmOpportunityDto dto = new CrmOpportunityDto();
        dto.setId(entity.getId());
        dto.setCustomerId(entity.getCustomerId());
        dto.setName(entity.getName());
        dto.setStage(entity.getStage());
        dto.setAmount(entity.getAmount());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setAmountBase(entity.getAmountBase());
        dto.setWinRate(entity.getWinRate());
        dto.setExpectCloseDate(entity.getExpectCloseDate());
        dto.setCompetitor(entity.getCompetitor());
        dto.setLostReason(entity.getLostReason());
        dto.setOwnerId(entity.getOwnerId());
        dto.setRemark(entity.getRemark());
        return dto;
    }

}
