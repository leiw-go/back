/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmInquiryMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmOpportunityMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmProductMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmQuotationItemMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmQuotationMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerMapper;
import com.yaowenltd.projectinfomationmanage.crm.common.MessageNotifier;
import com.yaowenltd.projectinfomationmanage.crm.common.MessageTextFactory;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing.PricedItem;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing.PricingItemRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing.ProductPricingInfo;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing.QuotationPricingRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing.QuotationPricingResult;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationItemDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationItemRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmInquiryEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmProductEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmQuotationEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmQuotationItemEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.MessageBizType;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.MessageType;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.QuotationStatus;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmQuotationService;
import com.yaowenltd.projectinfomationmanage.crm.service.CurrencyRateQueryService;
import com.yaowenltd.projectinfomationmanage.crm.service.QuotationPricingService;
import com.yaowenltd.projectinfomationmanage.crm.service.QuotationStatusService;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * CRM 报价单服务实现.
 *
 * @since 2026-11-08
 */
@Service
public class CrmQuotationServiceImpl implements CrmQuotationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrmQuotationServiceImpl.class);

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

    /** 单号里的日期段. */
    private static final DateTimeFormatter NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CrmQuotationMapper quotationMapper;
    private final CrmQuotationItemMapper quotationItemMapper;
    private final CrmInquiryMapper inquiryMapper;
    private final CrmProductMapper productMapper;
    private final CrmOpportunityMapper opportunityMapper;
    private final CustomerMapper customerMapper;
    private final QuotationPricingService quotationPricingService;
    private final QuotationStatusService quotationStatusService;
    private final CurrencyRateQueryService currencyRateQueryService;
    private final MessageNotifier messageNotifier;

    /**
     * 使用指定依赖构造 CrmQuotationServiceImpl.
     *
     * @param quotationMapper          报价单 Mapper
     * @param quotationItemMapper      报价明细 Mapper
     * @param inquiryMapper            询盘 Mapper
     * @param productMapper            产品 Mapper
     * @param opportunityMapper        商机 Mapper
     * @param customerMapper           客户 Mapper（逻辑外键校验）
     * @param quotationPricingService  算价引擎
     * @param quotationStatusService   报价单状态机
     * @param currencyRateQueryService 汇率折算服务
     * @param messageNotifier          站内信通知器（业务事件触发）
     */
    public CrmQuotationServiceImpl(CrmQuotationMapper quotationMapper,
                                   CrmQuotationItemMapper quotationItemMapper,
                                   CrmInquiryMapper inquiryMapper,
                                   CrmProductMapper productMapper,
                                   CrmOpportunityMapper opportunityMapper,
                                   CustomerMapper customerMapper,
                                   QuotationPricingService quotationPricingService,
                                   QuotationStatusService quotationStatusService,
                                   CurrencyRateQueryService currencyRateQueryService,
                                   MessageNotifier messageNotifier) {
        this.quotationMapper = quotationMapper;
        this.quotationItemMapper = quotationItemMapper;
        this.inquiryMapper = inquiryMapper;
        this.productMapper = productMapper;
        this.opportunityMapper = opportunityMapper;
        this.customerMapper = customerMapper;
        this.quotationPricingService = quotationPricingService;
        this.quotationStatusService = quotationStatusService;
        this.currencyRateQueryService = currencyRateQueryService;
        this.messageNotifier = messageNotifier;
    }

    @Override
    public CrmQuotationDto create(CrmQuotationRequest request) {
        return doCreate(request, null);
    }

    @Override
    public CrmQuotationDto createFromInquiry(String inquiryId, CrmQuotationRequest request) {
        if (inquiryId == null || inquiryId.trim().isEmpty()) {
            throw new IllegalArgumentException("inquiry id must not be blank");
        }
        return doCreate(request, inquiryId.trim());
    }

    @Override
    public CrmQuotationDto update(String id, CrmQuotationRequest request) {
        CrmQuotationEntity existing = requireQuotation(id);
        requireDraft(existing);
        if (request == null) {
            throw new IllegalArgumentException("quotation request must not be null");
        }

        String currency = resolveCurrency(request.getCurrencyCode(), existing.getCurrencyCode());
        requireCustomer(existing.getCustomerId());
        if (request.getOpportunityId() != null && !request.getOpportunityId().trim().isEmpty()) {
            requireOpportunity(request.getOpportunityId());
        }

        QuotationPricingResult pricing = price(request, currency);

        existing.setCurrencyCode(currency);
        existing.setIncoterm(request.getIncoterm() == null || request.getIncoterm().trim().isEmpty()
                ? existing.getIncoterm() : request.getIncoterm().trim());
        existing.setMarkupRate(defaultRate(request.getMarkupRate(), existing.getMarkupRate()));
        existing.setDiscountRate(defaultRate(request.getDiscountRate(), existing.getDiscountRate()));
        existing.setOpportunityId(blankToNull(request.getOpportunityId()));
        existing.setOwnerId(request.getOwnerId() == null || request.getOwnerId().trim().isEmpty()
                ? existing.getOwnerId() : request.getOwnerId().trim());
        existing.setRemark(request.getRemark());
        if (request.getValidUntil() != null) {
            existing.setValidUntil(request.getValidUntil());
        }
        applyTotals(existing, pricing);
        existing.setTotalAmountBase(snapshotBaseAmount(existing.getTotalAmount(), currency));

        quotationMapper.update(existing);
        replaceItems(existing.getId(), request.getItems(), currency, pricing);
        return toDto(existing, quotationItemMapper.selectByQuotationId(existing.getId()));
    }

    @Override
    public CrmQuotationDto getById(String id) {
        CrmQuotationEntity existing = requireQuotation(id);
        List<CrmQuotationItemEntity> items = quotationItemMapper.selectByQuotationId(id);
        CrmQuotationDto dto = toDto(existing, items);
        // 过期是"查询时按 valid_until 判定"，不依赖定时任务（需求文档 §3.5）
        if (quotationStatusService.isExpired(QuotationStatus.valueOf(existing.getQuotationStatus()),
                existing.getValidUntil(), LocalDate.now())) {
            dto.setQuotationStatus(QuotationStatus.EXPIRED.name());
        }
        return dto;
    }

    @Override
    public PageResponse<CrmQuotationDto> list(CrmQuotationQueryRequest query) {
        CrmQuotationQueryRequest actual = query == null ? new CrmQuotationQueryRequest() : query;
        int page = actual.getPage() == null || actual.getPage() < 1 ? 1 : actual.getPage();
        int size = actual.getSize() == null || actual.getSize() < 1
                ? DEFAULT_PAGE_SIZE : Math.min(actual.getSize(), MAX_PAGE_SIZE);
        if ((long) page * size > MAX_PAGE_OFFSET) {
            throw new IllegalArgumentException("page * size must not exceed " + MAX_PAGE_OFFSET);
        }
        PageHelper.startPage(page, size);
        try {
            PageInfo<CrmQuotationEntity> info = new PageInfo<>(quotationMapper.selectPage(actual));

            List<CrmQuotationDto> rows = new ArrayList<>(info.getList().size());
            for (CrmQuotationEntity entity : info.getList()) {
                CrmQuotationDto dto = toDto(entity, new ArrayList<>());
                if (quotationStatusService.isExpired(QuotationStatus.valueOf(entity.getQuotationStatus()),
                        entity.getValidUntil(), LocalDate.now())) {
                    dto.setQuotationStatus(QuotationStatus.EXPIRED.name());
                }
                rows.add(dto);
            }
            PageResponse<CrmQuotationDto> response = new PageResponse<>();
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
    public CrmQuotationDto calculate(CrmQuotationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("quotation request must not be null");
        }
        String currency = resolveCurrency(request.getCurrencyCode(), null);
        QuotationPricingResult pricing = price(request, currency);

        CrmQuotationDto dto = new CrmQuotationDto();
        dto.setCurrencyCode(currency);
        dto.setMarkupRate(defaultRate(request.getMarkupRate(), null));
        dto.setDiscountRate(defaultRate(request.getDiscountRate(), null));
        dto.setIncoterm(request.getIncoterm());
        dto.setValidUntil(request.getValidUntil());
        dto.setOwnerId(request.getOwnerId());
        dto.setQuotationStatus(QuotationStatus.DRAFT.name());
        applyTotals(dto, pricing);
        dto.setItems(toItemDtos(request.getItems(), pricing.getItems()));
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrmQuotationDto recalculate(String id) {
        CrmQuotationEntity existing = requireQuotation(id);
        requireDraft(existing);
        List<CrmQuotationItemEntity> items = quotationItemMapper.selectByQuotationId(id);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("quotation has no item to recalculate: " + id);
        }

        // 按当前单头加价率 / 折扣重算：手工改价的行保留原单价，其余行用成本快照重算
        List<CrmQuotationItemRequest> requests = new ArrayList<>(items.size());
        for (CrmQuotationItemEntity item : items) {
            CrmQuotationItemRequest request = new CrmQuotationItemRequest();
            request.setProductId(item.getProductId());
            request.setQuantity(item.getQuantity());
            request.setMarkupRate(item.getMarkupRate());
            request.setDiscountRate(item.getDiscountRate());
            request.setManualPrice(item.getManualPrice() != null && item.getManualPrice() == 1);
            if (request.isManualPrice()) {
                request.setUnitPrice(item.getUnitPrice());
            }
            requests.add(request);
        }

        QuotationPricingResult pricing = priceStored(items, existing);
        applyTotals(existing, pricing);
        existing.setTotalAmountBase(snapshotBaseAmount(existing.getTotalAmount(), existing.getCurrencyCode()));
        quotationMapper.update(existing);
        replaceItems(id, requests, existing.getCurrencyCode(), pricing);
        return toDto(existing, quotationItemMapper.selectByQuotationId(id));
    }

    @Override
    public CrmQuotationDto submit(String id) {
        return transit(id, QuotationStatus.SENT, null);
    }

    @Override
    public CrmQuotationDto confirm(String id) {
        return transit(id, QuotationStatus.CONFIRMED, null);
    }

    @Override
    public CrmQuotationDto reject(String id, String rejectReason) {
        return transit(id, QuotationStatus.REJECTED, rejectReason);
    }

    private CrmQuotationDto transit(String id, QuotationStatus target, String rejectReason) {
        CrmQuotationEntity existing = requireQuotation(id);
        QuotationStatus current = QuotationStatus.valueOf(existing.getQuotationStatus());
        quotationStatusService.assertCanTransit(current, target, rejectReason);
        existing.setQuotationStatus(target.name());
        if (target == QuotationStatus.REJECTED) {
            existing.setRejectReason(rejectReason.trim());
        }
        quotationMapper.update(existing);
        notifyStatusEvent(existing, target, rejectReason);
        return toDto(existing, quotationItemMapper.selectByQuotationId(id));
    }

    /**
     * 报价状态流转后发站内信（需求文档 §3.11）：报价被拒绝 / 报价被确认.
     * <p>
     * 发消息失败只记日志，不影响报价状态流转本身.
     * </p>
     */
    private void notifyStatusEvent(CrmQuotationEntity quotation, QuotationStatus target, String rejectReason) {
        if (target == QuotationStatus.REJECTED) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("reason", rejectReason == null ? "" : rejectReason);
            notifyOwner(MessageType.QUOTATION_REJECTED, quotation, placeholders);
            return;
        }
        if (target == QuotationStatus.CONFIRMED) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("amount", MessageTextFactory.money(
                    quotation.getTotalAmount(), quotation.getCurrencyCode()));
            notifyOwner(MessageType.QUOTATION_CONFIRMED, quotation, placeholders);
        }
    }

    /**
     * 给报价负责人发一条业务事件站内信.
     * <p>
     * 收件人缺失（历史数据没有负责人）或发送失败时只记日志并跳过，不抛异常.
     * </p>
     */
    private void notifyOwner(MessageType type, CrmQuotationEntity quotation,
                             Map<String, String> placeholders) {
        messageNotifier.notifyOwner(type, MessageBizType.QUOTATION.name(), quotation.getId(),
                quotation.getQuotationNo(), quotation.getOwnerId(), placeholders);
    }

    @Transactional(rollbackFor = Exception.class)
    private CrmQuotationDto doCreate(CrmQuotationRequest request, String inquiryId) {
        if (request == null) {
            throw new IllegalArgumentException("quotation request must not be null");
        }
        CrmInquiryEntity inquiry = null;
        if (inquiryId != null) {
            inquiry = inquiryMapper.selectById(inquiryId);
            if (inquiry == null) {
                throw new IllegalArgumentException("inquiry not found: " + inquiryId);
            }
        }

        String customerId = request.getCustomerId();
        if ((customerId == null || customerId.trim().isEmpty()) && inquiry != null) {
            customerId = inquiry.getCustomerId();
        }
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("customerId must not be blank");
        }
        requireCustomer(customerId);

        if (request.getOpportunityId() != null && !request.getOpportunityId().trim().isEmpty()) {
            requireOpportunity(request.getOpportunityId());
        }

        String defaultCurrency = inquiry == null ? null : inquiry.getCurrencyCode();
        String currency = resolveCurrency(request.getCurrencyCode(), defaultCurrency);
        QuotationPricingResult pricing = price(request, currency);

        CrmQuotationEntity entity = new CrmQuotationEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setQuotationNo(generateNo("QT"));
        entity.setCustomerId(customerId.trim());
        entity.setInquiryId(inquiry == null ? null : inquiry.getId());
        entity.setOpportunityId(blankToNull(request.getOpportunityId()));
        entity.setVersionNo(inquiry == null ? 1
                : quotationMapper.selectMaxVersion(inquiry.getId(), currency) + 1);
        entity.setCurrencyCode(currency);
        entity.setIncoterm(request.getIncoterm() == null ? "FOB" : request.getIncoterm().trim());
        entity.setMarkupRate(defaultRate(request.getMarkupRate(), null));
        entity.setDiscountRate(defaultRate(request.getDiscountRate(), null));
        entity.setValidUntil(request.getValidUntil());
        entity.setQuotationStatus(QuotationStatus.DRAFT.name());
        entity.setOwnerId(request.getOwnerId() == null || request.getOwnerId().trim().isEmpty()
                ? null : request.getOwnerId().trim());
        entity.setRemark(request.getRemark());
        applyTotals(entity, pricing);
        entity.setTotalAmountBase(snapshotBaseAmount(entity.getTotalAmount(), currency));
        entity.setDeleted(0L);

        quotationMapper.insert(entity);
        replaceItems(entity.getId(), request.getItems(), currency, pricing);

        if (inquiry != null) {
            // 首次回复时间在报价单创建时回填（需求文档 §3.4）
            inquiryMapper.updateFirstReplyAt(inquiry.getId(), LocalDateTime.now());
            inquiryMapper.updateStatus(inquiry.getId(), "QUOTED");
        }
        return toDto(entity, quotationItemMapper.selectByQuotationId(entity.getId()));
    }

    /**
     * 算价：把请求里的明细行组装成算价引擎的输入.
     */
    private QuotationPricingResult price(CrmQuotationRequest request, String currency) {
        List<CrmQuotationItemRequest> items = request.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("quotation must contain at least one item");
        }
        List<PricingItemRequest> pricingItems = new ArrayList<>(items.size());
        for (CrmQuotationItemRequest item : items) {
            if (item == null) {
                throw new IllegalArgumentException("quotation item must not be null");
            }
            if (item.getProductId() == null || item.getProductId().trim().isEmpty()) {
                throw new IllegalArgumentException("quotation item productId must not be blank");
            }
            if (item.isManualPrice() && item.getUnitPrice() == null) {
                throw new IllegalArgumentException("manual price quotation item requires unitPrice: "
                        + item.getProductId());
            }
            CrmProductEntity product = requireProduct(item.getProductId());
            pricingItems.add(toPricingItem(item, product, currency));
        }
        QuotationPricingRequest pricingRequest = new QuotationPricingRequest();
        pricingRequest.setCurrencyCode(currency);
        pricingRequest.setMarkupRate(defaultRate(request.getMarkupRate(), null));
        pricingRequest.setDiscountRate(defaultRate(request.getDiscountRate(), null));
        pricingRequest.setItems(pricingItems);
        return quotationPricingService.calculate(pricingRequest);
    }

    /**
     * 重算：沿用明细里已快照的成本价，只按当前加价率 / 折扣重算单价.
     * <p>
     * 已建明细的行不再重复校验在售状态与 MOQ —— 那是建明细时的准入条件。
     */
    private QuotationPricingResult priceStored(List<CrmQuotationItemEntity> storedItems,
                                                CrmQuotationEntity quotation) {
        List<PricingItemRequest> pricingItems = new ArrayList<>(storedItems.size());
        for (CrmQuotationItemEntity item : storedItems) {
            ProductPricingInfo info = new ProductPricingInfo();
            info.setProductId(item.getProductId());
            // 沿用明细里的成本价快照，不重新取当前产品成本价
            info.setCostPrice(item.getCostPrice());
            info.setCostCurrency(quotation.getCurrencyCode());
            info.setMoq(null);
            info.setOnSale(true);

            PricingItemRequest pricingItem = new PricingItemRequest();
            pricingItem.setProduct(info);
            pricingItem.setQuantity(item.getQuantity());
            pricingItem.setMarkupRate(item.getMarkupRate());
            pricingItem.setDiscountRate(item.getDiscountRate());
            if (item.getManualPrice() != null && item.getManualPrice() == 1) {
                pricingItem.setManualUnitPrice(item.getUnitPrice());
            }
            pricingItems.add(pricingItem);
        }
        QuotationPricingRequest pricingRequest = new QuotationPricingRequest();
        pricingRequest.setCurrencyCode(quotation.getCurrencyCode());
        pricingRequest.setMarkupRate(quotation.getMarkupRate());
        pricingRequest.setDiscountRate(quotation.getDiscountRate());
        pricingRequest.setItems(pricingItems);
        return quotationPricingService.calculate(pricingRequest);
    }

    private PricingItemRequest toPricingItem(CrmQuotationItemRequest item, CrmProductEntity product,
                                              String currency) {
        ProductPricingInfo info = new ProductPricingInfo();
        info.setProductId(product.getId());
        info.setCostPrice(product.getCostPrice());
        info.setCostCurrency(product.getCostCurrency());
        info.setMoq(product.getMoq());
        info.setOnSale(product.getProductStatus() != null && product.getProductStatus() == 1);

        PricingItemRequest pricingItem = new PricingItemRequest();
        pricingItem.setProduct(info);
        pricingItem.setQuantity(item.getQuantity());
        pricingItem.setMarkupRate(item.getMarkupRate());
        pricingItem.setDiscountRate(item.getDiscountRate());
        // 非手工改价的行，服务端**忽略**前端传来的单价
        if (item.isManualPrice()) {
            pricingItem.setManualUnitPrice(item.getUnitPrice());
        }
        return pricingItem;
    }

    /**
     * 整批替换报价明细.
     */
    private void replaceItems(String quotationId, List<CrmQuotationItemRequest> requests, String currency,
                              QuotationPricingResult pricing) {
        quotationItemMapper.softDeleteByQuotationId(quotationId, System.currentTimeMillis());
        List<PricedItem> pricedItems = pricing.getItems();
        for (int i = 0; i < requests.size(); i++) {
            CrmQuotationItemRequest request = requests.get(i);
            PricedItem priced = pricedItems.get(i);
            CrmProductEntity product = requireProduct(request.getProductId());

            CrmQuotationItemEntity entity = new CrmQuotationItemEntity();
            entity.setId(UUID.randomUUID().toString());
            entity.setQuotationId(quotationId);
            entity.setProductId(product.getId());
            entity.setProductName(product.getProductName());
            entity.setSpecification(product.getSpecification());
            entity.setQuantity(request.getQuantity());
            entity.setUnit(product.getUnit());
            entity.setCostPrice(priced.getCostPrice());
            entity.setMarkupRate(request.getMarkupRate());
            entity.setDiscountRate(request.getDiscountRate());
            entity.setUnitPrice(priced.getUnitPrice());
            entity.setAmount(priced.getAmount());
            entity.setManualPrice(priced.isManualPrice() ? 1 : 0);
            entity.setDeleted(0L);
            quotationItemMapper.insert(entity);
        }
    }

    private void applyTotals(CrmQuotationEntity entity, QuotationPricingResult pricing) {
        entity.setTotalAmount(pricing.getTotalAmount());
        entity.setTotalCost(pricing.getTotalCost());
        entity.setProfitAmount(pricing.getProfitAmount());
        entity.setProfitRate(pricing.getProfitRate());
    }

    private void applyTotals(CrmQuotationDto dto, QuotationPricingResult pricing) {
        dto.setTotalAmount(pricing.getTotalAmount());
        dto.setTotalCost(pricing.getTotalCost());
        dto.setProfitAmount(pricing.getProfitAmount());
        dto.setProfitRate(pricing.getProfitRate());
    }

    private List<CrmQuotationItemDto> toItemDtos(List<CrmQuotationItemRequest> requests,
                                                  List<PricedItem> pricedItems) {
        List<CrmQuotationItemDto> rows = new ArrayList<>(pricedItems.size());
        for (int i = 0; i < pricedItems.size(); i++) {
            PricedItem priced = pricedItems.get(i);
            CrmQuotationItemRequest request = requests.get(i);
            CrmQuotationItemDto dto = new CrmQuotationItemDto();
            dto.setProductId(priced.getProductId());
            dto.setQuantity(request.getQuantity());
            dto.setCostPrice(priced.getCostPrice());
            dto.setMarkupRate(priced.getMarkupRate());
            dto.setDiscountRate(priced.getDiscountRate());
            dto.setUnitPrice(priced.getUnitPrice());
            dto.setAmount(priced.getAmount());
            dto.setManualPrice(priced.isManualPrice());
            rows.add(dto);
        }
        return rows;
    }

    private CrmQuotationDto toDto(CrmQuotationEntity entity, List<CrmQuotationItemEntity> items) {
        CrmQuotationDto dto = new CrmQuotationDto();
        dto.setId(entity.getId());
        dto.setQuotationNo(entity.getQuotationNo());
        dto.setCustomerId(entity.getCustomerId());
        dto.setInquiryId(entity.getInquiryId());
        dto.setOpportunityId(entity.getOpportunityId());
        dto.setVersionNo(entity.getVersionNo());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setIncoterm(entity.getIncoterm());
        dto.setMarkupRate(entity.getMarkupRate());
        dto.setDiscountRate(entity.getDiscountRate());
        dto.setTotalAmount(entity.getTotalAmount());
        dto.setTotalCost(entity.getTotalCost());
        dto.setProfitAmount(entity.getProfitAmount());
        dto.setProfitRate(entity.getProfitRate());
        dto.setTotalAmountBase(entity.getTotalAmountBase());
        dto.setValidUntil(entity.getValidUntil());
        dto.setQuotationStatus(entity.getQuotationStatus());
        dto.setRejectReason(entity.getRejectReason());
        dto.setOwnerId(entity.getOwnerId());
        dto.setRemark(entity.getRemark());

        List<CrmQuotationItemDto> rows = new ArrayList<>(items.size());
        for (CrmQuotationItemEntity item : items) {
            CrmQuotationItemDto itemDto = new CrmQuotationItemDto();
            itemDto.setId(item.getId());
            itemDto.setProductId(item.getProductId());
            itemDto.setProductName(item.getProductName());
            itemDto.setSpecification(item.getSpecification());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setUnit(item.getUnit());
            itemDto.setCostPrice(item.getCostPrice());
            itemDto.setMarkupRate(item.getMarkupRate());
            itemDto.setDiscountRate(item.getDiscountRate());
            itemDto.setUnitPrice(item.getUnitPrice());
            itemDto.setAmount(item.getAmount());
            itemDto.setManualPrice(item.getManualPrice() != null && item.getManualPrice() == 1);
            rows.add(itemDto);
        }
        dto.setItems(rows);
        return dto;
    }

    private CrmQuotationEntity requireQuotation(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("quotation id must not be blank");
        }
        CrmQuotationEntity entity = quotationMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("quotation not found: " + id);
        }
        return entity;
    }

    private void requireDraft(CrmQuotationEntity quotation) {
        if (!QuotationStatus.DRAFT.name().equals(quotation.getQuotationStatus())) {
            throw new IllegalArgumentException(
                    "only DRAFT quotation can be modified: " + quotation.getQuotationStatus());
        }
    }

    private CrmProductEntity requireProduct(String productId) {
        CrmProductEntity product = productMapper.selectById(productId);
        if (product == null) {
            throw new IllegalArgumentException("product not found: " + productId);
        }
        return product;
    }

    private void requireCustomer(String customerId) {
        if (customerMapper.selectById(customerId) == null) {
            throw new IllegalArgumentException("customer not found: " + customerId);
        }
    }

    private void requireOpportunity(String opportunityId) {
        if (opportunityMapper.selectById(opportunityId) == null) {
            throw new IllegalArgumentException("opportunity not found: " + opportunityId);
        }
    }

    private String resolveCurrency(String requested, String fallback) {
        if (requested != null && !requested.trim().isEmpty()) {
            return requested.trim().toUpperCase(Locale.ROOT);
        }
        if (fallback != null && !fallback.trim().isEmpty()) {
            return fallback.trim().toUpperCase(Locale.ROOT);
        }
        return DEFAULT_CURRENCY;
    }

    private BigDecimal defaultRate(BigDecimal requested, BigDecimal fallback) {
        if (requested != null) {
            return requested;
        }
        return fallback == null ? BigDecimal.ZERO : fallback;
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
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

    /**
     * 生成单号：前缀 + 日期 + 6 位随机数，唯一性由数据库唯一索引兜底.
     */
    private String generateNo(String prefix) {
        int suffix = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return prefix + LocalDate.now().format(NO_DATE) + suffix;
    }

}
