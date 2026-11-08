/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service;

import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrossCheckRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.currency.CrossCheckOutcome;

/**
 * 两源汇率交叉比对与熔断判定.<p>按需求文档（汇率专项文档）§4：偏差超分级阈值即拒绝入库，连续超标触发熔断。
 *
 * @since 2026-11-08
 */
public interface CurrencyCrossCheckService {

    /**
     * 对两源汇率做交叉比对.
     *
     * @param request 比对请求
     * @return 比对结果
     */
    CrossCheckOutcome check(CrossCheckRequest request);

    /**
     * 判断某币种是否应被熔断（连续超标达到阈值天数）.
     *
     * @param consecutiveBreachDays 连续超标天数
     * @param suspectDays            熔断阈值天数
     * @return 应熔断返回 true
     */
    boolean shouldSuspend(int consecutiveBreachDays, int suspectDays);

    /**
     * 判断某币种是否应解除熔断（连续达标达到阈值天数）.
     *
     * @param consecutiveOkDays 连续达标天数
     * @param recoverDays       恢复阈值天数
     * @return 应解除返回 true
     */
    boolean shouldRecover(int consecutiveOkDays, int recoverDays);
}
