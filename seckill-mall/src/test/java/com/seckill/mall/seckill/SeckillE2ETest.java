package com.seckill.mall.seckill;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.cache.RedisKeyConstants;
import com.seckill.mall.service.CaptchaService;
import com.seckill.mall.service.SeckillGoodsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillE2ETest.java
 * 邮箱：nj651217@163.com
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class SeckillE2ETest {

    private static final Long E2E_SECKILL_ID = 6001L;

    // MySQL 8.0：业务数据库
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("seckill_mall")
            .withUsername("test")
            .withPassword("test123");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3.13-management"));

    @DynamicPropertySource
    static void injectContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
    }

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private SeckillGoodsService seckillGoodsService;
    @Autowired
    private RedissonClient redissonClient;

    // 注册接口依赖验证码，E2E 中绕过验证码逻辑以聚焦秒杀链路
    @MockBean
    private CaptchaService captchaService;

    @BeforeEach
    void setUp() {
        // 验证码始终放行
        given(captchaService.verifyCaptcha(anyString(), anyString())).willReturn(true);

        // 插入一条时间窗口覆盖当前时刻的 ACTIVE 秒杀活动（data.sql 中示例数据已过期）
        jdbcTemplate.update(
                "INSERT INTO t_seckill_goods (id, product_id, seckill_price, stock_count, available_count, " +
                        " start_time, end_time, status, creator_id, is_deleted) " +
                        "VALUES (?,?,?,?,?, DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 1 HOUR), 'ACTIVE', 1, 0) " +
                        "ON DUPLICATE KEY UPDATE available_count=100, status='ACTIVE', " +
                        " start_time=DATE_SUB(NOW(), INTERVAL 1 HOUR), end_time=DATE_ADD(NOW(), INTERVAL 1 HOUR)",
                E2E_SECKILL_ID, 1001L, new java.math.BigDecimal("5999.00"), 100, 100);

        // 布隆过滤器加入测试活动并预热 Redis 缓存
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisKeyConstants.SECKILL_BLOOM_GOODS);
        if (!bloomFilter.isExists()) {
            bloomFilter.tryInit(10000L, 0.001);
        }
        bloomFilter.add(E2E_SECKILL_ID);
        seckillGoodsService.preheatSeckill(E2E_SECKILL_ID);
    }

    @Test
    @DisplayName("秒杀完整链路：注册→登录→令牌→下单→轮询结果→支付→校验订单状态")
    void seckillFullFlow() throws Exception {
        String username = "e2e_" + System.currentTimeMillis();
        String password = "pass123";
        String phone = "138" + (System.currentTimeMillis() % 100000000);

        // 1. 注册新用户
        String registerBody = String.format(
                "{\"username\":\"%s\",\"password\":\"%s\",\"phone\":\"%s\",\"captchaKey\":\"k\",\"captchaCode\":\"c\"}",
                username, password, phone);
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 2. 登录获取 token
        String loginBody = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode loginNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String accessToken = loginNode.at("/data/accessToken").asText();

        // 3. 查看秒杀列表
        mockMvc.perform(get("/api/v1/seckill/list")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 4. 获取秒杀令牌
        MvcResult tokenResult = mockMvc.perform(get("/api/v1/seckill/{seckillId}/token", E2E_SECKILL_ID)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        String seckillToken = objectMapper
                .readTree(tokenResult.getResponse().getContentAsString())
                .at("/data").asText();

        // 5. 执行秒杀（携带防重放签名头 + 秒杀令牌头）
        String uri = "/api/v1/seckill/" + E2E_SECKILL_ID;
        SeckillSign sign = signSeckillRequest(uri);
        MvcResult seckillResult = mockMvc.perform(post(uri)
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Seckill-Token", seckillToken)
                        .header("X-Sign", sign.sign())
                        .header("X-Timestamp", sign.timestamp())
                        .header("X-Nonce", sign.nonce()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        JsonNode seckillNode = objectMapper.readTree(seckillResult.getResponse().getContentAsString());
        String requestId = seckillNode.at("/data/requestId").asText();

        // 6. 轮询秒杀结果直到非排队中
        Long orderId = null;
        for (int i = 0; i < 20; i++) {
            MvcResult resultResult = mockMvc.perform(get("/api/v1/seckill/{seckillId}/result", E2E_SECKILL_ID)
                            .param("requestId", requestId)
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode node = objectMapper.readTree(resultResult.getResponse().getContentAsString());
            int status = node.at("/data/status").asInt();
            if (status != 0) {
                assertThat(status).as("秒杀结果应成功").isEqualTo(1);
                orderId = node.at("/data/orderId").asLong();
                break;
            }
            Thread.sleep(500L);
        }
        assertThat(orderId).as("应获取到订单 ID").isNotNull();

        // 7. 支付订单
        mockMvc.perform(post("/api/v1/orders/{orderId}/pay", orderId)
                        .param("payMethod", "ALIPAY")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PAID"));

        // 8. 验证订单状态为 PAID
        mockMvc.perform(get("/api/v1/orders/{orderId}", orderId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    /**
     * 按 ReplayProtectionFilter 的算法生成防重放签名：HMAC-SHA256(secret, timestamp + nonce + uri)。
     */
    private SeckillSign signSeckillRequest(String uri) {
        String secret = "test-seckill-sign-secret-2024";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String payload = timestamp + nonce + uri;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String sign = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return new SeckillSign(sign, timestamp, nonce);
        } catch (Exception e) {
            throw new IllegalStateException("签名计算失败", e);
        }
    }

    private record SeckillSign(String sign, String timestamp, String nonce) {
    }
}
