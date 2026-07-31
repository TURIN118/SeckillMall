package com.seckill.mall.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：RateLimit.java
 * 邮箱：nj651217@163.com
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流 key 前缀（拼装到 rate:seckill:{key}:{userId}）。
     * 空字符串表示使用方法名作为前缀。
     */
    String key() default "";

    /**
     * 令牌桶容量：允许突发请求的最大数量。
     */
    int capacity() default 1;

    /**
     * 令牌补充速率（tokens/sec）。
     */
    int rate() default 1;

    /**
     * 时间窗口秒数：用于 IP 限流时换算容量。
     * 用户级秒杀限流默认 1 秒，IP 限流可设为 60。
     */
    int seconds() default 1;
}
