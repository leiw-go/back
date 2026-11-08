/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerContactDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.customer.CustomerContactRequest;
import com.yaowenltd.projectinfomationmanage.common.E164PhoneValidator;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerContactMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerMapper;
import com.yaowenltd.projectinfomationmanage.crm.service.CustomerContactService;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CustomerContactEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CustomerEntity;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 客户联系人服务实现.
 *
 * @since 2026-11-08
 */
@Service
public class CustomerContactServiceImpl implements CustomerContactService {

    private final CustomerContactMapper customerContactMapper;
    private final CustomerMapper customerMapper;

    /**
     * 使用指定的 Mapper 构造 CustomerContactServiceImpl.
     *
     * @param customerContactMapper 联系人 Mapper
     * @param customerMapper        客户 Mapper
     */
    public CustomerContactServiceImpl(CustomerContactMapper customerContactMapper,
                                      CustomerMapper customerMapper) {
        this.customerContactMapper = customerContactMapper;
        this.customerMapper = customerMapper;
    }

    @Override
    public CustomerContactDto create(String customerId, CustomerContactRequest request) {
        requireCustomer(customerId);
        validate(request);
        ensureEmailUnique(customerId, request.getEmail(), null);
        ensurePhoneUnique(customerId, request.getPhone(), null);

        CustomerContactEntity entity = new CustomerContactEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setCustomerId(customerId);
        applyRequest(entity, request);
        entity.setDeleted(0L);
        customerContactMapper.insert(entity);
        return toDto(entity);
    }

    @Override
    public CustomerContactDto update(String contactId, CustomerContactRequest request) {
        validate(request);
        CustomerContactEntity existing = requireContact(contactId);
        ensureEmailUnique(existing.getCustomerId(), request.getEmail(), contactId);
        ensurePhoneUnique(existing.getCustomerId(), request.getPhone(), contactId);

        applyRequest(existing, request);
        customerContactMapper.update(existing);
        return toDto(existing);
    }

    @Override
    public CustomerContactDto getById(String contactId) {
        return toDto(requireContact(contactId));
    }

    @Override
    public void delete(String contactId) {
        requireContact(contactId);
        customerContactMapper.softDelete(contactId, System.currentTimeMillis());
    }

    @Override
    public List<CustomerContactDto> listByCustomer(String customerId) {
        requireCustomer(customerId);
        List<CustomerContactEntity> entities = customerContactMapper.selectByCustomerId(customerId);
        List<CustomerContactDto> rows = new ArrayList<>(entities.size());
        for (CustomerContactEntity entity : entities) {
            rows.add(toDto(entity));
        }
        return rows;
    }

    private void validate(CustomerContactRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("contact request must not be null");
        }
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            E164PhoneValidator.validate(request.getPhone());
        }
        if (request.getWhatsapp() != null && !request.getWhatsapp().trim().isEmpty()) {
            E164PhoneValidator.validate(request.getWhatsapp());
        }
    }

    private void ensureEmailUnique(String customerId, String email, String excludeId) {
        if (email == null || email.trim().isEmpty()) {
            return;
        }
        CustomerContactEntity existing = customerContactMapper.selectByEmail(customerId, email);
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw new IllegalArgumentException("contact email already exists under this customer: " + email);
        }
    }

    private void ensurePhoneUnique(String customerId, String phone, String excludeId) {
        if (phone == null || phone.trim().isEmpty()) {
            return;
        }
        CustomerContactEntity existing = customerContactMapper.selectByPhone(customerId, phone);
        if (existing != null && !existing.getId().equals(excludeId)) {
            throw new IllegalArgumentException("contact phone already exists under this customer: " + phone);
        }
    }

    private CustomerEntity requireCustomer(String customerId) {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("customer id must not be blank");
        }
        CustomerEntity entity = customerMapper.selectById(customerId);
        if (entity == null) {
            throw new IllegalArgumentException("customer not found: " + customerId);
        }
        return entity;
    }

    private CustomerContactEntity requireContact(String contactId) {
        if (contactId == null || contactId.trim().isEmpty()) {
            throw new IllegalArgumentException("contact id must not be blank");
        }
        CustomerContactEntity entity = customerContactMapper.selectById(contactId);
        if (entity == null) {
            throw new IllegalArgumentException("contact not found: " + contactId);
        }
        return entity;
    }

    private void applyRequest(CustomerContactEntity entity, CustomerContactRequest request) {
        entity.setName(request.getName());
        entity.setPosition(request.getPosition());
        entity.setEmail(request.getEmail());
        entity.setPhone(request.getPhone());
        entity.setWhatsapp(request.getWhatsapp());
        entity.setLanguage(request.getLanguage());
        entity.setTimezone(request.getTimezone());
        entity.setIsPrimary(request.getIsPrimary());
        entity.setRemark(request.getRemark());
    }

    private CustomerContactDto toDto(CustomerContactEntity entity) {
        CustomerContactDto dto = new CustomerContactDto();
        dto.setId(entity.getId());
        dto.setCustomerId(entity.getCustomerId());
        dto.setName(entity.getName());
        dto.setPosition(entity.getPosition());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setWhatsapp(entity.getWhatsapp());
        dto.setLanguage(entity.getLanguage());
        dto.setTimezone(entity.getTimezone());
        dto.setIsPrimary(entity.getIsPrimary());
        dto.setRemark(entity.getRemark());
        return dto;
    }
}
