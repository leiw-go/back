/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.model.enums;

/**
 * 应收账龄分桶.
 * <p>
 * 分桶依据是「到期日（{@code t_receivable.due_date}）与今天的天数差」，
 * 也就是逾期天数：{@code 0} 表示今天到期或尚未到期（未到期的天数差为负，按 0 归入首个桶）。
 * </p>
 *
 * @since 2026-11-08
 */
public enum ReceivableAgingBucket {

    /**
     * 0-30 天（含今天到期、尚未到期）.
     */
    D_0_30("0-30", "0-30 天", 0, 30),

    /**
     * 31-60 天.
     */
    D_31_60("31-60", "31-60 天", 31, 60),

    /**
     * 61-90 天.
     */
    D_61_90("61-90", "61-90 天", 61, 90),

    /**
     * 90 天以上.
     */
    D_90_PLUS("90+", "90 天以上", 91, Integer.MAX_VALUE);

    /**
     * 桶标识（机器可读）.
     */
    private final String bucket;

    /**
     * 桶文案（展示用）.
     */
    private final String label;

    /**
     * 桶的下界（含）.
     */
    private final int minDays;

    /**
     * 桶的上界（含）.
     */
    private final int maxDays;

    ReceivableAgingBucket(String bucket, String label, int minDays, int maxDays) {
        this.bucket = bucket;
        this.label = label;
        this.minDays = minDays;
        this.maxDays = maxDays;
    }

    /**
     * 取桶标识.
     *
     * @return 桶标识
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * 取桶文案.
     *
     * @return 桶文案
     */
    public String getLabel() {
        return label;
    }

    /**
     * 取桶的下界（含）.
     *
     * @return 下界
     */
    public int getMinDays() {
        return minDays;
    }

    /**
     * 取桶的上界（含）.
     *
     * @return 上界
     */
    public int getMaxDays() {
        return maxDays;
    }

    /**
     * 按天数差定位所属的桶.
     * <p>
     * 边界规则：0 / 30 归入 {@code 0-30}，31 / 60 归入 {@code 31-60}，
     * 61 / 90 归入 {@code 61-90}，91 及以上归入 {@code 90+}；
     * 负数（尚未到期）按 0 天归入 {@code 0-30}。
     * </p>
     *
     * @param days 到期日与今天的天数差（今天到期为 0，逾期为正）
     * @return 所属的桶
     */
    public static ReceivableAgingBucket of(int days) {
        int effective = Math.max(days, 0);
        for (ReceivableAgingBucket bucket : values()) {
            if (effective >= bucket.minDays && effective <= bucket.maxDays) {
                return bucket;
            }
        }
        return D_90_PLUS;
    }

}
