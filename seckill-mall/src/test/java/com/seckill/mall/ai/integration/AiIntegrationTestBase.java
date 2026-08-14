package com.seckill.mall.ai.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 集成测试共享基类（T20）。
 * <p>复用 {@link com.seckill.mall.seckill.SeckillE2ETest} 的 Testcontainers 编排模式，
 * 额外引入 {@link MockWebServer} 模拟 DeepSeek/OpenAI 协议 LLM 服务端，
 * 并执行 {@code sql/02_ai_tables.sql} 初始化 AI 相关表（t_user_event/t_ai_audit/t_ai_conversation/t_ai_message）。
 *
 * <h3>容器编排</h3>
 * <ul>
 *   <li>MySQL 8.0：业务数据库 + AI 表</li>
 *   <li>Redis 7：限流/缓存/预算计数</li>
 *   <li>RabbitMQ 3.13：异步消息</li>
 *   <li>MockWebServer：模拟 OpenAI 兼容协议 LLM 服务端</li>
 * </ul>
 *
 * <h3>属性注入</h3>
 * <p>通过 {@code @DynamicPropertySource} 将容器连接信息与 {@code spring.ai.openai.base-url}
 * （指向 MockWebServer）注入 Spring 上下文。
 *
 * <h3>辅助方法</h3>
 * <ul>
 *   <li>{@link #enqueueLlmResponse(String)}：向 MockWebServer enqueue OpenAI 协议响应</li>
 *   <li>{@link #enqueueLlmResponse(String, int, int)}：带 token usage 的响应</li>
 *   <li>{@link #loginAsAdmin()}：登录 admin 用户获取 JWT token</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AiIntegrationTestBase.java
 * 邮箱：nj651217@163.com
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
public abstract class AiIntegrationTestBase {

    // ==================== 容器定义 ====================

    /** MySQL 8.0：业务数据库 + AI 表 */
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("seckill_mall")
            .withUsername("test")
            .withPassword("test123");

    /** Redis 7：限流/缓存/预算计数 */
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    /** RabbitMQ 3.13：异步消息 */
    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3.13-management"));

    /** MockWebServer：模拟 DeepSeek/OpenAI 协议 LLM 服务端 */
    static MockWebServer mockWebServer;

    // ==================== 属性注入 ====================

    @DynamicPropertySource
    static void injectContainerProperties(DynamicPropertyRegistry registry) {
        // 启动 MockWebServer（在属性注入阶段启动，确保 base-url 可解析）
        mockWebServer = new MockWebServer();
        try {
            mockWebServer.start();
        } catch (IOException e) {
            throw new IllegalStateException("MockWebServer 启动失败", e);
        }

        // 数据源
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");

        // Redis
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");

        // RabbitMQ
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);

        // AI 配置：指向 MockWebServer 模拟 LLM 服务端
        registry.add("spring.ai.openai.base-url", () -> mockWebServer.url("/").toString());
        registry.add("spring.ai.openai.api-key", () -> "test-api-key-for-integration");
    }

    // ==================== Schema 初始化 ====================

    /**
     * 执行 02_ai_tables.sql 初始化 AI 表（t_user_event/t_ai_audit/t_ai_conversation/t_ai_message）。
     * <p>application-test.yml 已通过 spring.sql.init 加载 schema.sql + data.sql，
     * 但 02_ai_tables.sql 未被自动加载，需在此手动执行。
     */
    @BeforeAll
    static void initAiSchema() throws SQLException {
        try (Connection conn = DriverManager.getConnection(
                mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("sql/02_ai_tables.sql"));
        }
    }

    /**
     * 关闭 MockWebServer 释放端口。
     */
    @AfterAll
    static void tearDownMockWebServer() throws IOException {
        if (mockWebServer != null) {
            mockWebServer.shutdown();
        }
    }

    // ==================== 共享组件 ====================

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    // ==================== 辅助方法 ====================

    /**
     * 向 MockWebServer enqueue OpenAI 协议格式的 LLM 响应（默认 token usage）。
     *
     * @param content 模型响应文本
     */
    protected void enqueueLlmResponse(String content) throws Exception {
        enqueueLlmResponse(content, 50, 20);
    }

    /**
     * 向 MockWebServer enqueue OpenAI 协议格式的 LLM 响应（带 token usage）。
     * <p>响应格式：
     * <pre>{@code
     * {
     *   "id": "chatcmpl-test",
     *   "object": "chat.completion",
     *   "choices": [
     *     {
     *       "index": 0,
     *       "message": {"role": "assistant", "content": "..."},
     *       "finish_reason": "stop"
     *     }
     *   ],
     *   "usage": {"prompt_tokens": 50, "completion_tokens": 20, "total_tokens": 70}
     * }
     * }</pre>
     *
     * @param content          模型响应文本
     * @param promptTokens     输入 token 数
     * @param completionTokens 输出 token 数
     */
    protected void enqueueLlmResponse(String content, int promptTokens, int completionTokens) throws Exception {
        ObjectNode response = objectMapper.createObjectNode();
        response.put("id", "chatcmpl-test");
        response.put("object", "chat.completion");

        ArrayNode choices = response.putArray("choices");
        ObjectNode choice = choices.addObject();
        choice.put("index", 0);
        ObjectNode message = choice.putObject("message");
        message.put("role", "assistant");
        message.put("content", content);
        choice.put("finish_reason", "stop");

        ObjectNode usage = response.putObject("usage");
        usage.put("prompt_tokens", promptTokens);
        usage.put("completion_tokens", completionTokens);
        usage.put("total_tokens", promptTokens + completionTokens);

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(response))
                .setHeader("Content-Type", "application/json"));
    }

    /**
     * 登录 admin 用户获取 JWT access token。
     * <p>为确保密码已知，先用 {@link PasswordEncoder} 将 admin 密码重置为 {@code "admin123"}，
     * 再调用 {@code /api/v1/auth/login} 获取 token。
     *
     * @return JWT access token
     */
    protected String loginAsAdmin() throws Exception {
        // 重置 admin 密码为已知明文，避免依赖 data.sql 中的密码哈希猜测
        jdbcTemplate.update(
                "UPDATE t_user SET password = ?, status = 'ACTIVE', is_deleted = 0 WHERE username = 'admin'",
                passwordEncoder.encode("admin123"));

        String loginBody = "{\"username\":\"admin\",\"password\":\"admin123\"}";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.at("/data/accessToken").asText();
    }

    /**
     * 调用 AI 网关调试接口 {@code /api/v1/admin/ai-gateway/test}。
     *
     * @param token  JWT access token
     * @param prompt 请求 prompt
     * @return ResultActions 用于后续断言（支持链式 .andExpect()）
     */
    protected ResultActions callAiGateway(String token, String prompt) throws Exception {
        String body = String.format("{\"prompt\":\"%s\"}", prompt);
        return mockMvc.perform(post("/api/v1/admin/ai-gateway/test")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body));
    }
}