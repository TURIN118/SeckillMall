package com.seckill.mall.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OperationLog.java
 * 邮箱：nj651217@163.com
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {

    String module();

    String action();

    /**
     * 目标 ID 的 SpEL 表达式，引用方法参数，例如 "#userId"。
     */
    String targetIdSpEL() default "";

    /**
     * 操作目标类型，例如 USER/PRODUCT/SECKILL，缺省取 module。
     */
    String targetType() default "";
}
