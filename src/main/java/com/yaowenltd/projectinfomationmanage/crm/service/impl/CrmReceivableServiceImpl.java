/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.crm.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmOrderMapper;
import com.yaowenltd.projectinfomationmanage.crm.mapper.CrmReceivableMapper;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableAgingBucket;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableAgingDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableDto;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableQueryRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.dto.order.CrmReceivableWriteOffRequest;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmOrderEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.entity.CrmReceivableEntity;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.OrderStatus;
import com.yaowenltd.projectinfomationmanage.crm.model.enums.ReceivableStatus;
import com.yaowenltd.projectinfomationmanage.crm.service.CrmReceivableService;
import com.yaowenltd.projectinfomationmanage.crm.service.OrderStatusService;
import com.yaowenltd.projectinfomationmanage.model.dto.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CRM 应收服务实现.
 * <p>
 * 按需求文档 §3.8：应收按 {@code deposit_rate} 拆成定金 + 尾款；
 * 核销只累加 {@code received_amount}（本期不建收款流水表），每次核销写一条结构化日志。
 * 逾期是"查询时按 due_date 判定"，库里存的始终是收款进度状态。
 *
 * @since 2026-11-08
 */
@Service
public class CrmReceivableServiceImpl implements CrmReceivableService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrmReceivableServiceImpl.class);

    /** 列表默认每页条数. */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /** 列表最大每页条数. */
    private static final int MAX_PAGE_SIZE = 200;

    /** 单次查询允许的最大偏移量. */
    private static final int MAX_PAGE_OFFSET = 10000;

    /** 金额保留的小数位. */
    private static final int AMOUNT_SCALE = 2;

    /** 账龄分桶顺序. */
    private static final String[] AGING_BUCKETS = {"0-30", "31-60", "61-90", "90+"};

    /** 账龄分桶中文名. */
    private static final Map<String, String> AGING_LABELS = new LinkedHashMap<>();

    static {
        AGING_LABELS.put("0-30", "0-30 天");
        AGING_LABELS.put("31-60", "31-60 天");
        AGING_LABELS.put("61-90", "61-90 天");
        AGING_LABELS.put("90+", "90 天以上");
    }

    private final CrmReceivableMapper receivableMapper;
    private final CrmOrderMapper orderMapper;
    private final OrderStatusService orderStatusService;

    /**
     * 使用指定的 Mapper 构造 CrmReceivableServiceImpl.
     *
     * @param receivableMapper   应收 Mapper
     * @param orderMapper        订单 Mapper（核销后回判订单状态）
     * @param orderStatusService 订单状态机
     */
    public CrmReceivableServiceImpl(CrmReceivableMapper receivableMapper,
                                     CrmOrderMapper orderMapper,
                                     OrderStatusService orderStatusService) {
        this.receivableMapper = receivableMapper;
        this.orderMapper = orderMapper;
        this.orderStatusService = orderStatusService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrmReceivableDto writeOff(String id, CrmReceivableWriteOffRequest request) {
        if (request == null || request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("write off amount must be positive");
        }
        CrmReceivableEntity receivable = requireReceivable(id);
        BigDecimal outstanding = outstanding(receivable);
        if (request.getAmount().compareTo(outstanding) > 0) {
            throw new IllegalArgumentException(
                    "write off amount exceeds the outstanding amount: " + outstanding.toPlainString());
        }

        BigDecimal received = receivable.getReceivedAmount().add(request.getAmount())
                .setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        receivable.setReceivedAmount(received);
        receivable.setReceivableStatus(received.compareTo(receivable.getReceivableAmount()) >= 0
                ? ReceivableStatus.PAID.name() : ReceivableStatus.PARTIAL.name());
        receivableMapper.update(receivable);

        LocalDate paidDate = request.getPaidDate() == null ? LocalDate.now() : request.getPaidDate();
        LOGGER.info("receivable write off: receivableId={}, orderId={}, amount={}, currency={}, "
                        + "paidDate={}, method={}, remark={}",
                receivable.getId(), receivable.getOrderId(), request.getAmount().toPlainString(),
                receivable.getCurrencyCode(), paidDate, request.getPaymentMethod(), request.getRemark());

        tryCompleteOrder(receivable.getOrderId());
        return toDto(receivable);
    }

    @Override
    public List<CrmReceivableDto> listByOrder(String orderId) {
        List<CrmReceivableEntity> entities = receivableMapper.selectByOrderId(orderId);
        List<CrmReceivableDto> rows = new ArrayList<>(entities.size());
        for (CrmReceivableEntity entity : entities) {
            rows.add(toDto(entity));
        }
        return rows;
    }

    @Override
    public PageResponse<CrmReceivableDto> list(CrmReceivableQueryRequest query) {
        CrmReceivableQueryRequest actual = query == null ? new CrmReceivableQueryRequest() : query;
        int page = actual.getPage() == null || actual.getPage() < 1 ? 1 : actual.getPage();
        int size = actual.getSize() == null || actual.getSize() < 1
                ? DEFAULT_PAGE_SIZE : Math.min(actual.getSize(), MAX_PAGE_SIZE);
        if ((long) page * size > MAX_PAGE_OFFSET) {
            throw new IllegalArgumentException("page * size must not exceed " + MAX_PAGE_OFFSET);
        }
        PageHelper.startPage(page, size);
        try {
            PageInfo<CrmReceivableEntity> info = new PageInfo<>(receivableMapper.selectPage(actual));

            List<CrmReceivableDto> rows = new ArrayList<>(info.getList().size());
            for (CrmReceivableEntity entity : info.getList()) {
                rows.add(toDto(entity));
            }
            PageResponse<CrmReceivableDto> response = new PageResponse<>();
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
    public CrmReceivableAgingDto aging() {
        List<CrmReceivableAgingBucket> raw = receivableMapper.selectAging();
        Map<String, CrmReceivableAgingBucket> byBucket = new LinkedHashMap<>();
        for (String bucket : AGING_BUCKETS) {
            CrmReceivableAgingBucket empty = new CrmReceivableAgingBucket();
            empty.setBucket(bucket);
            empty.setLabel(AGING_LABELS.get(bucket));
            empty.setAmount(BigDecimal.ZERO.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP));
            empty.setCount(0);
            byBucket.put(bucket, empty);
        }
        if (raw != null) {
            for (CrmReceivableAgingBucket row : raw) {
                if (row == null || row.getBucket() == null) {
                    continue;
                }
                CrmReceivableAgingBucket bucket = byBucket.get(row.getBucket());
                if (bucket != null) {
                    bucket.setAmount(row.getAmount() == null ? BigDecimal.ZERO : row.getAmount());
                    bucket.setCount(row.getCount());
                }
            }
        }

        CrmReceivableAgingDto dto = new CrmReceivableAgingDto();
        List<CrmReceivableAgingBucket> buckets = new ArrayList<>(AGING_BUCKETS.length);
        BigDecimal total = BigDecimal.ZERO.setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
        long count = 0;
        for (String key : AGING_BUCKETS) {
            CrmReceivableAgingBucket bucket = byBucket.get(key);
            buckets.add(bucket);
            total = total.add(bucket.getAmount());
            count += bucket.getCount();
        }
        dto.setBuckets(buckets);
        dto.setTotalOutstandingAmount(total);
        dto.setTotalOutstandingCount(count);
        return dto;
    }

    /**
     * 订单已交付且关联应收全部收清时，订单置 COMPLETED.
     * <p>
     * 状态机不允许跨级（SHIPPED 不能直接到 COMPLETED），条件不满足时静默跳过。
     */
    @Override
    public void tryCompleteOrder(String orderId) {
        CrmOrderEntity order = orderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        OrderStatus current = OrderStatus.valueOf(order.getOrderStatus());
        if (current != OrderStatus.DELIVERED) {
            return;
        }
        List<CrmReceivableEntity> receivables = receivableMapper.selectByOrderId(orderId);
        if (receivables.isEmpty()) {
            return;
        }
        for (CrmReceivableEntity receivable : receivables) {
            if (!ReceivableStatus.PAID.name().equals(receivable.getReceivableStatus())) {
                return;
            }
        }
        if (!orderStatusService.canTransit(current, OrderStatus.COMPLETED)) {
            return;
        }
        order.setOrderStatus(OrderStatus.COMPLETED.name());
        orderMapper.update(order);
    }

    private BigDecimal outstanding(CrmReceivableEntity receivable) {
        BigDecimal received = receivable.getReceivedAmount() == null
                ? BigDecimal.ZERO : receivable.getReceivedAmount();
        return receivable.getReceivableAmount().subtract(received).setScale(AMOUNT_SCALE, RoundingMode.HALF_UP);
    }

    private CrmReceivableEntity requireReceivable(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("receivable id must not be blank");
        }
        CrmReceivableEntity entity = receivableMapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("receivable not found: " + id);
        }
        return entity;
    }

    /**
     * 转成出参：逾期状态与逾期天数在查询时按 due_date 实时算（需求文档 §5.3.12）.
     */
    private CrmReceivableDto toDto(CrmReceivableEntity entity) {
        LocalDate today = LocalDate.now();
        boolean paid = ReceivableStatus.PAID.name().equals(entity.getReceivableStatus());
        int overdueDays = 0;
        if (!paid && entity.getDueDate() != null && entity.getDueDate().isBefore(today)) {
            overdueDays = (int) ChronoUnit.DAYS.between(entity.getDueDate(), today);
        }

        CrmReceivableDto dto = new CrmReceivableDto();
        dto.setId(entity.getId());
        dto.setOrderId(entity.getOrderId());
        dto.setCurrencyCode(entity.getCurrencyCode());
        dto.setReceivableType(entity.getReceivableType());
        dto.setReceivableAmount(entity.getReceivableAmount());
        dto.setReceivedAmount(entity.getReceivedAmount());
        dto.setOutstandingAmount(outstanding(entity));
        dto.setDueDate(entity.getDueDate());
        dto.setOverdueDays(overdueDays);
        dto.setOwnerId(entity.getOwnerId());
        if (paid) {
            dto.setReceivableStatus(ReceivableStatus.PAID.name());
        } else if (overdueDays > 0) {
            dto.setReceivableStatus(ReceivableStatus.OVERDUE.name());
        } else {
            dto.setReceivableStatus(entity.getReceivableStatus());
        }
        return dto;
    }

}
