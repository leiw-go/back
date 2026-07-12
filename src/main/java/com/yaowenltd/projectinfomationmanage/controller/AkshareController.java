/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller;

import com.yaowenltd.projectinfomationmanage.common.ResponseResult;
import com.yaowenltd.projectinfomationmanage.config.AkshareServiceProperties;
import com.yaowenltd.projectinfomationmanage.config.SkipAuth;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 转发到 akshare-service FastAPI 网关的轻量代理.
 * <p>
 * 端点与 FastAPI 一侧保持 1:1 对应；本控制器仅负责把上游故障
 * 翻译成 502 风格的失败响应，使调用方能够区分
 * "akshare 挂了" 与 "请求参数错误" 这两种情况。
 * </p>
 *
 * <pre>
 * GET /api/akshare/health
 * GET /api/akshare/stock/hist?symbol=000001&period=daily&startDate=20240101&endDate=20241231
 * GET /api/akshare/stock/spot
 * GET /api/akshare/fund/open-rank
 * GET /api/akshare/macro/china-cpi
 * </pre>
 *
 * <p>
 * <strong>@SkipAuth</strong> 在类级别生效，这样用 curl 临时调试或
 * Swagger UI 上测试时就不需要 JWT。一旦该接口成为正式的生产集成，
 * 移除 @SkipAuth，让 {@code AuthInterceptor} 像管理其他控制器一样
 * 对这些端点进行鉴权。
 * </p>
 */
@RestController
@RequestMapping("/api/akshare")
public class AkshareController {

    private final RestTemplate restTemplate;

    /**
     * akshare 网关的实时配置；这是一个 {@code @RefreshScope}
     * 代理，因此 Nacos 端对 {@code akshare.service.base-url}
     * 的任何变更都会在下次请求时被自动加载，无需重启。
     * 我们刻意不在构造时把 {@code baseUrl} 快照保存到字段中，
     * 否则就失去了 Refresh 的意义。
     */
    private final AkshareServiceProperties properties;

    /**
     * 由 Spring 注入构造函数，无需再额外加 @Autowired.
     *
     * @param restTemplate 共用的 HTTP 客户端（参见 {@code RestTemplateConfig}）
     * @param properties   akshare 网关的实时配置
     */
    public AkshareController(RestTemplate restTemplate,
                             AkshareServiceProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * 健康探针 —— 仅做透传转发，用于确认网关是否存活.
     *
     * @return 上游 /health 的负载或失败响应体
     */
    @GetMapping("/health")
    @SkipAuth
    public ResponseResult<?> health() {
        return relay("/health");
    }

    /**
     * A 股历史 K 线.
     *
     * @param symbol    6 位 A 股代码（例如 000001）
     * @param period    daily / weekly / monthly
     * @param startDate YYYYMMDD
     * @param endDate   YYYYMMDD
     * @param adjust    hfq / qfq / ""（不复权）
     * @return 上游返回的行数据列表
     */
    @GetMapping("/stock/hist")
    @SkipAuth
    public ResponseResult<?> stockHist(
            @RequestParam String symbol,
            @RequestParam(defaultValue = "daily") String period,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "hfq") String adjust) {
        return relay(
                "/api/stock/zh-a/hist"
                        + "?symbol={symbol}&period={period}"
                        + "&start_date={startDate}&end_date={endDate}&adjust={adjust}",
                Map.of(
                        "symbol", symbol,
                        "period", period,
                        "startDate", startDate,
                        "endDate", endDate,
                        "adjust", adjust));
    }

    /**
     * 全市场 A 股实时快照.
     *
     * @return 上游返回的快照数据
     */
    @GetMapping("/stock/spot")
    @SkipAuth
    public ResponseResult<?> stockSpot() {
        return relay("/api/stock/zh-a/spot");
    }

    /**
     * 开放式基金排名.
     *
     * @param symbol 全部 / 股票型 / 混合型 / ...
     * @return 上游返回的排名数据
     */
    @GetMapping("/fund/open-rank")
    @SkipAuth
    public ResponseResult<?> fundOpenRank(
            @RequestParam(defaultValue = "全部") String symbol) {
        return relay("/api/fund/open-rank?symbol={symbol}", Map.of("symbol", symbol));
    }

    /**
     * 中国月度 CPI.
     *
     * @return 上游返回的 CPI 序列
     */
    @GetMapping("/macro/china-cpi")
    @SkipAuth
    public ResponseResult<?> macroChinaCpi() {
        return relay("/api/macro/china-cpi");
    }

    // ----------------------------------------------------------------------- //
    // 内部：转发 + 错误包装
    // ----------------------------------------------------------------------- //

    private ResponseResult<?> relay(String path) {
        return relay(path, Map.of());
    }

    @SuppressWarnings("unchecked")
    private ResponseResult<?> relay(String path, Map<String, ?> uriVars) {
        final String url = properties.getBaseUrl() + path;
        try {
            Object body = restTemplate.getForObject(url, Object.class, uriVars);
            return ResponseResult.success(body);
        } catch (RestClientException e) {
            // 上游不可达 / 超时 / 5xx 都归并成 502 风格的失败体
            return new ResponseResult<>(502, "akshare upstream error: " + e.getMessage(), null);
        }
    }
}
