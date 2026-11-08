/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing.PricedItem;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing.PricingItemRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing.ProductPricingInfo;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing.QuotationPricingRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.pricing.QuotationPricingResult;
import com.yaowenltd.projectinfomationmanage.crm.service.impl.QuotationPricingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link QuotationPricingService} 的纯 JUnit 5 单元测试。
 * <p>
 * 契约来自需求文档 §3.5.1：
 * 行单价 = 成本单价 × (1 + 加价率/100) × (1 - 折扣率/100)，行金额四舍五入到 2 位；
 * 单头汇总 total_amount / total_cost / profit_amount / profit_rate；
 * 成本价缺失、成本币种与报价单币种不符、MOQ 不足、产品停售都必须被拒绝；
 * 提交时服务端重算，不信任前端传来的单价与金额。
 * </p>
 */
class QuotationPricingServiceTests {

    private QuotationPricingService pricingService;

    @BeforeEach
    void setUp() {
        pricingService = new QuotationPricingServiceImpl();
    }

    private static ProductPricingInfo product(String id, String cost, String costCurrency, Integer moq, boolean onSale) {
        ProductPricingInfo info = new ProductPricingInfo();
        info.setProductId(id);
        info.setCostPrice(cost == null ? null : new BigDecimal(cost));
        info.setCostCurrency(costCurrency);
        info.setMoq(moq);
        info.setOnSale(onSale);
        return info;
    }

    private static PricingItemRequest item(ProductPricingInfo product, String quantity) {
        PricingItemRequest request = new PricingItemRequest();
        request.setProduct(product);
        request.setQuantity(new BigDecimal(quantity));
        return request;
    }

    private static QuotationPricingRequest request(String currency, String markup, String discount,
                                                   PricingItemRequest... items) {
        QuotationPricingRequest request = new QuotationPricingRequest();
        request.setCurrencyCode(currency);
        request.setMarkupRate(markup == null ? null : new BigDecimal(markup));
        request.setDiscountRate(discount == null ? null : new BigDecimal(discount));
        request.setItems(Arrays.asList(items));
        return request;
    }

    @Nested
    @DisplayName("单价计算")
    class UnitPriceCalculation {

        @Test
        @DisplayName("无折扣时 单价 = 成本价 × (1 + 加价率)")
        void testUnitPriceWithoutDiscount() {
            QuotationPricingResult result = pricingService.calculate(
                    request("USD", "20", "0",
                            item(product("P1", "100", "USD", null, true), "2")));

            PricedItem priced = result.getItems().get(0);
            assertEquals(0, new BigDecimal("120.000000").compareTo(priced.getUnitPrice()),
                    "unit price should be 100 * 1.2 = 120 but was " + priced.getUnitPrice());
            assertEquals(0, new BigDecimal("240.00").compareTo(priced.getAmount()),
                    "amount should be 120 * 2 = 240 but was " + priced.getAmount());
            assertFalse(priced.isManualPrice(), "line should not be flagged as manual price");
        }

        @Test
        @DisplayName("折扣在加价之后生效 单价 = 成本价 × (1 + 加价率) × (1 - 折扣率)")
        void testUnitPriceWithDiscount() {
            QuotationPricingResult result = pricingService.calculate(
                    request("USD", "20", "10",
                            item(product("P1", "100", "USD", null, true), "1")));

            assertEquals(0, new BigDecimal("108.000000").compareTo(result.getItems().get(0).getUnitPrice()),
                    "unit price should be 100 * 1.2 * 0.9 = 108");
        }

        @Test
        @DisplayName("明细行覆盖加价率时使用行级值")
        void testItemOverridesMarkupRate() {
            PricingItemRequest item = item(product("P1", "100", "USD", null, true), "1");
            item.setMarkupRate(new BigDecimal("50"));

            QuotationPricingResult result = pricingService.calculate(request("USD", "20", "0", item));

            assertEquals(0, new BigDecimal("150.000000").compareTo(result.getItems().get(0).getUnitPrice()),
                    "line level markup rate should win over quotation level");
            assertEquals(0, new BigDecimal("50").compareTo(result.getItems().get(0).getMarkupRate()),
                    "effective markup rate should be recorded on the line");
        }

        @Test
        @DisplayName("明细行覆盖折扣率时使用行级值")
        void testItemOverridesDiscountRate() {
            PricingItemRequest item = item(product("P1", "100", "USD", null, true), "1");
            item.setDiscountRate(new BigDecimal("25"));

            QuotationPricingResult result = pricingService.calculate(request("USD", "0", "0", item));

            assertEquals(0, new BigDecimal("75.000000").compareTo(result.getItems().get(0).getUnitPrice()),
                    "line level discount rate should win over quotation level");
        }

        @Test
        @DisplayName("手工改价时不套加价率与折扣")
        void testManualPriceIgnoresMarkupAndDiscount() {
            PricingItemRequest item = item(product("P1", "100", "USD", null, true), "2");
            item.setManualUnitPrice(new BigDecimal("99"));

            QuotationPricingResult result = pricingService.calculate(request("USD", "20", "10", item));

            PricedItem priced = result.getItems().get(0);
            assertEquals(0, new BigDecimal("99.000000").compareTo(priced.getUnitPrice()),
                    "manual unit price should be kept as is");
            assertTrue(priced.isManualPrice(), "line should be flagged as manual price");
            assertEquals(0, new BigDecimal("198.00").compareTo(priced.getAmount()),
                    "amount should still be unit price * quantity");
        }

        @Test
        @DisplayName("手工改价不影响成本汇总")
        void testManualPriceDoesNotChangeTotalCost() {
            PricingItemRequest item = item(product("P1", "100", "USD", null, true), "2");
            item.setManualUnitPrice(new BigDecimal("99"));

            QuotationPricingResult result = pricingService.calculate(request("USD", "20", "0", item));

            assertEquals(0, new BigDecimal("200.00").compareTo(result.getTotalCost()),
                    "total cost should still be computed from cost price");
        }

        @Test
        @DisplayName("单价保留 6 位小数 金额四舍五入到 2 位")
        void testRoundingPrecision() {
            QuotationPricingResult result = pricingService.calculate(
                    request("USD", "0", "0",
                            item(product("P1", "0.333333", "USD", null, true), "3")));

            PricedItem priced = result.getItems().get(0);
            assertEquals(0, new BigDecimal("0.333333").compareTo(priced.getUnitPrice()),
                    "unit price should keep 6 decimal places");
            assertEquals(0, new BigDecimal("1.00").compareTo(priced.getAmount()),
                    "amount should be rounded to 2 decimals, 0.999999 -> 1.00");
        }
    }

    @Nested
    @DisplayName("单头汇总")
    class Summary {

        @Test
        @DisplayName("总额 成本 毛利 毛利率应与明细一致")
        void testSummaryIsConsistentWithLines() {
            QuotationPricingResult result = pricingService.calculate(
                    request("USD", "20", "0",
                            item(product("P1", "100", "USD", null, true), "2"),
                            item(product("P2", "50", "USD", null, true), "3")));

            assertEquals(0, new BigDecimal("465.00").compareTo(result.getTotalAmount()),
                    "total amount = 240 + 225 = 465");
            assertEquals(0, new BigDecimal("350.00").compareTo(result.getTotalCost()),
                    "total cost = 200 + 150 = 350");
            assertEquals(0, new BigDecimal("115.00").compareTo(result.getProfitAmount()),
                    "profit = 465 - 350 = 115");
            assertEquals(0, new BigDecimal("24.73").compareTo(result.getProfitRate()),
                    "profit rate = 115 / 465 * 100 = 24.73");
        }

        @Test
        @DisplayName("总额为 0 时毛利率为 0 不应除零")
        void testProfitRateIsZeroWhenTotalAmountIsZero() {
            QuotationPricingResult result = pricingService.calculate(
                    request("USD", "0", "0",
                            item(product("P1", "0", "USD", null, true), "0")));

            assertEquals(0, BigDecimal.ZERO.compareTo(result.getProfitRate()),
                    "profit rate should be 0 when total amount is 0");
        }

        @Test
        @DisplayName("汇总结果应保留全部明细")
        void testSummaryKeepsEveryLine() {
            QuotationPricingResult result = pricingService.calculate(
                    request("USD", "10", "0",
                            item(product("P1", "100", "USD", null, true), "1"),
                            item(product("P2", "200", "USD", null, true), "1"),
                            item(product("P3", "300", "USD", null, true), "1")));

            assertEquals(3, result.getItems().size(), "every line should be returned");
        }
    }

    @Nested
    @DisplayName("校验与拦截")
    class Validation {

        @Test
        @DisplayName("请求为 null 应被拒绝")
        void testNullRequestIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> pricingService.calculate(null));
        }

        @Test
        @DisplayName("明细为空 应被拒绝")
        void testEmptyItemsIsRejected() {
            QuotationPricingRequest request = request("USD", "0", "0");
            request.setItems(Collections.emptyList());

            assertThrows(IllegalArgumentException.class, () -> pricingService.calculate(request));
        }

        @Test
        @DisplayName("明细为 null 应被拒绝")
        void testNullItemsIsRejected() {
            QuotationPricingRequest request = request("USD", "0", "0");
            request.setItems(null);

            assertThrows(IllegalArgumentException.class, () -> pricingService.calculate(request));
        }

        @Test
        @DisplayName("报价单币种为空 应被拒绝")
        void testMissingCurrencyIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> pricingService.calculate(
                    request(null, "0", "0", item(product("P1", "100", "USD", null, true), "1"))));
        }

        @Test
        @DisplayName("成本价缺失的产品不能参与报价")
        void testMissingCostPriceIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> pricingService.calculate(
                    request("USD", "0", "0", item(product("P1", null, "USD", null, true), "1"))));
        }

        @Test
        @DisplayName("成本币种与报价单币种不一致 应被拒绝")
        void testCostCurrencyMismatchIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> pricingService.calculate(
                    request("USD", "0", "0", item(product("P1", "100", "EUR", null, true), "1"))));
        }

        @Test
        @DisplayName("成本币种与报价单币种一致 应通过")
        void testCostCurrencyMatchIsAccepted() {
            QuotationPricingResult result = pricingService.calculate(
                    request("EUR", "0", "0", item(product("P1", "100", "EUR", null, true), "1")));

            assertEquals(1, result.getItems().size());
        }

        @Test
        @DisplayName("数量低于 MOQ 应被拒绝")
        void testQuantityBelowMoqIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> pricingService.calculate(
                    request("USD", "0", "0", item(product("P1", "100", "USD", 100, true), "99"))));
        }

        @Test
        @DisplayName("数量等于 MOQ 应通过")
        void testQuantityEqualsMoqIsAccepted() {
            QuotationPricingResult result = pricingService.calculate(
                    request("USD", "0", "0", item(product("P1", "100", "USD", 100, true), "100")));

            assertEquals(1, result.getItems().size());
        }

        @Test
        @DisplayName("MOQ 未维护时不校验数量下限")
        void testMoqNullSkipsMoqCheck() {
            QuotationPricingResult result = pricingService.calculate(
                    request("USD", "0", "0", item(product("P1", "100", "USD", null, true), "1")));

            assertEquals(1, result.getItems().size());
        }

        @Test
        @DisplayName("停售产品不能参与报价")
        void testOffShelfProductIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> pricingService.calculate(
                    request("USD", "0", "0", item(product("P1", "100", "USD", null, false), "1"))));
        }

        @Test
        @DisplayName("数量为 0 或负数 应被拒绝")
        void testNonPositiveQuantityIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> pricingService.calculate(
                    request("USD", "0", "0", item(product("P1", "100", "USD", null, true), "0"))));
            assertThrows(IllegalArgumentException.class, () -> pricingService.calculate(
                    request("USD", "0", "0", item(product("P1", "100", "USD", null, true), "-1"))));
        }

        @Test
        @DisplayName("明细缺少产品信息 应被拒绝")
        void testMissingProductInfoIsRejected() {
            PricingItemRequest item = new PricingItemRequest();
            item.setQuantity(BigDecimal.ONE);

            assertThrows(IllegalArgumentException.class, () -> pricingService.calculate(
                    request("USD", "0", "0", item)));
        }

        @Test
        @DisplayName("加价率超出 0~1000 应被拒绝")
        void testMarkupRateOutOfRangeIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> pricingService.calculate(
                    request("USD", "1001", "0", item(product("P1", "100", "USD", null, true), "1"))));
            assertThrows(IllegalArgumentException.class, () -> pricingService.calculate(
                    request("USD", "-1", "0", item(product("P1", "100", "USD", null, true), "1"))));
        }

        @Test
        @DisplayName("折扣率超出 0~100 应被拒绝")
        void testDiscountRateOutOfRangeIsRejected() {
            assertThrows(IllegalArgumentException.class, () -> pricingService.calculate(
                    request("USD", "0", "101", item(product("P1", "100", "USD", null, true), "1"))));
            assertThrows(IllegalArgumentException.class, () -> pricingService.calculate(
                    request("USD", "0", "-1", item(product("P1", "100", "USD", null, true), "1"))));
        }

        @Test
        @DisplayName("加价率与折扣率边界值应被接受")
        void testBoundaryRatesAreAccepted() {
            QuotationPricingResult result = pricingService.calculate(
                    request("USD", "1000", "100", item(product("P1", "100", "USD", null, true), "1")));

            assertEquals(0, new BigDecimal("0.000000").compareTo(result.getItems().get(0).getUnitPrice()),
                    "100 * 11 * 0 = 0");
        }

        @Test
        @DisplayName("折扣率为 100 时单价为 0")
        void testFullDiscountYieldsZeroUnitPrice() {
            QuotationPricingResult result = pricingService.calculate(
                    request("USD", "0", "100", item(product("P1", "100", "USD", null, true), "1")));

            assertEquals(0, BigDecimal.ZERO.compareTo(result.getItems().get(0).getUnitPrice()));
        }
    }

    @Nested
    @DisplayName("服务端重算")
    class ServerSideRecalculation {

        @Test
        @DisplayName("同一请求两次算价结果应完全一致")
        void testCalculationIsDeterministic() {
            QuotationPricingRequest request = request("USD", "17.5", "3",
                    item(product("P1", "12.34", "USD", null, true), "7"));

            QuotationPricingResult first = pricingService.calculate(request);
            QuotationPricingResult second = pricingService.calculate(request);

            assertEquals(0, first.getTotalAmount().compareTo(second.getTotalAmount()),
                    "pricing must be deterministic so the server can safely recompute");
            assertEquals(0, first.getItems().get(0).getUnitPrice()
                    .compareTo(second.getItems().get(0).getUnitPrice()));
        }

        @Test
        @DisplayName("改数量后重算结果应随之变化")
        void testChangingQuantityChangesAmount() {
            List<PricingItemRequest> items = new ArrayList<>();
            items.add(item(product("P1", "100", "USD", null, true), "1"));

            QuotationPricingRequest request = request("USD", "0", "0");
            request.setItems(items);
            QuotationPricingResult before = pricingService.calculate(request);

            items.get(0).setQuantity(new BigDecimal("2"));
            QuotationPricingResult after = pricingService.calculate(request);

            assertEquals(0, new BigDecimal("100.00").compareTo(before.getTotalAmount()));
            assertEquals(0, new BigDecimal("200.00").compareTo(after.getTotalAmount()));
        }
    }
}
