/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
 */

package com.yaowenltd.projectinfomationmanage.controller;

import com.yaowenltd.projectinfomationmanage.testsupport.JwtTestTokenFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.security.SecureRandom;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * LotteryPeriodController 集成测试 —— 覆盖 7 个端点的三态。
 * <p>
 * 分页字段是 {@code data}（不是 list）—— 见 {@link com.yaowenltd.projectinfomationmanage.model.dto.PageResponse}。
 * </p>
 * <p>
 * statistics 端点的输入日期用 {@code h2-data-all.sql} 里 seed 的 3 条 period 覆盖区间
 * （2025-01-04 / 2025-01-06 / 2025-01-08）。
 * </p>
 *
 * @since 2026-08-05
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("all-endpoints-it")
class LotteryPeriodControllerIntegrationTests {

    private static final String JWT_SECRET_BASE64 = generateRandomSecret();

    private static String generateRandomSecret() {
        byte[] keyBytes = new byte[64];
        new SecureRandom().nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    @DynamicPropertySource
    static void registerJwtSecret(DynamicPropertyRegistry registry) {
        registry.add("jwt.secret", () -> JWT_SECRET_BASE64);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtTestTokenFactory jwtFactory;

    @BeforeEach
    void resetState() {
        // 保留 seed 3 条 lottery_period（25001/25002/25003）；清掉测试新增
        jdbcTemplate.execute("DELETE FROM t_lottery_period WHERE period NOT IN ('25001', '25002', '25003')");
    }

    // =================== POST /api/lottery/periods ===================

    @Test
    void createPeriod_admin_returns201_andEchoesPeriod() throws Exception {
        mockMvc.perform(post("/api/lottery/periods")
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"period\":\"25999\",\"drawDate\":\"2025-12-31\","
                                + "\"front1\":1,\"front2\":2,\"front3\":3,\"front4\":4,\"front5\":5,"
                                + "\"back1\":1,\"back2\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.period").value("25999"))
                .andExpect(jsonPath("$.data.id").exists());
    }

    @Test
    void createPeriod_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/lottery/periods")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"period\":\"25999\",\"drawDate\":\"2025-12-31\","
                                + "\"front1\":1,\"front2\":2,\"front3\":3,\"front4\":4,\"front5\":5,"
                                + "\"back1\":1,\"back2\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== PUT /api/lottery/periods/{id} ===================

    @Test
    void updatePeriod_admin_returns200_andEchoesUpdatedPeriod() throws Exception {
        mockMvc.perform(post("/api/lottery/periods")
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"period\":\"25888\",\"drawDate\":\"2025-11-01\","
                                + "\"front1\":2,\"front2\":4,\"front3\":6,\"front4\":8,\"front5\":10,"
                                + "\"back1\":1,\"back2\":3}"))
                .andExpect(status().isOk());

        String periodId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_lottery_period WHERE period = '25888'", String.class);

        mockMvc.perform(put("/api/lottery/periods/" + periodId)
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"period\":\"25888\",\"drawDate\":\"2025-11-02\","
                                + "\"front1\":2,\"front2\":4,\"front3\":6,\"front4\":8,\"front5\":10,"
                                + "\"back1\":1,\"back2\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.drawDate").value("2025-11-02"));
    }

    @Test
    void updatePeriod_noToken_returns401() throws Exception {
        mockMvc.perform(put("/api/lottery/periods/880e8400-e29b-41d4-a716-446655440001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"period\":\"25001\",\"drawDate\":\"2025-01-04\","
                                + "\"front1\":1,\"front2\":2,\"front3\":3,\"front4\":4,\"front5\":5,"
                                + "\"back1\":1,\"back2\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== DELETE /api/lottery/periods/{id} ===================

    @Test
    void deletePeriod_admin_returns200_andRemovesFromDb() throws Exception {
        mockMvc.perform(post("/api/lottery/periods")
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"period\":\"25777\",\"drawDate\":\"2025-10-01\","
                                + "\"front1\":1,\"front2\":2,\"front3\":3,\"front4\":4,\"front5\":5,"
                                + "\"back1\":1,\"back2\":2}"))
                .andExpect(status().isOk());

        String periodId = jdbcTemplate.queryForObject(
                "SELECT id FROM t_lottery_period WHERE period = '25777'", String.class);

        mockMvc.perform(delete("/api/lottery/periods/" + periodId)
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/lottery/periods/" + periodId)
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void deletePeriod_noToken_returns401() throws Exception {
        mockMvc.perform(delete("/api/lottery/periods/880e8400-e29b-41d4-a716-446655440001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== GET /api/lottery/periods/{id} ===================

    @Test
    void getPeriodById_admin_returns200_andEchoesPeriod() throws Exception {
        mockMvc.perform(get("/api/lottery/periods/880e8400-e29b-41d4-a716-446655440001")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.period").value("25001"))
                .andExpect(jsonPath("$.data.drawDate").value("2025-01-04"));
    }

    @Test
    void getPeriodById_admin_returnsCode400_whenIdNotFound() throws Exception {
        mockMvc.perform(get("/api/lottery/periods/00000000-0000-0000-0000-000000000000")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void getPeriodById_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/lottery/periods/880e8400-e29b-41d4-a716-446655440001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== GET /api/lottery/periods (paginated) ===================

    @Test
    void getAllPeriodsPaginated_admin_returns200_andContainsSeededPeriods() throws Exception {
        mockMvc.perform(get("/api/lottery/periods?page=1&size=10")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                // 注意：PageResponse 字段名是 data（不是 list）
                .andExpect(jsonPath("$.data.data").isArray())
                .andExpect(jsonPath("$.data.data.length()").value(3))
                .andExpect(jsonPath("$.data.data[?(@.period == '25001')]").exists())
                .andExpect(jsonPath("$.data.data[?(@.period == '25002')]").exists())
                .andExpect(jsonPath("$.data.data[?(@.period == '25003')]").exists());
    }

    @Test
    void getAllPeriodsPaginated_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/lottery/periods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== GET /api/lottery/statistics/single ===================

    @Test
    void getSinglePeriodStatistics_admin_returns200_andTotalPeriodsMatchesSeed() throws Exception {
        mockMvc.perform(get("/api/lottery/statistics/single?startDate=2025-01-01&endDate=2025-01-31")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalPeriods").value(3))
                .andExpect(jsonPath("$.data.frontAreaStats").isArray())
                .andExpect(jsonPath("$.data.backAreaStats").isArray());
    }

    @Test
    void getSinglePeriodStatistics_admin_returnsEmpty_whenNoPeriodsInRange() throws Exception {
        mockMvc.perform(get("/api/lottery/statistics/single?startDate=2030-01-01&endDate=2030-12-31")
                        .header("Authorization", jwtFactory.bearer("admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.totalPeriods").value(0));
    }

    @Test
    void getSinglePeriodStatistics_noToken_returns401() throws Exception {
        mockMvc.perform(get("/api/lottery/statistics/single?startDate=2025-01-01&endDate=2025-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // =================== POST /api/lottery/statistics/multiple ===================

    @Test
    void getMultiplePeriodStatistics_admin_returns200_andEchoesPeriodLabels() throws Exception {
        mockMvc.perform(post("/api/lottery/statistics/multiple")
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ranges\":["
                                + "{\"label\":\"2025-01上旬\",\"startDate\":\"2025-01-01\",\"endDate\":\"2025-01-10\"}"
                                + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.periods").isArray())
                .andExpect(jsonPath("$.data.periods[0].label").value("2025-01上旬"))
                .andExpect(jsonPath("$.data.periods[0].totalPeriods").value(3))
                .andExpect(jsonPath("$.data.frontAreaStats").isArray())
                .andExpect(jsonPath("$.data.backAreaStats").isArray());
    }

    @Test
    void getMultiplePeriodStatistics_admin_returnsCode400_whenRangesEmpty() throws Exception {
        mockMvc.perform(post("/api/lottery/statistics/multiple")
                        .header("Authorization", jwtFactory.bearer("admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ranges\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void getMultiplePeriodStatistics_noToken_returns401() throws Exception {
        mockMvc.perform(post("/api/lottery/statistics/multiple")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ranges\":[{\"label\":\"X\",\"startDate\":\"2025-01-01\",\"endDate\":\"2025-01-10\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }
}