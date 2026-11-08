/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing.PricedItem;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing.PricingItemRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing.ProductPricingInfo;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing.QuotationPricingRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing.QuotationPricingResult;
import com.yaowenltd.projectinfomationmanage.crm.service.QuotationPricingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 报价自动算价引擎实现.
 * <p>按需求文档 §3.5.1：
 * 行单价 = 成本单价 × (1 + 加价率/100) × (1 - 折扣率/100)，单价保留 6 位小数；
 * 行金额 = 行单价 × 数量，四舍五入到 2 位；单头汇总报价总额、成本汇总、毛利额与毛利率。
 * </p>
 *
 * @since 2026-11-08
 */
@Service
public class QuotationPricingServiceImpl implements QuotationPricingService {

    /** 单价小数位. */
    private static final int UNIT_PRICE_SCALE = 6;

    /** 金额小数位. */
    private static final int AMOUNT_SCALE = 2;

    /** 比例小数位. */
    private static final int RATE_SCALE = 2;

    /** 加价率上限（%）. */
    private static final BigDecimal MAX_MARKUP_RATE = new BigDecimal("1000");

    /** 折扣率上限（%）. */
    private static final BigDecimal MAX_DISCOUNT_RATE = new BigDecimal("100");

    @Override
    public QuotationPricingResult calculate(QuotationPricingRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("pricing request must not be null");
        }
        String currency = request.getCurrencyCode();
        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("quotation currency must not be blank");
        }
        List<PricingItemRequest> items = request.getItems();
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("quotation must contain at least one item");
        }

        BigDecimal defaultMarkup = request.getMarkupRate() == null ? BigDecimal.ZERO : request.getMarkupRate();
        BigDecimal defaultDiscount = request.getDiscountRate() == null ? BigDecimal.ZERO : request.getDiscountRate();
        validateRate(defaultMarkup, MAX_MARKUP_RATE, "markupRate");
        validateRate(defaultDiscount, MAX_DISCOUNT_RATE, "discountRate");

        List<PricedItem> pricedItems = new ArrayList<>(items.size());
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        BigDecimal totalCost = BigDecimal.ZERO.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);

        for (PricingItemRequest item : items) {
            PricedItem priced = priceItem(item, currency, defaultMarkup, defaultDiscount);
            pricedItems.add(priced);
            totalAmount = totalAmount.add(priced.getAmount());
            totalCost = totalCost.add(priced.getCostPrice()
                    .multiply(item.getQuantity()).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP));
        }

        totalCost = totalCost.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        totalAmount = totalAmount.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);

        QuotationPricingResult result = new QuotationPricingResult();
        result.setItems(pricedItems);
        result.setTotalAmount(totalAmount);
        result.setTotalCost(totalCost);
        result.setProfitAmount(totalAmount.subtract(totalCost).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP));
        result.setProfitRate(totalAmount.signum() > 0
                ? result.getProfitAmount().multiply(BigDecimal.valueOf(100))
                        .divide(totalAmount, RATE_SCALE, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(RATE_SCALE, RoundingMode.HALF_UP));
        return result;
    }

    private PricedItem priceItem(PricingItemRequest item, String currency,
                                 BigDecimal defaultMarkup, BigDecimal defaultDiscount) {
        if (item == null) {
            throw new IllegalArgumentException("quotation item must not be null");
        }
        ProductPricingInfo product = item.getProduct();
        if (product == null) {
            throw new IllegalArgumentException("quotation item must carry product information");
        }
        if (!product.isOnSale()) {
            throw new IllegalArgumentException("product is off shelf and cannot be quoted: " + product.getProductId());
        }
        if (product.getCostPrice() == null) {
            throw new IllegalArgumentException("product cost price is missing: " + product.getProductId());
        }
        if (product.getCostCurrency() == null || !product.getCostCurrency().equals(currency)) {
            throw new IllegalArgumentException("product cost currency does not match quotation currency: "
                    + product.getProductId());
        }
        if (item.getQuantity() == null || item.getQuantity().signum() <= 0) {
            throw new IllegalArgumentException("quotation item quantity must be positive: " + product.getProductId());
        }
        if (product.getMoq() != null && item.getQuantity().compareTo(new BigDecimal(product.getMoq())) < 0) {
            throw new IllegalArgumentException("quotation item quantity is below MOQ: " + product.getProductId());
        }

        BigDecimal markup = item.getMarkupRate() == null ? defaultMarkup : item.getMarkupRate();
        BigDecimal discount = item.getDiscountRate() == null ? defaultDiscount : item.getDiscountRate();
        validateRate(markup, MAX_MARKUP_RATE, "markupRate");
        validateRate(discount, MAX_DISCOUNT_RATE, "discountRate");

        BigDecimal unitPrice;
        boolean manualPrice = item.getManualUnitPrice() != null;
        if (manualPrice) {
            unitPrice = item.getManualUnitPrice().setScale(UNIT_PRICE_SCALE, RoundingMode.HALF_UP);
        } else {
            unitPrice = product.getCostPrice()
                    .multiply(rateFactor(markup, true))
                    .multiply(rateFactor(discount, false))
                    .setScale(UNIT_PRICE_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal amount = unitPrice.multiply(item.getQuantity()).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);

        PricedItem priced = new PricedItem();
        priced.setProductId(product.getProductId());
        priced.setCostPrice(product.getCostPrice());
        priced.setMarkupRate(markup);
        priced.setDiscountRate(discount);
        priced.setUnitPrice(unitPrice);
        priced.setAmount(amount);
        priced.setManualPrice(manualPrice);
        return priced;
    }

    private void validateRate(BigDecimal rate, BigDecimal max, String field) {
        if (rate == null || rate.signum() < 0 || rate.compareTo(max) > 0) {
            throw new IllegalArgumentException(field + " must be between 0 and " + max.toPlainString());
        }
    }

    private BigDecimal rateFactor(BigDecimal percent, boolean markup) {
        BigDecimal ratio = percent.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        return markup ? BigDecimal.ONE.add(ratio) : BigDecimal.ONE.subtract(ratio);
    }
}
