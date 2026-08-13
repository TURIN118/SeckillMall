package com.seckill.mall.ai.gateway.advisor;

import com.seckill.mall.cache.RedisKeyConstants;
import com.seckill.mall.cache.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * AI 语义缓存 Advisor（T5 实现）
 * <p>基于 Redis 对 prompt 做 SHA-256 哈希精确匹配缓存：命中直接返回，未命中调 LLM 后写缓存。
 * <p>缓存 key 规约：{@code ai:cache:{sha256(systemText+userText)}}，TTL {@link #CACHE_TTL_SECONDS} 秒。
 * <p>流式调用不缓存（{@link #aroundStream} 直接透传 chain）。
 * <p>getOrder()=20（在 RateLimitAdvisor=10 之后执行）；getName()="SemanticCacheAdvisor"。
 *
 * <h3>实现说明</h3>
 * <ul>
 *   <li>缓存命中：从 Redis 读取响应文本，构造 {@link AssistantMessage} → {@link Generation}
 *       → {@link ChatResponse} → {@link AdvisedResponse} 直接返回，不调用后续 chain。</li>
 *   <li>缓存未命中：调用 {@code chain.nextAroundCall(request)} 获取响应，提取首条 Generation
 *       的 AssistantMessage 文本写入 Redis（带 TTL），再返回原响应。</li>
 *   <li>哈希失败、Redis 异常、响应内容为空等场景均降级为不缓存（直接调 chain），保证可用性。</li>
 * </ul>
 */
public class SemanticCacheAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private static final Logger log = LoggerFactory.getLogger(SemanticCacheAdvisor.class);

    /** 缓存 TTL：3600 秒（1 小时） */
    private static final long CACHE_TTL_SECONDS = 3600L;

    private final RedisService redisService;

    public SemanticCacheAdvisor(RedisService redisService) {
        this.redisService = redisService;
        log.info("SemanticCacheAdvisor 初始化：Redis SHA-256 prompt 哈希缓存，TTL={}s", CACHE_TTL_SECONDS);
    }

    @Override
    public String getName() {
        return "SemanticCacheAdvisor";
    }

    @Override
    public int getOrder() {
        // 在 RateLimitAdvisor(10) 之后执行，限流放行后才查缓存
        return 20;
    }

    /**
     * 同步调用缓存逻辑：先查 Redis，命中构造响应直接返回；未命中调 chain 后写缓存。
     */
    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        String prompt = extractPrompt(request);
        String cacheKey = buildCacheKey(prompt);

        // 1. 查缓存
        if (cacheKey != null) {
            try {
                String cached = redisService.get(cacheKey);
                if (StringUtils.hasText(cached)) {
                    if (log.isDebugEnabled()) {
                        log.debug("AI 缓存命中 key={} promptLen={}", cacheKey, prompt.length());
                    }
                    return buildCachedResponse(request, cached);
                }
            } catch (Exception e) {
                // Redis 异常降级：不缓存，直接调 chain
                log.warn("AI 缓存读取异常，降级直调 key={} err={}", cacheKey, e.getMessage());
            }
        }

        // 2. 未命中或降级：调后续 chain
        AdvisedResponse response = chain.nextAroundCall(request);

        // 3. 写缓存
        if (cacheKey != null) {
            writeCacheAsync(cacheKey, response);
        }

        return response;
    }

    /**
     * 流式调用不缓存，直接透传 chain。
     */
    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest request, StreamAroundAdvisorChain chain) {
        return chain.nextAroundStream(request);
    }

    /**
     * 提取 prompt：systemText + userText。两者皆空时返回空串（不缓存）。
     */
    private String extractPrompt(AdvisedRequest request) {
        String system = request.systemText();
        String user = request.userText();
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(system)) {
            sb.append(system);
        }
        if (StringUtils.hasText(user)) {
            sb.append(user);
        }
        return sb.toString();
    }

    /**
     * 构建缓存 key：{@code ai:cache:{sha256(prompt)}}。
     * <p>prompt 为空时返回 null（不缓存）；SHA-256 失败时降级为 hashCode 十六进制。
     */
    private String buildCacheKey(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(prompt.getBytes(StandardCharsets.UTF_8));
            return RedisKeyConstants.CACHE_AI + HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.warn("SHA-256 哈希失败，降级 hashCode key err={}", e.getMessage());
            return RedisKeyConstants.CACHE_AI + Integer.toHexString(prompt.hashCode());
        }
    }

    /**
     * 构造缓存命中的 {@link AdvisedResponse}，不调用后续 chain。
     * <p>链路：{@code cachedText → AssistantMessage → Generation → ChatResponse → AdvisedResponse}。
     * <p>adviseContext 复用当前请求的上下文。
     */
    private AdvisedResponse buildCachedResponse(AdvisedRequest request, String cachedContent) {
        AssistantMessage assistantMessage = new AssistantMessage(cachedContent);
        Generation generation = new Generation(assistantMessage);
        ChatResponse chatResponse = new ChatResponse(List.of(generation));
        Map<String, Object> adviseContext = request.adviseContext();
        return new AdvisedResponse(chatResponse, adviseContext);
    }

    /**
     * 写缓存：提取响应首条 Generation 的 AssistantMessage 文本写入 Redis（带 TTL）。
     * <p>响应内容为空或提取异常时不写缓存。同步执行但异常不影响主流程。
     */
    private void writeCacheAsync(String cacheKey, AdvisedResponse response) {
        try {
            String content = extractResponseContent(response);
            if (!StringUtils.hasText(content)) {
                return;
            }
            redisService.set(cacheKey, content, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
            if (log.isDebugEnabled()) {
                log.debug("AI 缓存写入 key={} contentLen={}", cacheKey, content.length());
            }
        } catch (Exception e) {
            log.warn("AI 缓存写入异常 key={} err={}", cacheKey, e.getMessage());
        }
    }

    /**
     * 提取 {@link AdvisedResponse} 中首条 Generation 的文本内容。
     */
    private String extractResponseContent(AdvisedResponse response) {
        if (response == null || response.response() == null) {
            return null;
        }
        List<Generation> generations = response.response().getResults();
        if (generations == null || generations.isEmpty()) {
            return null;
        }
        Generation gen = generations.get(0);
        if (gen == null || gen.getOutput() == null) {
            return null;
        }
        return gen.getOutput().getContent();
    }
}
