/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.opportunity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * CRM 销售漏斗（按当前 stage 快照统计）.
 *
 * @since 2026-11-08
 */
@Data
public class CrmOpportunityPipelineDto {

    /**
     * 各阶段明细，按漏斗顺序排列.
     */
    private List<StageBucket> stages = new ArrayList<>();

    /**
     * 赢单率（%）：WON 数 ÷ (WON + LOST) 数 × 100，分母为 0 时取 0.
     */
    private BigDecimal winRate = BigDecimal.ZERO;

    /**
     * 漏斗中的商机总数（不含 LOST）.
     */
    private long totalCount;

    /**
     * 漏斗中的商机金额合计（本位币 USD）.
     */
    private BigDecimal totalAmountBase = BigDecimal.ZERO;

    /**
     * 单个阶段的统计.
     */
    @Data
    public static class StageBucket {

        /**
         * 阶段码.
         */
        private String stage;

        /**
         * 阶段中文名.
         */
        private String stageName;

        /**
         * 该阶段的商机数.
         */
        private long count;

        /**
         * 该阶段的商机金额合计（本位币 USD）.
         */
        private BigDecimal amountBase = BigDecimal.ZERO;

        /**
         * 阶段转化率（%）：本阶段商机数 ÷ 上一阶段商机数 × 100；首阶段为 100.
         */
        private BigDecimal conversionRate = BigDecimal.ZERO;

    }

}
