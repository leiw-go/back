/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring 上下文冒烟测试 —— 验证主 {@code @SpringBootApplication} 类（{@link ProjectInformationManageApplication}）
 * 在测试 profile 下能完整加载而不抛异常。
 * <p>
 * <strong>为什么不连 MySQL：</strong>本类在 {@code spring.autoconfigure.exclude} 里显式排除
 * {@code DataSourceAutoConfiguration} + {@code MybatisAutoConfiguration}，并对全部 7 个 Mapper
 * （{@code UserMapper} / {@code RoleMapper} / {@code UserRoleMapper} / {@code PermissionMapper} /
 * {@code RolePermissionMapper} / {@code ProductMapper} / {@code LotteryPeriodMapper}）加
 * {@code @MockBean} 占位，加上 {@code t_llm_provider} / {@code t_llm_request_log} 两个新增的
 * LLM Mapper —— 共 9 个 Mapper 全部 mock。
 * </p>
 * <p>
 * <strong>为什么不连 Nacos：</strong>{@code @ActiveProfiles("test")} + {@code application-test.yml}
 * 已关闭 Nacos config / discovery / service-registry，jwt 占位 secret 由 yml 提供。
 * </p>
 * <p>
 * <strong>为什么 mock OpenAiRelayService：</strong>它依赖 {@code ProviderRepository} /
 * {@code AesGcmEncryptor} / {@code TokenEstimator} / {@code UsageRecorder} / {@code OpenTelemetry}，
 * 这些都需要在 H2 + mock mapper 的上下文里能装配；最干净的做法是 mock relay 让 context 不试图
 * 真正连上游 provider。
 * </p>
 * <p>
 * <strong>关于 {@code ProdProfileWithoutNacosTests}：</strong>本文档与
 * {@code AGENTS.md §11} 提到的 {@code ProdProfileWithoutNacosTests} 当前不在仓库里
 * （{@code Explore} 子代理确认）。本轮不补这条 —— 改了会和文档不一致，不修又好像在"装看不见"；
 * 决定保留状态留待后续独立 PR。
 * </p>
 *
 * @since 2026-08-05
 */
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude="
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.sql.init.DataSourceInitializationAutoConfiguration,"
                + "org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration,"
                + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
@ActiveProfiles("test")
class ProjectInformationManageApplicationTests {

    @MockBean
    private com.yaowenltd.projectinfomationmanage.mapper.UserMapper userMapper;

    @MockBean
    private com.yaowenltd.projectinfomationmanage.mapper.RoleMapper roleMapper;

    @MockBean
    private com.yaowenltd.projectinfomationmanage.mapper.UserRoleMapper userRoleMapper;

    @MockBean
    private com.yaowenltd.projectinfomationmanage.mapper.PermissionMapper permissionMapper;

    @MockBean
    private com.yaowenltd.projectinfomationmanage.mapper.RolePermissionMapper rolePermissionMapper;

    @MockBean
    private com.yaowenltd.projectinfomationmanage.mapper.ProductMapper productMapper;

    @MockBean
    private com.yaowenltd.projectinfomationmanage.mapper.LotteryPeriodMapper lotteryPeriodMapper;

    @MockBean
    private com.yaowenltd.projectinfomationmanage.mapper.llm.LlmProviderMapper llmProviderMapper;

    @MockBean
    private com.yaowenltd.projectinfomationmanage.mapper.llm.LlmRequestLogMapper llmRequestLogMapper;

    @MockBean
    private com.yaowenltd.projectinfomationmanage.service.openai.OpenAiRelayService openAiRelay;

    @Test
    void contextLoads() {
        // 主目的就是验证 context 能完整装配；本方法体为空即可。
    }
}