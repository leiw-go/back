/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.task;

import com.yaowenltd.projectinfomationmanage.crm.common.MessageNotifier;
import com.yaowenltd.projectinfomationmanage.crm.common.MessageTextFactory;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmOrderMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmReceivableMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmOrderEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmReceivableEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.MessageBizType;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.MessageType;
import com.yaowenltd.projectinfomationmanage.crm.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CRM 应收逾期扫描定时任务.
 * <p>
 * 需求文档 §3.11：应收逾期发 {@code RECEIVABLE_OVERDUE}（URGENT）站内信给订单负责人。
 * <p>
 * **幂等**：同一笔应收只发一次 —— 判据是"该应收是否已发过 RECEIVABLE_OVERDUE 系统消息"
 * （{@code t_message} 里按 bizType + bizId + messageType 查）。
 * 重复执行不会重复骚扰业务员。
 * <p>
 * ⚠️ **时区**：cron 显式指定 {@code zone = "Asia/Shanghai"}。
 *
 * @since 2026-11-08
 */
@Component
public class CrmReceivableOverdueTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrmReceivableOverdueTask.class);

    /** 应收状态：逾期. */
    private static final String STATUS_OVERDUE = "OVERDUE";

    private final CrmReceivableMapper receivableMapper;
    private final CrmOrderMapper orderMapper;
    private final MessageService messageService;
    private final MessageNotifier messageNotifier;

    /**
     * 使用指定依赖构造 CrmReceivableOverdueTask.
     *
     * @param receivableMapper 应收 Mapper
     * @param orderMapper      订单 Mapper
     * @param messageService   站内信服务
     * @param messageNotifier  站内信通知器
     */
    public CrmReceivableOverdueTask(CrmReceivableMapper receivableMapper,
                                    CrmOrderMapper orderMapper,
                                    MessageService messageService,
                                    MessageNotifier messageNotifier) {
        this.receivableMapper = receivableMapper;
        this.orderMapper = orderMapper;
        this.messageService = messageService;
        this.messageNotifier = messageNotifier;
    }

    /**
     * 扫描逾期应收并通知订单负责人.
     */
    @Scheduled(cron = "${crm.receivable.overdue-cron:0 0 8 * * ?}", zone = "Asia/Shanghai")
    public void scan() {
        int notified = scanOverdue(LocalDate.now());
        LOGGER.info("crm receivable overdue scan finished: {} notifications sent", notified);
    }

    /**
     * 执行逾期扫描（供 cron 与单元测试调用）.
     *
     * @param today 判定基准日
     * @return 本次发出的通知条数
     */
    public int scanOverdue(LocalDate today) {
        List<CrmReceivableEntity> overdueList = receivableMapper.selectOverdueList(today);
        if (overdueList == null || overdueList.isEmpty()) {
            return 0;
        }
        int notified = 0;
        for (CrmReceivableEntity receivable : overdueList) {
            if (receivable.getDueDate() == null) {
                continue;
            }
            int overdueDays = (int) ChronoUnit.DAYS.between(receivable.getDueDate(), today);
            if (overdueDays <= 0) {
                continue;
            }
            if (!STATUS_OVERDUE.equals(receivable.getReceivableStatus())) {
                receivable.setReceivableStatus(STATUS_OVERDUE);
                receivable.setOverdueDays(overdueDays);
                receivableMapper.update(receivable);
            }
            if (messageService.existsSystemMessage(MessageBizType.RECEIVABLE.name(),
                    receivable.getId(), MessageType.RECEIVABLE_OVERDUE.name())) {
                continue;
            }
            if (notifyOwner(receivable, overdueDays)) {
                notified++;
            }
        }
        return notified;
    }

    /**
     * 给订单负责人发逾期催款站内信.
     *
     * @param receivable  逾期应收
     * @param overdueDays 逾期天数
     * @return 是否成功发出
     */
    private boolean notifyOwner(CrmReceivableEntity receivable, int overdueDays) {
        String receiverId = receivable.getOwnerId();
        if (receiverId == null || receiverId.trim().isEmpty()) {
            LOGGER.warn("应收逾期通知跳过：负责人为空, receivableId={}", receivable.getId());
            return false;
        }
        BigDecimal outstanding = outstanding(receivable);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("overdueDays", String.valueOf(overdueDays));
        placeholders.put("amount", MessageTextFactory.money(outstanding, receivable.getCurrencyCode()));
        messageNotifier.notifyOwner(MessageType.RECEIVABLE_OVERDUE, MessageBizType.RECEIVABLE.name(),
                receivable.getId(), orderNo(receivable.getOrderId()), receiverId, placeholders);
        return true;
    }

    /**
     * 取未收金额.
     *
     * @param receivable 应收
     * @return 未收金额
     */
    private BigDecimal outstanding(CrmReceivableEntity receivable) {
        BigDecimal amount = receivable.getReceivableAmount() == null
                ? BigDecimal.ZERO : receivable.getReceivableAmount();
        BigDecimal received = receivable.getReceivedAmount() == null
                ? BigDecimal.ZERO : receivable.getReceivedAmount();
        BigDecimal result = amount.subtract(received);
        return result.signum() > 0 ? result : BigDecimal.ZERO;
    }

    /**
     * 取订单号（订单不存在时退回应收 ID）.
     *
     * @param orderId 订单 ID
     * @return 订单号
     */
    private String orderNo(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            return "-";
        }
        CrmOrderEntity order = orderMapper.selectById(orderId);
        return order == null || order.getOrderNo() == null ? orderId : order.getOrderNo();
    }

}
