/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

/**
 * 应收账龄的原始行（Mapper 查询结果，Service 层再分桶）.
 * <p>
 * 分桶在 Service 层做（而不是 SQL 的 CASE），这样桶边界只有一处定义，
 * 与 {@link com.yaowenltd.projectinfomationmanage.crm.model.enums.ReceivableAgingBucket} 保持一致。
 * </p>
 *
 * @since 2026-11-08
 */
@Data
public class CrmReceivableAgingRowDto {

    /**
     * 约定收款日（{@code t_receivable.due_date}）；为空时按 0 天分桶.
     */
    private LocalDate dueDate;

    /**
     * 未回款金额（本位币 USD）：按所属订单的 {@code total_amount_base / total_amount} 推算.
     */
    private BigDecimal amountBase = BigDecimal.ZERO;

}
