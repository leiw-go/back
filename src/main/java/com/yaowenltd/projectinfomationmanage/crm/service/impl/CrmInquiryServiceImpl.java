/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmInquiryMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmQuotationMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerContactMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.inquiry.CrmInquiryDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.inquiry.CrmInquiryQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.inquiry.CrmInquiryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.quotation.CrmQuotationSummaryDto;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmInquiryEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmQuotationEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CustomerContactEntity;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmInquiryService;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * CRM 询盘服务实现.
 *
 * @since 2026-11-08
 */
@Service
public class CrmInquiryServiceImpl implements CrmInquiryService {

    /** 列表默认每页条数. */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 列表最大每页条数. */
    private static final int MAX_PAGE_SIZE = 200;

    /** 单次查询允许的最大偏移量. */
    private static final int MAX_PAGE_OFFSET = 10000;

    /** 默认币种. */
    private static final String DEFAULT_CURRENCY = "USD";

    /** 默认状态. */
    private static final String DEFAULT_STATUS = "NEW";

    /** 超时未报价的判定时长（小时）. */
    private static final int PENDING_QUOTE_HOURS = 24;

    /** 单号里的日期段. */
    private static final DateTimeFormatter NO_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final CrmInquiryMapper inquiryMapper;
    private final CrmQuotationMapper quotationMapper;
    private final CustomerMapper customerMapper;
    private final CustomerContactMapper customerContactMapper;

    /**
     * 使用指定的 Mapper 构造 CrmInquiryServiceImpl.
     *
     * @param inquiryMapper         询盘 Mapper
     * @param quotationMapper       报价单 Mapper（关联报价单）
     * @param customerMapper        客户 Mapper（逻辑外键校验）
     * @param customerContactMapper 联系人 Mapper（逻辑外键校验）
     */
    public CrmInquiryServiceImpl(CrmInquiryMapper inquiryMapper,
                                 CrmQuotationMapper quotationMapper,
                                 CustomerMapper customerMapper,
                                 CustomerContactMapper customerContactMapper) {
        this.inquiryMapper = inquiryMapper;
        this.quotationMapper = quotationMapper;
        this.customerMapper = customerMapper;
        this.customerContactMapper = customerContactMapper;
    }

    @Override
    public CrmInquiryDto create(CrmInquiryRequest request) {
        validate(request);
        requireCustomer(request.getCustomerId());
        requireContactOfCustomer(request.getContactId(), request.getCustomerId());

        CrmInquiryEntity entity = new CrmInquiryEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setInquiryNo(generateNo());
        applyRequest(entity, request);
        entity.setInquiryStatus(request.getInquiryStatus() == null || request.getInquiryStatus().trim().isEmpty()
                ? DEFAULT_STATUS : request.getInquiryStatus().trim().toUpperCase(Locale.ROOT));
        entity.setDeleted(0L);
        inquiryMapper.insert(entity);
        return toDto(entity, new ArrayList<>());
    }

    @Override
    public CrmInquiryDto update(String id, CrmInquiryRequest request) {
        validate(request);
        CrmInquiryEntity existing = requireInquiry(id);
        requireCustomer(request.getCustomerId());
        requireContactOfCustomer(request.getContactId(), request.getCustomerId());

        applyRequest(existing, request);
        if (request.getInquiryStatus() != null && !request.getInquiryStatus().trim().isEmpty()) {
            existing.setInquiryStatus(request.getInquiryStatus().trim().toUpperCase(Locale.ROOT));
        }
        inquiryMapper.update(existing);
        return toDto(existing, quotationMapper.selectByInquiryId(id));
    }

    @Override
    public CrmInquiryDto getById(String id) {
        CrmInquiryEntity existing = requireInquiry(id);
        List<CrmQuotationEntity> quotations = quotationMapper.selectByInquiryId(id);
        CrmInquiryDto dto = toDto(existing, quotations);
        dto.setPendingQuote(isPendingQuote(existing, quotations));
        return dto;
    }

    @Override
    public PageResponse<CrmInquiryDto> list(CrmInquiryQueryRequest query) {
        CrmInquiryQueryRequest actual = query == null ? new CrmInquiryQueryRequest() : query;
        int page = actual.getPage() == null || actual.getPage() < 1 ? 1 : actual.getPage();
        int size = actual.getSize() == null || actual.getSize() < 1
                ? DEFAULT_PAGE_SIZE : Math.min(actual.getSize(), MAX_PAGE_SIZE);
        if ((long) page * size > MAX_PAGE_OFFSET) {
            throw new IllegalArgumentException("page * size must not exceed " + MAX_PAGE_OFFSET);
        }
        PageHelper.startPage(page, size);
        try {
            PageInfo<CrmInquiryEntity> info = new PageInfo<>(inquiryMapper.selectPage(actual));

            List<CrmInquiryDto> rows = new ArrayList<>(info.getList().size());
            for (CrmInquiryEntity entity : info.getList()) {
                rows.add(toDto(entity, new ArrayList<>()));
            }
            PageResponse<CrmInquiryDto> response = new PageResponse<>();
            response.setData(rows);
            response.setTotal(info.getTotal());
            response.setPage(info.getPageNum());
            response.setSize(info.getPageSize());
            return response;
        } finally {
            PageHelper.clearPage();
        }
    }

    /**
     * 是否"超时未报价"：创建超过 24 小时、还没回过价、且名下没有任何报价单.
     */
    private boolean isPendingQuote(CrmInquiryEntity inquiry, List<CrmQuotationEntity> quotations) {
        if (inquiry.getFirstReplyAt() != null || !quotations.isEmpty()) {
            return false;
        }
        if (inquiry.getCreateTime() == null) {
            return false;
        }
        return inquiry.getCreateTime().isBefore(LocalDateTime.now().minusHours(PENDING_QUOTE_HOURS));
    }

    private void validate(CrmInquiryRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("inquiry request must not be null");
        }
        requireText(request.getCustomerId(), "customerId");
        requireText(request.getSource(), "source");
        requireText(request.getOwnerId(), "ownerId");
        if (request.getTargetPrice() != null && request.getTargetPrice().signum() < 0) {
            throw new IllegalArgumentException("targetPrice must not be negative");
        }
        if (request.getQuantity() != null && request.getQuantity().signum() < 0) {
            throw new IllegalArgumentException("quantity must not be negative");
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

    /**
     * 联系人必须存在且属于该客户（需求文档 §5.4）.
     */
    private void requireContactOfCustomer(String contactId, String customerId) {
        if (contactId == null || contactId.trim().isEmpty()) {
            return;
        }
        CustomerContactEntity contact = customerContactMapper.selectById(contactId);
        if (contact == null) {
            throw new IllegalArgumentException("contact not found: " + contactId);
        }
        if (!customerId.equals(contact.getCustomerId())) {
            throw new IllegalArgumentException(
                    "contact does not belong to the customer: " + contactId);
        }
    }

    private CrmInquiryEntity requireInquiry(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("inquiry id must not be blank");
        }
        CrmInquiryEntity entity = inquiryMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("inquiry not found: " + id);
        }
        return entity;
    }

    private void applyRequest(CrmInquiryEntity entity, CrmInquiryRequest request) {
        entity.setCustomerId(request.getCustomerId().trim());
        entity.setContactId(request.getContactId() == null || request.getContactId().trim().isEmpty()
                ? null : request.getContactId().trim());
        entity.setSource(request.getSource().trim());
        entity.setSubject(request.getSubject());
        entity.setTargetPrice(request.getTargetPrice());
        entity.setCurrencyCode(request.getCurrencyCode() == null || request.getCurrencyCode().trim().isEmpty()
                ? DEFAULT_CURRENCY : request.getCurrencyCode().trim().toUpperCase(Locale.ROOT));
        entity.setQuantity(request.getQuantity());
        entity.setUnit(request.getUnit());
        entity.setExpectDeliveryDate(request.getExpectDeliveryDate());
        entity.setContent(request.getContent());
        entity.setOwnerId(request.getOwnerId().trim());
    }

    private CrmInquiryDto toDto(CrmInquiryEntity entity, List<CrmQuotationEntity> quotations) {
        CrmInquiryDto dto = new CrmInquiryDto();
        dto.setId(entity.getId());
        dto.setCustomerId(entity.getCustomerId());
        dto.setContactId(entity.getContactId());
        dto.setInquiryNo(entity.getInquiryNo());
        dto.setSource(entity.getSource());
        dto.setSubject(entity.getSubject());
        dto.setTargetPrice(entity.getTargetPrice());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setQuantity(entity.getQuantity());
        dto.setUnit(entity.getUnit());
        dto.setExpectDeliveryDate(entity.getExpectDeliveryDate());
        dto.setInquiryStatus(entity.getInquiryStatus());
        dto.setContent(entity.getContent());
        dto.setOwnerId(entity.getOwnerId());
        dto.setFirstReplyAt(entity.getFirstReplyAt());

        List<CrmQuotationSummaryDto> summaries = new ArrayList<>(quotations.size());
        for (CrmQuotationEntity quotation : quotations) {
            CrmQuotationSummaryDto summary = new CrmQuotationSummaryDto();
            summary.setId(quotation.getId());
            summary.setQuotationNo(quotation.getQuotationNo());
            summary.setVersionNo(quotation.getVersionNo());
            summary.setCurrencyCode(quotation.getCurrencyCode());
            summary.setTotalAmount(quotation.getTotalAmount());
            summary.setQuotationStatus(quotation.getQuotationStatus());
            summary.setValidUntil(quotation.getValidUntil());
            summaries.add(summary);
        }
        dto.setQuotations(summaries);
        return dto;
    }

    /**
     * 生成询盘编号：INQ + 日期 + 6 位随机数，唯一性由数据库唯一索引兜底.
     */
    private String generateNo() {
        int suffix = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "INQ" + LocalDate.now().format(NO_DATE) + suffix;
    }

}
