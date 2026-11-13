/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yaowenltd.projectinfomationmanage.crm.config.CrmCurrencyProperties;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmCountryRefMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CurrencyRateMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.country.CrmCountryInfoDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.country.CrmCountryRefDto;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmCountryRefEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmCurrencyRateEntity;
import com.yaowenltd.projectinfomationmanage.crm.service.CountryReferenceService;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * CRM 国家参考服务实现.
 * <p>
 * 国家 → 时区 / 主要货币来自参考表 {@code t_country_ref}；
 * 汇率取本地汇率表里**最近一次成功汇率**（基准币 USD），再三角折算出对人民币的汇率。
 * 汇率缺失时返回 {@code rateMissing = true}，其余字段照常返回 —— 业务不中断。
 *
 * @since 2026-11-09
 */
@Service
public class CountryReferenceServiceImpl implements CountryReferenceService {

    /** 列表默认每页条数. */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 列表最大每页条数. */
    private static final int MAX_PAGE_SIZE = 200;

    /** 人民币币种码. */
    private static final String CURRENCY_CNY = "CNY";

    /** 北京时区（用于算时差）. */
    private static final String ZONE_BEIJING = "Asia/Shanghai";

    /** 汇率小数位. */
    private static final int RATE_SCALE = 8;

    /** 时差（小时）小数位. */
    private static final int HOURS_SCALE = 2;

    /** 可读时间格式. */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CrmCountryRefMapper countryRefMapper;
    private final CurrencyRateMapper currencyRateMapper;
    private final CrmCurrencyProperties currencyProperties;

    /**
     * 使用指定依赖构造 CountryReferenceServiceImpl.
     *
     * @param countryRefMapper    国家参考数据 Mapper
     * @param currencyRateMapper  汇率 Mapper
     * @param currencyProperties  汇率配置
     */
    public CountryReferenceServiceImpl(CrmCountryRefMapper countryRefMapper,
                                       CurrencyRateMapper currencyRateMapper,
                                       CrmCurrencyProperties currencyProperties) {
        this.countryRefMapper = countryRefMapper;
        this.currencyRateMapper = currencyRateMapper;
        this.currencyProperties = currencyProperties;
    }

    @Override
    public PageResponse<CrmCountryRefDto> list(String keyword, Integer page, Integer size) {
        int pageNo = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        String actual = keyword == null ? null : keyword.trim();
        if (actual != null && actual.isEmpty()) {
            actual = null;
        }
        PageHelper.startPage(pageNo, pageSize);
        try {
            PageInfo<CrmCountryRefEntity> info = new PageInfo<>(countryRefMapper.selectPage(actual));
            List<CrmCountryRefDto> rows = new ArrayList<>(info.getList().size());
            for (CrmCountryRefEntity entity : info.getList()) {
                rows.add(toRefDto(entity));
            }
            PageResponse<CrmCountryRefDto> response = new PageResponse<>();
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
    public CrmCountryInfoDto getInfo(String country) {
        if (country == null || country.trim().isEmpty()) {
            throw new IllegalArgumentException("country must not be blank");
        }
        String keyword = country.trim();
        CrmCountryRefEntity entity = countryRefMapper.selectByKeyword(keyword);
        if (entity == null) {
            throw new IllegalArgumentException("country not found: " + keyword);
        }
        CrmCountryInfoDto dto = new CrmCountryInfoDto();
        dto.setCountryCode(entity.getCountryCode());
        dto.setCountryName(entity.getCountryName());
        dto.setCountryNameZh(entity.getCountryNameZh());
        dto.setCountryAliases(entity.getCountryAliases());
        dto.setTimezone(entity.getTimezone());
        dto.setTimezones(entity.getTimezones());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setCurrencyName(entity.getCurrencyName());
        dto.setCurrencySymbol(entity.getCurrencySymbol());

        fillLocalTime(dto, entity.getTimezone());
        fillRates(dto, entity.getCurrencyCode());
        return dto;
    }

    /**
     * 填充该国当前时间与时差.
     *
     * @param dto      结果 DTO
     * @param timezone IANA 时区名
     */
    private void fillLocalTime(CrmCountryInfoDto dto, String timezone) {
        ZoneId zone;
        try {
            zone = ZoneId.of(timezone);
        } catch (DateTimeException e) {
            throw new IllegalArgumentException("unknown timezone: " + timezone);
        }
        ZonedDateTime now = ZonedDateTime.now(zone);
        dto.setCurrentTime(now.toOffsetDateTime().toString());
        dto.setCurrentTimeText(now.format(TIME_FORMATTER));
        dto.setUtcOffset(now.getOffset().getId().equals("Z") ? "+00:00" : now.getOffset().getId());

        ZoneId beijing = ZoneId.of(ZONE_BEIJING);
        int offsetSeconds = zone.getRules().getOffset(now.toInstant()).getTotalSeconds()
                - beijing.getRules().getOffset(now.toInstant()).getTotalSeconds();
        BigDecimal hours = BigDecimal.valueOf(offsetSeconds)
                .divide(BigDecimal.valueOf(3600), HOURS_SCALE, RoundingMode.HALF_UP);
        dto.setHoursFromBeijing(hours);
        dto.setBeijingOffsetText(offsetText(hours));
    }

    /**
     * 生成时差可读文本.
     *
     * @param hours 与北京时间的时差（小时）
     * @return 可读文本
     */
    private String offsetText(BigDecimal hours) {
        int signum = hours.signum();
        if (signum == 0) {
            return "与北京时间相同";
        }
        String magnitude = hours.abs().stripTrailingZeros().toPlainString();
        return signum > 0
                ? "比北京时间早 " + magnitude + " 小时"
                : "比北京时间晚 " + magnitude + " 小时";
    }

    /**
     * 填充该国货币对美元 / 人民币的汇率.
     * <p>
     * 本地汇率表以 USD 为基准，因此：
     * <ul>
     *   <li>1 美元 = rate(USD→X) 本币；</li>
     *   <li>1 本币 = 1 / rate(USD→X) 美元；</li>
     *   <li>1 人民币 = rate(USD→X) / rate(USD→CNY) 本币；</li>
     *   <li>1 本币 = rate(USD→CNY) / rate(USD→X) 人民币。</li>
     * </ul>
     *
     * @param dto           结果 DTO
     * @param currencyCode  该国主要货币
     */
    private void fillRates(CrmCountryInfoDto dto, String currencyCode) {
        String base = currencyProperties.getBaseCurrency();
        CrmCurrencyRateEntity localRate = null;
        BigDecimal usdToLocal = null;
        if (base.equalsIgnoreCase(currencyCode)) {
            usdToLocal = BigDecimal.ONE;
        } else {
            localRate = currencyRateMapper.selectLatest(base, currencyCode);
            if (localRate != null && localRate.getRate() != null) {
                usdToLocal = localRate.getRate();
            }
        }

        BigDecimal usdToCny = null;
        CrmCurrencyRateEntity cnyRate = null;
        if (base.equalsIgnoreCase(CURRENCY_CNY)) {
            usdToCny = BigDecimal.ONE;
        } else {
            cnyRate = currencyRateMapper.selectLatest(base, CURRENCY_CNY);
            if (cnyRate != null && cnyRate.getRate() != null) {
                usdToCny = cnyRate.getRate();
            }
        }

        if (localRate != null && localRate.getRateDate() != null) {
            dto.setRateDate(localRate.getRateDate().toString());
        } else if (cnyRate != null && cnyRate.getRateDate() != null) {
            dto.setRateDate(cnyRate.getRateDate().toString());
        }
        dto.setRateMissing(usdToLocal == null || usdToCny == null);
        if (usdToLocal == null || usdToCny == null) {
            return;
        }

        dto.setRateFromUsd(scale(usdToLocal));
        dto.setRateToUsd(scale(BigDecimal.ONE.divide(usdToLocal, RATE_SCALE + 4, RoundingMode.HALF_UP)));
        dto.setRateFromCny(scale(usdToLocal.divide(usdToCny, RATE_SCALE + 4, RoundingMode.HALF_UP)));
        dto.setRateToCny(scale(usdToCny.divide(usdToLocal, RATE_SCALE + 4, RoundingMode.HALF_UP)));
    }

    /**
     * 统一汇率小数位.
     *
     * @param value 汇率
     * @return 处理后的汇率
     */
    private BigDecimal scale(BigDecimal value) {
        return value.setScale(RATE_SCALE, RoundingMode.HALF_UP);
    }

    private CrmCountryRefDto toRefDto(CrmCountryRefEntity entity) {
        CrmCountryRefDto dto = new CrmCountryRefDto();
        dto.setCountryCode(entity.getCountryCode());
        dto.setCountryName(entity.getCountryName());
        dto.setCountryNameZh(entity.getCountryNameZh());
        dto.setCountryAliases(entity.getCountryAliases());
        dto.setTimezone(entity.getTimezone());
        dto.setTimezones(entity.getTimezones());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setCurrencyName(entity.getCurrencyName());
        dto.setCurrencySymbol(entity.getCurrencySymbol());
        dto.setIsCommon(entity.getIsCommon());
        return dto;
    }

}
