package com.seckill.mall.analytics.tracking.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式埋点注解
 * <p>标注于业务方法上，由 {@link com.seckill.mall.analytics.tracking.aspect.TrackingAspect}
 * 在方法正常返回后异步采集埋点，投递 MQ 落库。
 * <p>使用示例：
 * <pre>{@code
 * @Tracking(eventType = "ORDER", targetType = "SECKILL", targetIdSpEL = "#result.id")
 * public SeckillOrder createOrder(Long seckillId, Long userId, String requestId) { ... }
 * }</pre>
 * <p>{@code targetIdSpEL} 支持 SpEL 表达式，可引用方法参数（{@code #paramName}）
 * 或返回值（{@code #result}）。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：Tracking.java
 * 邮箱：nj651217@163.com
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Tracking {

    /** 事件类型：VIEW/CLICK/ADD_CART/FAVORITE/ORDER/SEARCH */
    String eventType();

    /** 目标类型：PRODUCT/CATEGORY/SECKILL/ORDER（可空） */
    String targetType() default "";

    /**
     * SpEL 表达式取 targetId，可引用方法参数（{@code #paramName}）或返回值（{@code #result}）。
     * 例：{@code "#result.id"}、{@code "#productId"}、{@code "#seckillId"}
     */
    String targetIdSpEL() default "";
}