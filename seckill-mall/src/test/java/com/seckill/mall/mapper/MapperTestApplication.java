package com.seckill.mall.mapper;

import com.seckill.mall.config.MetaObjectHandler;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/**
 * Mapper 切片测试专用引导类：仅装配 DataSource + MyBatis-Plus + Mapper 扫描，
 * 屏蔽 Redis/RabbitMQ/Mail/Security/Redisson 等中间件自动配置，使用 H2 内存库。
 */
@SpringBootApplication(
        scanBasePackages = "com.seckill.mall.mapper",
        exclude = {
                DataSourceAutoConfiguration.class,
                RedisAutoConfiguration.class,
                RedisRepositoriesAutoConfiguration.class,
                RabbitAutoConfiguration.class,
                MailSenderAutoConfiguration.class,
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class,
                UserDetailsServiceAutoConfiguration.class
        })
@MapperScan("com.seckill.mall.mapper")
@Import(MetaObjectHandler.class)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:seckill_mapper_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema-h2.sql",
        "spring.sql.init.continue-on-error=false",
        "mybatis-plus.mapper-locations=classpath:mapper/*.xml",
        "mybatis-plus.global-config.db-config.id-type=ASSIGN_ID",
        "mybatis-plus.global-config.db-config.logic-delete-field=isDeleted",
        "mybatis-plus.global-config.db-config.logic-delete-value=1",
        "mybatis-plus.global-config.db-config.logic-not-delete-value=0",
        "mybatis-plus.configuration.map-underscore-to-camel-case=true"
})
public class MapperTestApplication {
}
