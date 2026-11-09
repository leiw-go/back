/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmEmailCampaignMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmEmailLogMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmEmailTemplateMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerContactMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CustomerMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailCampaignDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailCampaignRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailLogDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailPreviewDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailTemplateDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.email.CrmEmailTemplateRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmEmailCampaignEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmEmailLogEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmEmailTemplateEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CustomerContactEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CustomerEntity;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CRM 邮件服务实现.
 *
 * @since 2026-11-08
 */
@Service
public class CrmEmailServiceImpl implements CrmEmailService {

    /** 模板变量占位符：{{xxx}}. */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{([\\w.]+)}}");

    /** 群发范围：按标签. */
    private static final String TARGET_TAG = "TAG";

    /** 群发范围：按负责人. */
    private static final String TARGET_OWNER = "OWNER";

    /** 群发范围：按客户. */
    private static final String TARGET_CUSTOMER = "CUSTOMER";

    /** 发送状态：成功（本期只落库记录）. */
    private static final String SEND_STATUS_SUCCESS = "SUCCESS";

    /** 群发任务状态：草稿. */
    private static final String CAMPAIGN_STATUS_DRAFT = "DRAFT";

    /** 群发任务状态：已发送. */
    private static final String CAMPAIGN_STATUS_SENT = "SENT";

    private static final Logger LOGGER = LoggerFactory.getLogger(CrmEmailServiceImpl.class);

    private final CrmEmailTemplateMapper templateMapper;
    private final CrmEmailCampaignMapper campaignMapper;
    private final CrmEmailLogMapper logMapper;
    private final CustomerMapper customerMapper;
    private final CustomerContactMapper customerContactMapper;

    /**
     * 使用指定的 Mapper 构造 CrmEmailServiceImpl.
     *
     * @param templateMapper        模板 Mapper
     * @param campaignMapper        群发任务 Mapper
     * @param logMapper             发送记录 Mapper
     * @param customerMapper        客户 Mapper（解析群发范围）
     * @param customerContactMapper 联系人 Mapper（取收件邮箱）
     */
    public CrmEmailServiceImpl(CrmEmailTemplateMapper templateMapper,
                                CrmEmailCampaignMapper campaignMapper,
                                CrmEmailLogMapper logMapper,
                                CustomerMapper customerMapper,
                                CustomerContactMapper customerContactMapper) {
        this.templateMapper = templateMapper;
        this.campaignMapper = campaignMapper;
        this.logMapper = logMapper;
        this.customerMapper = customerMapper;
        this.customerContactMapper = customerContactMapper;
    }

    @Override
    public List<CrmEmailTemplateDto> listTemplates() {
        List<CrmEmailTemplateEntity> entities = templateMapper.selectAll();
        List<CrmEmailTemplateDto> rows = new ArrayList<>(entities.size());
        for (CrmEmailTemplateEntity entity : entities) {
            rows.add(toTemplateDto(entity));
        }
        return rows;
    }

    @Override
    public CrmEmailTemplateDto saveTemplate(CrmEmailTemplateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("email template request must not be null");
        }
        requireText(request.getName(), "name");
        requireText(request.getSubject(), "subject");
        requireText(request.getContent(), "content");
        requireText(request.getTemplateType(), "templateType");

        if (request.getId() != null && !request.getId().trim().isEmpty()) {
            CrmEmailTemplateEntity existing = requireTemplate(request.getId());
            ensureNameNotDuplicate(request.getName(), existing.getId());
            applyRequest(existing, request);
            templateMapper.update(existing);
            return toTemplateDto(existing);
        }

        ensureNameNotDuplicate(request.getName(), null);
        CrmEmailTemplateEntity entity = new CrmEmailTemplateEntity();
        entity.setId(UUID.randomUUID().toString());
        applyRequest(entity, request);
        entity.setDeleted(0L);
        templateMapper.insert(entity);
        return toTemplateDto(entity);
    }

    @Override
    public void deleteTemplate(String id) {
        requireTemplate(id);
        templateMapper.softDelete(id, System.currentTimeMillis());
    }

    @Override
    public CrmEmailPreviewDto preview(String id, Map<String, String> placeholders) {
        CrmEmailTemplateEntity template = requireTemplate(id);
        CrmEmailPreviewDto dto = new CrmEmailPreviewDto();
        dto.setTemplateId(template.getId());
        dto.setSubject(render(template.getSubject(), placeholders, dto));
        dto.setContent(render(template.getContent(), placeholders, dto));
        return dto;
    }

    @Override
    public List<CrmEmailCampaignDto> listCampaigns(String ownerId, String status) {
        List<CrmEmailCampaignEntity> entities = campaignMapper.selectPage(ownerId, status);
        List<CrmEmailCampaignDto> rows = new ArrayList<>(entities.size());
        for (CrmEmailCampaignEntity entity : entities) {
            rows.add(toCampaignDto(entity));
        }
        return rows;
    }

    @Override
    public CrmEmailCampaignDto createCampaign(CrmEmailCampaignRequest request, String ownerId) {
        if (request == null) {
            throw new IllegalArgumentException("email campaign request must not be null");
        }
        requireText(request.getName(), "name");
        requireText(request.getTemplateId(), "templateId");
        requireText(request.getTargetType(), "targetType");
        requireText(request.getTargetValue(), "targetValue");
        requireTemplate(request.getTemplateId());
        String targetType = request.getTargetType().trim().toUpperCase(Locale.ROOT);
        if (!TARGET_TAG.equals(targetType) && !TARGET_OWNER.equals(targetType)
                && !TARGET_CUSTOMER.equals(targetType)) {
            throw new IllegalArgumentException("targetType must be TAG, OWNER or CUSTOMER");
        }

        CrmEmailCampaignEntity entity = new CrmEmailCampaignEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setName(request.getName().trim());
        entity.setTemplateId(request.getTemplateId().trim());
        entity.setTargetType(targetType);
        entity.setTargetValue(request.getTargetValue().trim());
        entity.setCampaignStatus(CAMPAIGN_STATUS_DRAFT);
        entity.setTotalCount(0);
        entity.setSuccessCount(0);
        entity.setFailCount(0);
        entity.setOwnerId(ownerId);
        entity.setDeleted(0L);
        campaignMapper.insert(entity);
        return toCampaignDto(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrmEmailCampaignDto send(String id) {
        CrmEmailCampaignEntity campaign = requireCampaign(id);
        if (CAMPAIGN_STATUS_SENT.equals(campaign.getCampaignStatus())) {
            throw new IllegalArgumentException("campaign already sent: " + id);
        }
        CrmEmailTemplateEntity template = requireTemplate(campaign.getTemplateId());

        List<CustomerEntity> customers = resolveTargets(campaign);
        List<CrmEmailLogEntity> logs = new ArrayList<>();
        Set<String> emails = new LinkedHashSet<>();
        int success = 0;
        int fail = 0;
        LocalDateTime sentAt = LocalDateTime.now();

        for (CustomerEntity customer : customers) {
            Map<String, String> placeholders = buildPlaceholders(customer);
            for (CustomerContactEntity contact : customerContactMapper.selectByCustomerId(customer.getId())) {
                String email = contact.getEmail();
                if (email == null || email.trim().isEmpty()) {
                    continue;
                }
                if (!emails.add(email)) {
                    continue;
                }
                CrmEmailLogEntity log = new CrmEmailLogEntity();
                log.setId(UUID.randomUUID().toString());
                log.setCampaignId(campaign.getId());
                log.setContactId(contact.getId());
                log.setEmail(email.trim());
                log.setSendStatus(SEND_STATUS_SUCCESS);
                log.setErrorMsg(null);
                log.setSentAt(sentAt);
                logs.add(log);
                success++;
                LOGGER.info("email record created: campaignId={}, email={}, subject={}",
                        campaign.getId(), email.trim(), render(template.getSubject(), placeholders, null));
            }
        }
        if (!logs.isEmpty()) {
            logMapper.insertBatch(logs);
        }
        fail = logs.isEmpty() && !customers.isEmpty() ? customers.size() : 0;

        campaign.setCampaignStatus(CAMPAIGN_STATUS_SENT);
        campaign.setTotalCount(logs.size());
        campaign.setSuccessCount(success);
        campaign.setFailCount(fail);
        campaign.setSentAt(sentAt);
        campaignMapper.update(campaign);
        return toCampaignDto(campaign);
    }

    @Override
    public List<CrmEmailLogDto> listLogs(String id) {
        requireCampaign(id);
        List<CrmEmailLogEntity> entities = logMapper.selectByCampaignId(id);
        List<CrmEmailLogDto> rows = new ArrayList<>(entities.size());
        for (CrmEmailLogEntity entity : entities) {
            CrmEmailLogDto dto = new CrmEmailLogDto();
            dto.setId(entity.getId());
            dto.setCampaignId(entity.getCampaignId());
            dto.setContactId(entity.getContactId());
            dto.setEmail(entity.getEmail());
            dto.setSendStatus(entity.getSendStatus());
            dto.setErrorMsg(entity.getErrorMsg());
            dto.setSentAt(entity.getSentAt());
            rows.add(dto);
        }
        return rows;
    }

    /**
     * 解析群发范围对应的客户.
     */
    private List<CustomerEntity> resolveTargets(CrmEmailCampaignEntity campaign) {
        String type = campaign.getTargetType();
        String value = campaign.getTargetValue();
        if (TARGET_OWNER.equals(type)) {
            return customerMapper.selectByOwner(value);
        }
        if (TARGET_TAG.equals(type)) {
            return customerMapper.selectByTagId(value);
        }
        if (TARGET_CUSTOMER.equals(type)) {
            CustomerEntity customer = customerMapper.selectById(value);
            return customer == null ? new ArrayList<>() : java.util.Collections.singletonList(customer);
        }
        throw new IllegalArgumentException("unsupported target type: " + type);
    }

    /**
     * 构造模板占位符上下文（客户维度）.
     */
    private Map<String, String> buildPlaceholders(CustomerEntity customer) {
        Map<String, String> placeholders = new java.util.LinkedHashMap<>();
        placeholders.put("customer.company_name", customer.getCompanyName());
        placeholders.put("customer.short_name", customer.getShortName());
        placeholders.put("customer.country_code", customer.getCountryCode());
        placeholders.put("customer.city", customer.getCity());
        placeholders.put("customer.contact_email", customer.getId());
        return placeholders;
    }

    /**
     * 渲染模板；未提供的占位符会被记录到 unresolvedPlaceholders.
     */
    private String render(String template, Map<String, String> placeholders, CrmEmailPreviewDto collector) {
        if (template == null) {
            return "";
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = placeholders == null ? null : placeholders.get(key);
            if (value == null) {
                if (collector != null) {
                    collector.getUnresolvedPlaceholders().add(key);
                }
                matcher.appendReplacement(result, Matcher.quoteReplacement(matcher.group()));
            } else {
                matcher.appendReplacement(result, Matcher.quoteReplacement(value));
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private void ensureNameNotDuplicate(String name, String excludeId) {
        CrmEmailTemplateEntity existing = templateMapper.selectByName(name.trim(), excludeId);
        if (existing != null) {
            throw new IllegalArgumentException("email template name already exists: " + name.trim());
        }
    }

    private void applyRequest(CrmEmailTemplateEntity entity, CrmEmailTemplateRequest request) {
        entity.setName(request.getName().trim());
        entity.setSubject(request.getSubject());
        entity.setContent(request.getContent());
        entity.setLanguage(request.getLanguage() == null || request.getLanguage().trim().isEmpty()
                ? "zh" : request.getLanguage().trim());
        entity.setTemplateType(request.getTemplateType().trim());
    }

    private void requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    private CrmEmailTemplateEntity requireTemplate(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("email template id must not be blank");
        }
        CrmEmailTemplateEntity entity = templateMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("email template not found: " + id);
        }
        return entity;
    }

    private CrmEmailCampaignEntity requireCampaign(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("email campaign id must not be blank");
        }
        CrmEmailCampaignEntity entity = campaignMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("email campaign not found: " + id);
        }
        return entity;
    }

    private CrmEmailTemplateDto toTemplateDto(CrmEmailTemplateEntity entity) {
        CrmEmailTemplateDto dto = new CrmEmailTemplateDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSubject(entity.getSubject());
        dto.setContent(entity.getContent());
        dto.setLanguage(entity.getLanguage());
        dto.setTemplateType(entity.getTemplateType());
        return dto;
    }

    private CrmEmailCampaignDto toCampaignDto(CrmEmailCampaignEntity entity) {
        CrmEmailCampaignDto dto = new CrmEmailCampaignDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setTemplateId(entity.getTemplateId());
        dto.setTargetType(entity.getTargetType());
        dto.setTargetValue(entity.getTargetValue());
        dto.setCampaignStatus(entity.getCampaignStatus());
        dto.setTotalCount(entity.getTotalCount());
        dto.setSuccessCount(entity.getSuccessCount());
        dto.setFailCount(entity.getFailCount());
        dto.setSentAt(entity.getSentAt());
        dto.setOwnerId(entity.getOwnerId());
        return dto;
    }

}
