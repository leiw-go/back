/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerRequest;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmOpportunityMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerContactMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerTagRelMapper;
import com.yaowenltd.projectinfomationmanage.crm.service.CustomerService;
import com.yaowenltd.projectinfomationmanage.crm.common.MessageNotifier;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CustomerEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.MessageBizType;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.MessageType;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * 客户档案服务实现.
 *
 * @since 2026-11-08
 */
@Service
public class CustomerServiceImpl implements CustomerService {

    /** 导出上限（行）: 同步导出不做异步任务，靠这个上限兜底. */
    private static final int EXPORT_LIMIT = 10000;

    /** 列表默认每页条数. */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 列表最大每页条数. */
    private static final int MAX_PAGE_SIZE = 200;

    private final CustomerMapper customerMapper;
    private final CustomerContactMapper customerContactMapper;
    private final CustomerTagRelMapper customerTagRelMapper;
    private final CrmOpportunityMapper opportunityMapper;
    private final MessageNotifier messageNotifier;

    /**
     * 使用指定的 Mapper 构造 CustomerServiceImpl.
     *
     * @param customerMapper         客户 Mapper
     * @param customerContactMapper  联系人 Mapper
     * @param customerTagRelMapper   客户标签关联 Mapper
     * @param opportunityMapper      商机 Mapper（关联商机数与删除保护）
     */
    public CustomerServiceImpl(CustomerMapper customerMapper,
                               CustomerContactMapper customerContactMapper,
                               CustomerTagRelMapper customerTagRelMapper,
                               CrmOpportunityMapper opportunityMapper,
                               MessageNotifier messageNotifier) {
        this.customerMapper = customerMapper;
        this.customerContactMapper = customerContactMapper;
        this.customerTagRelMapper = customerTagRelMapper;
        this.opportunityMapper = opportunityMapper;
        this.messageNotifier = messageNotifier;
    }

    @Override
    public CustomerDto create(CustomerRequest request) {
        validate(request);
        String companyNameNorm = normalize(request.getCompanyName());
        ensureNotDuplicate(request.getCountryCode(), companyNameNorm, null);

        CustomerEntity entity = new CustomerEntity();
        entity.setId(UUID.randomUUID().toString());
        applyRequest(entity, request);
        entity.setCompanyNameNorm(companyNameNorm);
        entity.setCustomerStatus(request.getCustomerStatus() == null
                ? "POTENTIAL" : request.getCustomerStatus());
        entity.setDeleted(0L);
        customerMapper.insert(entity);
        return toDto(entity);
    }

    @Override
    public CustomerDto update(String id, CustomerRequest request) {
        validate(request);
        CustomerEntity existing = requireCustomer(id);
        String companyNameNorm = normalize(request.getCompanyName());
        ensureNotDuplicate(request.getCountryCode(), companyNameNorm, id);

        applyRequest(existing, request);
        existing.setCompanyNameNorm(companyNameNorm);
        customerMapper.update(existing);
        return toDto(existing);
    }

    @Override
    public CustomerDto getById(String id) {
        CustomerEntity entity = requireCustomer(id);
        CustomerDto dto = toDto(entity);
        dto.setContactCount(customerContactMapper.countByCustomerId(id));
        dto.setOpportunityCount(opportunityMapper.countByCustomer(id));
        return dto;
    }

    @Override
    public void delete(String id) {
        requireCustomer(id);
        // 有关联商机时拒绝删除（需求文档 §4.2）：商机是客户的业务凭据，不做静默级联
        long opportunityCount = opportunityMapper.countByCustomer(id);
        if (opportunityCount > 0) {
            throw new IllegalArgumentException("customer has related opportunities: " + opportunityCount);
        }
        // 先清关联再删主体（无物理外键，级联要自己管）
        customerContactMapper.deleteByCustomerId(id);
        customerTagRelMapper.deleteByCustomerId(id);
        customerMapper.softDelete(id, System.currentTimeMillis());
    }

    @Override
    public CustomerDto transfer(String id, String newOwnerId) {
        CustomerEntity existing = requireCustomer(id);
        if (newOwnerId == null || newOwnerId.trim().isEmpty()) {
            throw new IllegalArgumentException("new owner id must not be blank");
        }
        existing.setOwnerId(newOwnerId);
        customerMapper.update(existing);
        messageNotifier.notifyOwner(MessageType.CUSTOMER_ASSIGNED, MessageBizType.CUSTOMER.name(),
                existing.getId(), existing.getCompanyName(), newOwnerId, new HashMap<>());
        return toDto(existing);
    }

    @Override
    public PageResponse<CustomerDto> list(CustomerQueryRequest query) {
        CustomerQueryRequest actual = query == null ? new CustomerQueryRequest() : query;
        int page = actual.getPage() == null || actual.getPage() < 1 ? 1 : actual.getPage();
        int size = actual.getSize() == null || actual.getSize() < 1
                ? DEFAULT_PAGE_SIZE : Math.min(actual.getSize(), MAX_PAGE_SIZE);
        if ((long) page * size > EXPORT_LIMIT) {
            throw new IllegalArgumentException("page * size must not exceed " + EXPORT_LIMIT);
        }
        PageHelper.startPage(page, size);
        PageInfo<CustomerEntity> info = new PageInfo<>(customerMapper.selectPage(actual));

        List<CustomerDto> rows = new ArrayList<>(info.getList().size());
        for (CustomerEntity entity : info.getList()) {
            rows.add(toDto(entity));
        }
        PageResponse<CustomerDto> response = new PageResponse<>();
        response.setData(rows);
        response.setTotal(info.getTotal());
        response.setPage(info.getPageNum());
        response.setSize(info.getPageSize());
        return response;
    }

    @Override
    public List<CustomerDto> exportList(CustomerQueryRequest query) {
        List<CustomerEntity> entities = customerMapper.selectForExport(
                query == null ? new CustomerQueryRequest() : query);
        List<CustomerDto> rows = new ArrayList<>(entities.size());
        for (CustomerEntity entity : entities) {
            rows.add(toDto(entity));
        }
        return rows;
    }

    private void validate(CustomerRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("customer request must not be null");
        }
        requireText(request.getCompanyName(), "companyName");
        requireText(request.getCountryCode(), "countryCode");
        requireText(request.getSource(), "source");
        requireText(request.getOwnerId(), "ownerId");
    }

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    private void ensureNotDuplicate(String countryCode, String companyNameNorm, String excludeId) {
        CustomerEntity existing = customerMapper.selectByCompanyNorm(countryCode, companyNameNorm, excludeId);
        if (existing != null) {
            throw new IllegalArgumentException(
                    "customer already exists: " + countryCode + " / " + companyNameNorm);
        }
    }

    private CustomerEntity requireCustomer(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("customer id must not be blank");
        }
        CustomerEntity entity = customerMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("customer not found: " + id);
        }
        return entity;
    }

    private void applyRequest(CustomerEntity entity, CustomerRequest request) {
        entity.setCompanyName(request.getCompanyName());
        entity.setShortName(request.getShortName());
        entity.setCountryCode(request.getCountryCode());
        entity.setProvince(request.getProvince());
        entity.setCity(request.getCity());
        entity.setAddress(request.getAddress());
        entity.setWebsite(request.getWebsite());
        entity.setIndustry(request.getIndustry());
        entity.setSource(request.getSource());
        entity.setLevel(request.getLevel());
        entity.setOwnerId(request.getOwnerId());
        entity.setRemark(request.getRemark());
    }

    /**
     * 归一化公司名：去掉全部空白并转大写，作为去重键的一部分.
     *
     * @param companyName 原始公司名
     * @return 归一化结果
     */
    private String normalize(String companyName) {
        return companyName.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private CustomerDto toDto(CustomerEntity entity) {
        CustomerDto dto = new CustomerDto();
        dto.setId(entity.getId());
        dto.setCompanyName(entity.getCompanyName());
        dto.setShortName(entity.getShortName());
        dto.setCountryCode(entity.getCountryCode());
        dto.setProvince(entity.getProvince());
        dto.setCity(entity.getCity());
        dto.setAddress(entity.getAddress());
        dto.setWebsite(entity.getWebsite());
        dto.setIndustry(entity.getIndustry());
        dto.setSource(entity.getSource());
        dto.setLevel(entity.getLevel());
        dto.setCustomerStatus(entity.getCustomerStatus());
        dto.setOwnerId(entity.getOwnerId());
        dto.setRemark(entity.getRemark());
        return dto;
    }
}
