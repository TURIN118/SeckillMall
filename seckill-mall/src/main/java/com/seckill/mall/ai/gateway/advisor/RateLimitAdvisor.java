package com.seckill.mall.ai.gateway.advisor;

import com.seckill.mall.cache.RedisKeyConstants;
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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.Map;

/**
 * AI 限流 Advisor（T4 实现）
 * <p>基于 Redis 令牌桶，按用户/IP/模型维度限流，复用现有 RateLimitAspect 的 Lua 脚本（lua/rate_limit.lua）。
 * <p>key 规约：{@code rate:ai:{caller}:{userId或ip}}，登录用户走 user 维度（user-capacity/user-rate），
 * 未登录走 IP 维度（ip-capacity/ip-rate）。
 * <p>调用方需在 toolContext 中传入：
 * <ul>
 *   <li>{@code caller} —— 调用方标识（如 "chat"、"seckill-ai"），缺省 "default"</li>
 *   <li>{@code userId} —— 登录用户 ID（优先）</li>
 *   <li>{@code ip}    —— 客户端 IP（userId 缺失时使用）</li>
 *   <li>{@code model} —— 模型名（可选，拼入 key 实现模型维度限流）</li>
 * </ul>
 * <p>getOrder()=10，最先执行；getName()="RateLimitAdvisor"。
 */
public class RateLimitAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAdvisor.class);

    /** 单次请求消耗令牌数 */
    private static final long TOKEN_COST = 1L;
    /** toolContext 中调用方标识键 */
    private static final String CTX_CALLER = "caller";
    /** toolContext 中用户 ID 键 */
    private static final String CTX_USER_ID = "userId";
    /** toolContext 中客户端 IP 键 */
    private static final String CTX_IP = "ip";
    /** toolContext 中模型名键 */
    private static final String CTX_MODEL = "model";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;

    /** 登录用户令牌桶容量（突发上限） */
    private final int userCapacity;
    /** 登录用户令牌补充速率（tokens/sec） */
    private final int userRate;
    /** 未登录用户（IP 维度）令牌桶容量 */
    private final int ipCapacity;
    /** 未登录用户（IP 维度）令牌补充速率 */
    private final int ipRate;
    /** 时间窗口秒数，>0 时由 Lua 按 capacity/seconds 计算补充速率 */
    private final int windowSeconds;

    public RateLimitAdvisor(StringRedisTemplate redisTemplate,
                            DefaultRedisScript<Long> rateLimitScript,
                            int userCapacity, int userRate,
                            int ipCapacity, int ipRate,
                            int windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.rateLimitScript = rateLimitScript;
        this.userCapacity = userCapacity;
        this.userRate = userRate;
        this.ipCapacity = ipCapacity;
        this.ipRate = ipRate;
        this.windowSeconds = windowSeconds;
        log.info("RateLimitAdvisor 初始化：user(capacity={},rate={}), ip(capacity={},rate={}), windowSeconds={}",
                userCapacity, userRate, ipCapacity, ipRate, windowSeconds);
    }

    @Override
    public String getName() {
        return "RateLimitAdvisor";
    }

    @Override
    public int getOrder() {
        // 最先执行，确保超限请求不进入后续 Advisor/模型调用
        return 10;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        checkRateLimit(request);
        return chain.nextAroundCall(request);
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest request, StreamAroundAdvisorChain chain) {
        // 流式调用同样在发起前校验令牌；超限以 Flux.error 透传给上游
        try {
            checkRateLimit(request);
        } catch (BusinessException ex) {
            return Flux.error(ex);
        }
        return chain.nextAroundStream(request);
    }

    /**
     * 令牌桶限流校验：从 toolContext 解析 caller/userId/ip/model，构建 key 并执行 Lua 脚本。
     * <p>超限抛 {@link BusinessException}({@link ErrorCode#AI_RATE_LIMIT_EXCEEDED})。
     */
    private void checkRateLimit(AdvisedRequest request) {
        Map<String, Object> toolContext = request.toolContext();

        String caller = MapUtils.readString(toolContext, CTX_CALLER, "default");
        String userId = MapUtils.readString(toolContext, CTX_USER_ID, null);
        String ip = MapUtils.readString(toolContext, CTX_IP, null);
        String model = MapUtils.readString(toolContext, CTX_MODEL, null);

        // 限流主体：优先 userId，其次 ip，最后 unknown
        String subject;
        int capacity;
        int rate;
        if (StringUtils.hasText(userId)) {
            subject = userId;
            capacity = userCapacity;
            rate = userRate;
        } else if (StringUtils.hasText(ip)) {
            subject = ip;
            capacity = ipCapacity;
            rate = ipRate;
        } else {
            subject = "unknown";
            capacity = ipCapacity;
            rate = ipRate;
        }

        // key = rate:ai:{caller}:{subject}[:model]
        StringBuilder keyBuilder = new StringBuilder(RedisKeyConstants.RATE_AI)
                .append(caller).append(':').append(subject);
        if (StringUtils.hasText(model)) {
            keyBuilder.append(':').append(model);
        }
        String limitKey = keyBuilder.toString();

        long nowSec = System.currentTimeMillis() / 1000L;
        Long allowed = redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(limitKey),
                String.valueOf(capacity),
                String.valueOf(rate),
                String.valueOf(nowSec),
                String.valueOf(TOKEN_COST),
                String.valueOf(windowSeconds));

        if (allowed == null || allowed == 0L) {
            log.warn("AI 限流拦截 key={} caller={} subject={} model={} capacity={} rate={} window={}s",
                    limitKey, caller, subject, model, capacity, rate, windowSeconds);
            throw new BusinessException(ErrorCode.AI_RATE_LIMIT_EXCEEDED);
        }
        if (log.isDebugEnabled()) {
            log.debug("AI 限流放行 key={} caller={} subject={} model={}", limitKey, caller, subject, model);
        }
    }

}
