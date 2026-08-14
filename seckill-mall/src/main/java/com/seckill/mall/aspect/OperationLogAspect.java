package com.seckill.mall.aspect;

import com.seckill.mall.dto.LoginRequest;
import com.seckill.mall.entity.OperationLog;
import com.seckill.mall.entity.User;
import com.seckill.mall.mapper.UserMapper;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.utils.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OperationLogAspect.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogRecorder operationLogRecorder;
    private final UserMapper userMapper;
    private final SecurityUtils securityUtils;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    @AfterReturning("@annotation(com.seckill.mall.annotation.OperationLog)")
    public void afterReturning(JoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            com.seckill.mall.annotation.OperationLog operationLog =
                    method.getAnnotation(com.seckill.mall.annotation.OperationLog.class);
            if (operationLog == null) {
                return;
            }

            OperationLog entity = new OperationLog();
            entity.setModule(operationLog.module());
            entity.setAction(operationLog.action());
            entity.setTargetType(operationLog.targetType().isEmpty()
                    ? operationLog.module() : operationLog.targetType());
            entity.setTargetId(evaluateTargetId(operationLog.targetIdSpEL(), method, joinPoint.getArgs()));
            entity.setIpAddress(IpUtils.getClientIp());

            fillOperator(entity, joinPoint.getArgs());

            operationLogRecorder.record(entity);
        } catch (Exception e) {
            // 日志记录失败不得影响主流程
            log.warn("操作日志切面处理异常: {}", e.getMessage(), e);
        }
    }

    private void fillOperator(OperationLog entity, Object[] args) {
        try {
            entity.setOperatorId(securityUtils.getCurrentUserId());
        } catch (Exception e) {
            // SecurityContext 无用户（如登录场景），尝试从参数回查
            if (args != null) {
                for (Object arg : args) {
                    if (arg instanceof LoginRequest loginReq) {
                        try {
                            User user = userMapper.findByUsername(loginReq.getUsername());
                            if (user != null) {
                                entity.setOperatorId(user.getId());
                            }
                        } catch (Exception ex) {
                            log.debug("从LoginRequest回查操作人失败: {}", ex.getMessage());
                        }
                        break;
                    }
                }
            }
            if (entity.getOperatorId() == null) {
                log.debug("无法获取当前操作人信息: {}", e.getMessage());
            }
        }
    }

    private String evaluateTargetId(String spel, Method method, Object[] args) {
        if (spel == null || spel.isBlank()) {
            return null;
        }
        try {
            EvaluationContext context = new StandardEvaluationContext();
            String[] paramNames = discoverer.getParameterNames(method);
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length && i < args.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                }
            }
            Expression expression = parser.parseExpression(spel);
            Object value = expression.getValue(context);
            return value == null ? null : value.toString();
        } catch (Exception e) {
            log.warn("解析 targetIdSpEL 失败: spel={}, err={}", spel, e.getMessage());
            return null;
        }
    }

}
