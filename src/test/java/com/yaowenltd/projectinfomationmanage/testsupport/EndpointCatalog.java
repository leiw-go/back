/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.testsupport;

import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;

/**
 * 所有受保护端点（{@code /api/**} + {@code /v1/**}）的清单 + 配套常量.
 * <p>
 * <strong>单一事实源：</strong>{@link AuthPermissionMatrixIntegrationTests} 用它做参数化输入，
 * 各 Controller IT 类也共用同一份以保证覆盖与代码同步。
 * </p>
 * <p>
 * 当前共 34 个端点：{@link #all()} = {@link #required()} + {@link #skipAuth()}。
 * </p>
 *
 * @since 2026-08-05
 */
public final class EndpointCatalog {

    private EndpointCatalog() {
    }

    /**
     * 单个端点条目.
     *
     * @param method   HTTP 方法
     * @param path     相对路径（无 context-path 前缀，MockMvc 自动剥 /design）
     * @param skipAuth 是否标了 {@code @SkipAuth}（true 时权限矩阵不应期望 401）
     * @param tag      用于矩阵 @MethodSource 的简短描述（保证输出稳定）
     */
    public record Entry(HttpMethod method, String path, boolean skipAuth, String tag) {
    }

    /**
     * 全部 34 个端点.
     *
     * @return 端点清单
     */
    public static List<Entry> all() {
        List<Entry> list = new ArrayList<>();
        list.addAll(skipAuth());
        list.addAll(required());
        return list;
    }

    /**
     * 标了 {@code @SkipAuth} 的 2 个端点（AuthController#register + AuthController#login）.
     *
     * @return SkipAuth 端点
     */
    public static List<Entry> skipAuth() {
        return List.of(
                new Entry(HttpMethod.POST, "/api/auth/register", true, "auth.register"),
                new Entry(HttpMethod.POST, "/api/auth/login", true, "auth.login"));
    }

    /**
     * 需要 JWT 的 32 个端点.
     *
     * @return 受保护端点
     */
    public static List<Entry> required() {
        return List.of(
                // AuthController
                new Entry(HttpMethod.GET, "/api/auth/currentUser", false, "auth.currentUser"),
                // UserController
                new Entry(HttpMethod.POST,   "/api/users",       false, "users.create"),
                new Entry(HttpMethod.PUT,    "/api/users/{id}",  false, "users.update"),
                new Entry(HttpMethod.DELETE, "/api/users/{id}",  false, "users.delete"),
                new Entry(HttpMethod.GET,    "/api/users/{id}",  false, "users.getById"),
                new Entry(HttpMethod.GET,    "/api/users",       false, "users.list"),
                // RoleController
                new Entry(HttpMethod.POST,   "/api/roles",              false, "roles.create"),
                new Entry(HttpMethod.PUT,    "/api/roles/{id}",         false, "roles.update"),
                new Entry(HttpMethod.DELETE, "/api/roles/{id}",         false, "roles.delete"),
                new Entry(HttpMethod.GET,    "/api/roles/{id}",         false, "roles.getById"),
                new Entry(HttpMethod.GET,    "/api/roles",              false, "roles.list"),
                new Entry(HttpMethod.GET,    "/api/roles/permissions",  false, "roles.permissions"),
                // ProductController
                new Entry(HttpMethod.POST,   "/api/products",      false, "products.create"),
                new Entry(HttpMethod.PUT,    "/api/products/{id}", false, "products.update"),
                new Entry(HttpMethod.DELETE, "/api/products/{id}", false, "products.delete"),
                new Entry(HttpMethod.GET,    "/api/products/{id}", false, "products.getById"),
                new Entry(HttpMethod.GET,    "/api/products",      false, "products.list"),
                // LotteryPeriodController
                new Entry(HttpMethod.POST,   "/api/lottery/periods",                   false, "lottery.create"),
                new Entry(HttpMethod.PUT,    "/api/lottery/periods/{id}",              false, "lottery.update"),
                new Entry(HttpMethod.DELETE, "/api/lottery/periods/{id}",              false, "lottery.delete"),
                new Entry(HttpMethod.GET,    "/api/lottery/periods/{id}",              false, "lottery.getById"),
                new Entry(HttpMethod.GET,    "/api/lottery/periods",                   false, "lottery.list"),
                new Entry(HttpMethod.GET,    "/api/lottery/statistics/single",         false, "lottery.statSingle"),
                new Entry(HttpMethod.POST,   "/api/lottery/statistics/multiple",       false, "lottery.statMultiple"),
                // UsageController
                new Entry(HttpMethod.GET,    "/api/llm/usage", false, "llm.usage"),
                // ProviderController
                new Entry(HttpMethod.GET,    "/api/llm/providers",      false, "llm.providers.list"),
                new Entry(HttpMethod.POST,   "/api/llm/providers",      false, "llm.providers.create"),
                new Entry(HttpMethod.PUT,    "/api/llm/providers",      false, "llm.providers.update"),
                new Entry(HttpMethod.DELETE, "/api/llm/providers/{id}", false, "llm.providers.delete"),
                // OpenAI compatible
                new Entry(HttpMethod.GET,    "/v1/models",                false, "openai.models"),
                new Entry(HttpMethod.POST,   "/v1/chat/completions",      false, "openai.chat"),
                new Entry(HttpMethod.POST,   "/v1/embeddings",            false, "openai.embeddings"));
    }

    /**
     * Spring Test 默认 base url 前缀（IT 用 MockMvc 自动剥 context-path）.
     */
    public static final String BASE_URL = "";
}