package com.seckill.mall.analytics.tracking.aspect;

import com.seckill.mall.analytics.tracking.annotation.Tracking;
import com.seckill.mall.analytics.tracking.entity.UserEvent;
import com.seckill.mall.analytics.tracking.service.TrackService;
import com.seckill.mall.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 声明式埋点切面
 * <p>拦截 {@link Tracking} 注解方法，在方法正常返回后异步采集埋点：
 * <ol>
 *   <li>解析 {@code targetIdSpEL} 取 targetId（支持 {@code #paramName} 与 {@code #result}）</li>
 *   <li>从 SecurityContext 获取 userId（未登录为 null）</li>
 *   <li>构建 {@link UserEvent}，调 {@link TrackService#track(UserEvent)} 投递 MQ</li>
 * </ol>
 * <p>使用 {@code @Async("logExecutor")} 异步执行，避免埋点 IO 阻塞业务线程；
 * 切面内吞掉所有异常，保证埋点失败不影响主流程。
 * <p>SpEL 解析模式参考 {@link com.seckill.mall.aspect.OperationLogAspect}。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：TrackingAspect.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class TrackingAspect {

    private final TrackService trackService;
    private final SecurityUtils securityUtils;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    /**
     * 方法正常返回后异步埋点。
     * <p>{@code @Async} 与 {@code @AfterReturning} 组合：切面方法本身被异步执行，
     * 业务方法返回值通过 JoinPoint 传入，不阻塞调用方。
     *
     * @param joinPoint 连接点
     * @param result    方法返回值（Spring 自动注入，名称需与注解 returning 一致；此处用 args 绑定）
     * @param tracking  注解元数据
     */
    @AfterReturning(value = "@annotation(tracking)", returning = "result")
    @Async("logExecutor")
    public void afterReturning(JoinPoint joinPoint, Object result, Tracking tracking) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();

            UserEvent event = new UserEvent();
            event.setEventType(tracking.eventType());
            event.setTargetType(tracking.targetType().isEmpty() ? null : tracking.targetType());
            event.setTargetId(evaluateTargetId(tracking.targetIdSpEL(), method, joinPoint.getArgs(), result));
            event.setUserId(currentUserIdOrNull());

            trackService.track(event);
        } catch (Exception e) {
            // 埋点失败不得影响主流程
            log.warn("埋点切面处理异常: {}", e.getMessage(), e);
        }
    }

    /**
     * 解析 SpEL 取 targetId。
     * <p>上下文变量：方法参数（{@code #paramName}）+ 返回值（{@code #result}）。
     *
     * @param spel   SpEL 表达式
     * @param method 目标方法
     * @param args   方法参数
     * @param result 方法返回值
     * @return targetId，解析失败或空表达式返回 null
     */
    private Long evaluateTargetId(String spel, Method method, Object[] args, Object result) {
        if (spel == null || spel.isBlank()) {
            return null;
        }
        try {
            EvaluationContext context = new StandardEvaluationContext();
            // 暴露返回值
            context.setVariable("result", result);
            // 暴露方法参数
            String[] paramNames = discoverer.getParameterNames(method);
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length && i < args.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            Expression expression = parser.parseExpression(spel);
            Object value = expression.getValue(context);
            if (value == null) {
                return null;
            }
            if (value instanceof Number number) {
                return number.longValue();
            }
            return Long.valueOf(value.toString());
        } catch (Exception e) {
            log.warn("解析 targetIdSpEL 失败: spel={}, err={}", spel, e.getMessage());
            return null;
        }
    }

    /**
     * 安全获取当前 userId，未登录或异常返回 null（埋点允许匿名）。
     */
    private Long currentUserIdOrNull() {
        try {
            return securityUtils.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}