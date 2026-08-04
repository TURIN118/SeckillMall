package com.seckill.mall.config;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 全局序列化配置。
 *
 * <p>雪花算法生成的 {@code Long} 型 ID 达 19 位，超过 JavaScript {@code Number.MAX_SAFE_INTEGER}
 * （2^53-1，16 位），前端 {@code JSON.parse} 后会出现精度丢失（末尾几位被置 0）。
 * 此配置将 {@code Long}（含包装类型与基本类型）全局序列化为字符串，前端按字符串接收即可保留精度。</p>
 *
 * <p>采用 {@link Jackson2ObjectMapperBuilderCustomizer} 在 Spring Boot 自动配置基础上追加模块，
 * 不会破坏 {@code application.yml} 中已有的 {@code date-format}/{@code time-zone} 等配置。</p>
 *
 * <p><b>关键实现细节：</b>使用 {@code builder.modulesToInstall(module)} 而非 {@code builder.modules(module)}。
 * 后者会<b>替换</b>整个 Jackson 模块列表，从而清掉 Spring Boot 默认自动注册的 {@code JavaTimeModule}，
 * 导致所有含 {@code LocalDateTime} 等 Java 8 时间类型的接口序列化时报
 * {@code InvalidDefinitionException: Java 8 date/time type `java.time.LocalDateTime` not supported by default}。
 * {@code modulesToInstall} 仅追加自定义模块，保留自动发现的 {@code JavaTimeModule}。</p>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：JacksonConfig.java
 * 邮箱：nj651217@163.com
 */
@Configuration
public class JacksonConfig {

    /**
     * 注册自定义序列化模块：将 {@link Long} 与 {@code long} 统一序列化为字符串。
     *
     * @return Jackson 构建器定制器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonLongToStringCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            // Long.class：包装类型；Long.TYPE：基本类型 long
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            // 使用 modulesToInstall 追加模块，而非 modules 替换整个模块列表。
            // builder.modules(...) 会清空 Spring Boot 自动注册的 JavaTimeModule，
            // 导致 LocalDateTime 等 Java 8 时间类型序列化报错：
            //   InvalidDefinitionException: Java 8 date/time type `java.time.LocalDateTime` not supported by default
            // modulesToInstall 仅追加自定义模块，保留自动发现的 JavaTimeModule。
            builder.modulesToInstall(module);
        };
    }
}