package com.seckill.mall.ai.gateway.advisor;

import com.seckill.mall.cache.RedisKeyConstants;
import com.seckill.mall.cache.RedisService;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.utils.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

/**
 * AI 预算护栏 Advisor（T7 实现）
 * <p>按"日 token 上限 + 月成本上限"两档护栏，超阈值时直接拒绝调用（抛
 * {@link BusinessException}({@link ErrorCode#AI_BUDGET_EXCEEDED})），
 * 避免超预算时还浪费令牌桶配额与 LLM 调用费用。
 *
 * <h3>实现说明</h3>
 * <ul>
 *   <li>同步调用 {@link #aroundCall}：先校验预算，通过则透传 chain；超限抛异常。</li>
 *   <li>流式调用 {@link #aroundStream}：先校验预算，通过则透传 chain；超限以
 *       {@code Flux.error} 透传给上游。</li>
 *   <li>Redis 读取异常或值解析失败时按 0 处理（fail-open，避免 Redis 故障阻断全部 AI 调用）。</li>
 *   <li>预算计数由 {@link AuditAdvisor} 在调用成功后累加（tokens/cost），本 Advisor 仅做读取校验。</li>
 * </ul>
 *
 * <h3>Redis key 规约</h3>
 * <ul>
 *   <li>日 token 计数：{@code ai:budget:tokens:{yyyy-MM-dd}}，值为当日累计 token 数（long）</li>
 *   <li>月成本计数：{@code ai:budget:cost:{yyyy-MM}}，值为当月累计成本（double，¥）</li>
 * </ul>
 *
 * <h3>toolContext 约定</h3>
 * <ul>
 *   <li>{@code caller} —— 调用方标识（如 "shopping-assistant"、"aigc"），缺省 "default"</li>
 * </ul>
 *
 * <p>getOrder()=5（最先执行，在 RateLimitAdvisor=10 之前，避免超预算时还浪费令牌桶配额）；
 * getName()="BudgetAdvisor"。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：BudgetAdvisor.java
 * 邮箱：nj651217@163.com
 */
public class BudgetAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private static final Logger log = LoggerFactory.getLogger(BudgetAdvisor.class);

    /** toolContext 中调用方标识键 */
    private static final String CTX_CALLER = "caller";
    private static final String DEFAULT_CALLER = "default";

    private final RedisService redisService;

    /** 日 token 上限（默认 200 万） */
    private final long dailyTokenLimit;
    /** 月成本上限（¥，默认 5000） */
    private final double monthlyCostLimit;

    public BudgetAdvisor(RedisService redisService,
                         long dailyTokenLimit,
                         double monthlyCostLimit) {
        this.redisService = redisService;
        this.dailyTokenLimit = dailyTokenLimit;
        this.monthlyCostLimit = monthlyCostLimit;
        log.info("BudgetAdvisor 初始化：dailyTokenLimit={}, monthlyCostLimit={}¥",
                dailyTokenLimit, monthlyCostLimit);
    }

    @Override
    public String getName() {
        return "BudgetAdvisor";
    }

    @Override
    public int getOrder() {
        // 最先执行，在 RateLimitAdvisor(10) 之前，避免超预算时还浪费令牌桶配额
        return 5;
    }

    /**
     * 同步调用预算校验：先校验日 token / 月成本，通过则透传 chain；超限抛 {@link BusinessException}。
     */
    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        checkBudget(request);
        return chain.nextAroundCall(request);
    }

    /**
     * 流式调用预算校验：先校验，通过则透传 chain；超限以 {@code Flux.error} 透传给上游。
     */
    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest request, StreamAroundAdvisorChain chain) {
        try {
            checkBudget(request);
        } catch (BusinessException ex) {
            return Flux.error(ex);
        }
        return chain.nextAroundStream(request);
    }

    /**
     * 预算护栏校验：读取 Redis 日 token 计数与月成本计数，超阈值抛 {@link BusinessException}。
     * <p>Redis 异常或值解析失败时按 0 处理（fail-open），避免 Redis 故障阻断全部 AI 调用。
     */
    private void checkBudget(AdvisedRequest request) {
        Map<String, Object> toolContext = request.toolContext();
        String caller = MapUtils.readString(toolContext, CTX_CALLER, DEFAULT_CALLER);

        // 1. 校验日 token 预算
        String dailyKey = RedisKeyConstants.BUDGET_AI + LocalDate.now().toString();
        long dailyTokens = readLong(dailyKey);
        if (dailyTokens > dailyTokenLimit) {
            log.warn("AI 预算护栏拦截（日 token 超限）caller={} dailyTokens={} limit={}",
                    caller, dailyTokens, dailyTokenLimit);
            throw new BusinessException(ErrorCode.AI_BUDGET_EXCEEDED);
        }

        // 2. 校验月成本预算
        String monthlyKey = RedisKeyConstants.BUDGET_AI_COST + YearMonth.now().toString();
        double monthlyCost = readDouble(monthlyKey);
        if (monthlyCost > monthlyCostLimit) {
            log.warn("AI 预算护栏拦截（月成本超限）caller={} monthlyCost={}¥ limit={}¥",
                    caller, monthlyCost, monthlyCostLimit);
            throw new BusinessException(ErrorCode.AI_BUDGET_EXCEEDED);
        }

        if (log.isDebugEnabled()) {
            log.debug("AI 预算护栏放行 caller={} dailyTokens={}/{} monthlyCost={}/{}",
                    caller, dailyTokens, dailyTokenLimit, monthlyCost, monthlyCostLimit);
        }
    }

    /**
     * 从 Redis 读取 long 值，key 不存在或解析失败时返回 0（fail-open）。
     */
    private long readLong(String key) {
        try {
            String value = redisService.get(key);
            if (!StringUtils.hasText(value)) {
                return 0L;
            }
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            log.warn("AI 预算护栏读取 long 失败，按 0 处理 key={} err={}", key, e.getMessage());
            return 0L;
        }
    }

    /**
     * 从 Redis 读取 double 值，key 不存在或解析失败时返回 0.0（fail-open）。
     */
    private double readDouble(String key) {
        try {
            String value = redisService.get(key);
            if (!StringUtils.hasText(value)) {
                return 0.0;
            }
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            log.warn("AI 预算护栏读取 double 失败，按 0 处理 key={} err={}", key, e.getMessage());
            return 0.0;
        }
    }

}
