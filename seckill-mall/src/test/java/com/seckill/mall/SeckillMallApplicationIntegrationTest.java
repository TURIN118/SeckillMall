package com.seckill.mall;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillMallApplicationIntegrationTest.java
 * 邮箱：nj651217@163.com
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class SeckillMallApplicationIntegrationTest {

    // MySQL 8.0：业务数据库
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("seckill_mall")
            .withUsername("test")
            .withPassword("test123");

    // Redis 7：缓存与限流
    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    // RabbitMQ 3.13：秒杀异步下单
    @Container
    static RabbitMQContainer rabbitmq = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3.13-management"));

    @DynamicPropertySource
    static void injectContainerProperties(DynamicPropertyRegistry registry) {
        // MySQL
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
    }

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("contextLoads：Testcontainers 启动 MySQL/Redis/RabbitMQ 后应用上下文可加载")
    void contextLoads() {
        // ApplicationContext 非空即代表所有 Bean 装配成功
        org.assertj.core.api.Assertions.assertThat(applicationContext).isNotNull();
    }
}
