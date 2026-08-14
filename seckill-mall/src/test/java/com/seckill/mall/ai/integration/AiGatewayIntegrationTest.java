package com.seckill.mall.ai.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 网关集成测试（T20）：基于 {@link AiIntegrationTestBase} 编排 Testcontainers + MockWebServer，
 * 端到端验证 ai-gateway 五大 Advisor（限流/缓存/审计/降级/预算）的协同行为。
 *
 * <h3>测试用例清单</h3>
 * <ul>
 *   <li>I-GW-01 正常调用返回 LLM 响应</li>
 *   <li>I-GW-02 限流触发降级（@Disabled：需调 101 次触发 IP 维度限流，耗时过长）</li>
 *   <li>I-GW-03 语义缓存命中</li>
 *   <li>I-GW-04 LLM 超时降级（@Disabled：MockWebServer 超时模拟需 bodyDelay > 15s，耗时过长）</li>
 *   <li>I-GW-05 审计记录写入 t_ai_audit</li>
 *   <li>I-GW-06 日预算超限降级</li>
 * </ul>
 *
 * <p>环境依赖：运行需 Docker（Testcontainers 编排 MySQL/Redis/RabbitMQ），编译不依赖。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AiGatewayIntegrationTest.java
 * 邮箱：nj651217@163.com
 */
@DisplayName("AI网关集成测试")
class AiGatewayIntegrationTest extends AiIntegrationTestBase {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * I-GW-01 正常调用返回 LLM 响应。
     * <p>Given：MockWebServer enqueue OpenAI 协议响应
     * <p>When：以 ADMIN 身份调 /api/v1/admin/ai-gateway/test
     * <p>Then：响应 code=200，data 为 MockWebServer enqueue 的内容
     */
    @Test
    @DisplayName("I-GW-01 正常调用返回 LLM 响应")
    void normalCall_shouldReturnLlmResponse() throws Exception {
        // given
        String expectedContent = "你好，我是 AI 测试助手，很高兴为你服务。";
        enqueueLlmResponse(expectedContent);
        String token = loginAsAdmin();

        // when & then
        callAiGateway(token, "你好")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(expectedContent));
    }

    /**
     * I-GW-02 限流触发降级。
     * <p>Given：AiGatewayController 未传 userId/ip，限流走 unknown 主体（ip-capacity=100）
     * <p>When：连续调 101 次（超 ip-capacity=100）
     * <p>Then：第 101 次返回降级文案 "AI 服务暂时不可用，请稍后再试"
     *
     * <p>@Disabled 原因：需调 101 次触发 IP 维度限流，单测耗时过长（每次 HTTP + LLM mock），
     * 已在单元测试 RateLimitAdvisorTest 中验证限流逻辑，集成层标记跳过。
     */
    @Test
    @Disabled("限流走 unknown 主体（ip-capacity=100），需调 101 次触发，集成测试耗时过长；限流逻辑已由 RateLimitAdvisorTest 单测覆盖")
    @DisplayName("I-GW-02 限流触发降级：连续调用超容量后返回降级文案")
    void rateLimit_shouldDegradeAfterCapacityExceeded() throws Exception {
        String token = loginAsAdmin();
        String prompt = "限流测试-" + System.currentTimeMillis();

        // 前 100 次正常调用（每次 enqueue LLM 响应）
        for (int i = 0; i < 100; i++) {
            enqueueLlmResponse("正常响应-" + i);
            callAiGateway(token, prompt)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        // 第 101 次应触发限流降级（不 enqueue，限流 Advisor 在 LLM 调用前拦截）
        callAiGateway(token, prompt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("AI 服务暂时不可用，请稍后再试"));
    }

    /**
     * I-GW-03 语义缓存命中。
     * <p>Given：SemanticCacheAdvisor 用 SHA-256(prompt) 精确匹配缓存
     * <p>When：同一 prompt 连续调 2 次
     * <p>Then：第二次响应内容与第一次相同，且 MockWebServer 只收到 1 次 LLM 请求
     */
    @Test
    @DisplayName("I-GW-03 语义缓存命中：同 prompt 第二次走缓存，MockWebServer 仅收 1 次请求")
    void semanticCache_shouldHitOnSamePrompt() throws Exception {
        String cachedContent = "这是缓存测试的固定响应。";
        enqueueLlmResponse(cachedContent);
        String token = loginAsAdmin();
        String prompt = "缓存测试-" + System.currentTimeMillis();

        // 第一次调用：未命中缓存，调 LLM
        String firstData = callAiGateway(token, prompt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        JsonNode firstNode = objectMapper.readTree(firstData);
        assertThat(firstNode.at("/data").asText()).isEqualTo(cachedContent);

        // 第二次调用：应命中缓存，不再调 LLM
        String secondData = callAiGateway(token, prompt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        JsonNode secondNode = objectMapper.readTree(secondData);
        assertThat(secondNode.at("/data").asText()).isEqualTo(cachedContent);

        // MockWebServer 只收到 1 次 LLM 请求（第二次走缓存）
        assertThat(mockWebServer.getRequestCount())
                .as("语义缓存命中：MockWebServer 应只收到 1 次 LLM 请求")
                .isEqualTo(1);
    }

    /**
     * I-GW-04 LLM 超时降级。
     * <p>Given：MockWebServer 响应延迟超过 fallback.timeout-ms=15000ms
     * <p>When：调 /api/v1/admin/ai-gateway/test
     * <p>Then：返回降级文案 "AI 服务暂时不可用，请稍后再试"
     *
     * <p>@Disabled 原因：MockWebServer 超时模拟需设置 bodyDelay > 15s，
     * 单测耗时过长；降级逻辑已由 FallbackAdvisorTest 单测覆盖。
     */
    @Test
    @Disabled("MockWebServer 超时模拟需 bodyDelay > 15s，单测耗时过长；降级逻辑已由 FallbackAdvisorTest 单测覆盖")
    @DisplayName("I-GW-04 LLM 超时降级：返回降级文案")
    void llmTimeout_shouldReturnFallbackText() throws Exception {
        // 实现思路（启用时）：
        // 1. enqueue MockResponse.setBodyDelay(16, TimeUnit.SECONDS) 超过 fallback.timeout-ms=15000ms
        // 2. 调 callAiGateway(token, "超时测试")
        // 3. 验证 $.data = "AI 服务暂时不可用，请稍后再试"
    }

    /**
     * I-GW-05 审计记录写入 t_ai_audit。
     * <p>Given：AuditAdvisor 同步落库 t_ai_audit
     * <p>When：调一次 /api/v1/admin/ai-gateway/test
     * <p>Then：t_ai_audit 新增一条记录，caller/model/tokens_in/tokens_out/elapsed_ms 字段完整
     */
    @Test
    @DisplayName("I-GW-05 审计记录写入 t_ai_audit：字段完整")
    void audit_shouldWriteRecordToTAiAudit() throws Exception {
        enqueueLlmResponse("审计测试响应", 30, 10);
        String token = loginAsAdmin();
        String prompt = "审计测试-" + System.currentTimeMillis();

        // 调用前记录审计条数
        Long countBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_ai_audit", Long.class);

        // 调用接口
        callAiGateway(token, prompt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 调用后审计应新增至少 1 条
        Long countAfter = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_ai_audit", Long.class);
        assertThat(countAfter).as("审计记录应新增").isGreaterThan(countBefore);

        // 查询最新一条审计记录，验证字段完整
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                "SELECT * FROM t_ai_audit ORDER BY create_time DESC, id DESC LIMIT 1");
        assertThat(records).as("应能查到审计记录").isNotEmpty();

        Map<String, Object> audit = records.get(0);
        assertThat(audit.get("caller")).as("caller 字段应完整").isEqualTo("ai-gateway-test");
        assertThat(audit.get("model")).as("model 字段应完整").isNotNull();
        assertThat(audit.get("tokens_in")).as("tokens_in 字段应完整").isNotNull();
        assertThat(audit.get("tokens_out")).as("tokens_out 字段应完整").isNotNull();
        assertThat(audit.get("elapsed_ms")).as("elapsed_ms 字段应完整").isNotNull();
        assertThat(audit.get("create_time")).as("create_time 字段应完整").isNotNull();
    }

    /**
     * I-GW-06 日预算超限降级。
     * <p>Given：预置 Redis ai:budget:tokens:{today} = 3000000（超过 daily-token-limit=2000000）
     * <p>When：调 /api/v1/admin/ai-gateway/test
     * <p>Then：BudgetAdvisor 抛 BusinessException(AI_BUDGET_EXCEEDED)，
     *         被 AiGatewayService try-catch 后返回降级文案 "AI 服务暂时不可用，请稍后再试"
     */
    @Test
    @DisplayName("I-GW-06 日预算超限降级：返回降级文案")
    void budgetExceeded_shouldReturnFallbackText() throws Exception {
        // given：预置 Redis 日 token 计数超限
        String todayKey = "ai:budget:tokens:" + LocalDate.now().toString();
        stringRedisTemplate.opsForValue().set(todayKey, "3000000");

        String token = loginAsAdmin();
        String prompt = "预算超限测试-" + System.currentTimeMillis();

        // when & then：应返回降级文案（BudgetAdvisor 抛异常 → AiGatewayService catch → fallback）
        callAiGateway(token, prompt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("AI 服务暂时不可用，请稍后再试"));

        // 清理：删除预置的 Redis key，避免影响后续测试
        stringRedisTemplate.delete(todayKey);
    }
}