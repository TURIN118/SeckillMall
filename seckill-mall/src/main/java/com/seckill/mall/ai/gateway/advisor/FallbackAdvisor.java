package com.seckill.mall.ai.gateway.advisor;

import com.seckill.mall.cache.CacheDegradeService;
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

import java.util.List;
import java.util.Map;

/**
 * AI 降级兜底 Advisor（T7 实现）
 * <p>LLM 调用异常/超时时按 caller 返回兜底文案，保证用户体验：AI 不可用不等于业务中断。
 *
 * <h3>实现说明</h3>
 * <ul>
 *   <li>同步调用 {@link #aroundCall}：try-catch 包裹 chain，异常时构造降级 {@link AdvisedResponse} 返回。</li>
 *   <li>流式调用 {@link #aroundStream}：用 {@code onErrorResume} 包裹 chain，异常时返回降级 Flux。</li>
 *   <li>降级文案按 caller 区分：导购→关键词搜索提示、AIGC→手动编辑提示、客服→转人工、默认→稍后再试。</li>
 *   <li>同时暴露 {@link #fallback(String, Throwable)} 供 {@link com.seckill.mall.ai.gateway.service.AiGatewayService}
 *       在 Service 层 try-catch 后直接获取兜底文本（用于返回 String/Flux&lt;String&gt; 的方法签名）。</li>
 * </ul>
 *
 * <h3>toolContext 约定</h3>
 * <ul>
 *   <li>{@code caller} —— 调用方标识（shopping-assistant/aigc/customer-service），缺省 "default"</li>
 * </ul>
 *
 * <p>getOrder()=40（在 AuditAdvisor=30 之后执行，作为链路最末兜底）；
 * getName()="FallbackAdvisor"。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：FallbackAdvisor.java
 * 邮箱：nj651217@163.com
 */
public class FallbackAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private static final Logger log = LoggerFactory.getLogger(FallbackAdvisor.class);

    /** toolContext 中调用方标识键 */
    private static final String CTX_CALLER = "caller";
    private static final String DEFAULT_CALLER = "default";

    /** 各 caller 的降级文案 */
    private static final String FALLBACK_SHOPPING = "抱歉，AI 助手暂时开小差了，已为您切换到关键词搜索～";
    private static final String FALLBACK_AIGC = "AI 生成服务繁忙，请稍后重试或手动编辑文案";
    private static final String FALLBACK_CUSTOMER_SERVICE = "智能客服暂不可用，正在为您转接人工客服...";
    private static final String FALLBACK_DEFAULT = "AI 服务暂时不可用，请稍后再试";

    private final CacheDegradeService cacheDegradeService;

    public FallbackAdvisor() {
        this(null);
    }

    public FallbackAdvisor(CacheDegradeService cacheDegradeService) {
        this.cacheDegradeService = cacheDegradeService;
        log.info("FallbackAdvisor 初始化：异常兜底按 caller 返回降级文案，order=40");
    }

    @Override
    public String getName() {
        return "FallbackAdvisor";
    }

    @Override
    public int getOrder() {
        // 在 AuditAdvisor(30) 之后执行，作为链路最末兜底
        return 40;
    }

    /**
     * 同步调用降级兜底：try-catch 包裹 chain，异常时构造降级 {@link AdvisedResponse} 返回。
     */
    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        try {
            return chain.nextAroundCall(request);
        } catch (Exception e) {
            String caller = readCaller(request);
            log.warn("AI 调用异常，触发降级 caller={} errType={} msg={}",
                    caller, e.getClass().getSimpleName(), e.getMessage());
            return buildDegradedResponse(request, caller);
        }
    }

    /**
     * 流式调用降级兜底：用 {@code onErrorResume} 包裹 chain，异常时返回降级 Flux。
     */
    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest request, StreamAroundAdvisorChain chain) {
        return chain.nextAroundStream(request)
                .onErrorResume(e -> {
                    String caller = readCaller(request);
                    log.warn("AI 流式调用异常，触发降级 caller={} errType={} msg={}",
                            caller, e.getClass().getSimpleName(), e.getMessage());
                    return Flux.just(buildDegradedResponse(request, caller));
                });
    }

    /**
     * 供 {@link com.seckill.mall.ai.gateway.service.AiGatewayService} 调用的降级入口。
     * <p>Service 层方法签名为 {@code String}/{@code Flux<String>}，无法直接返回 {@link AdvisedResponse}，
     * 故暴露此方法返回兜底文本。
     *
     * @param caller 调用方标识
     * @param cause  异常原因（仅用于日志，P0 不区分异常类型）
     * @return 兜底文案
     */
    public String fallback(String caller, Throwable cause) {
        String c = StringUtils.hasText(caller) ? caller : DEFAULT_CALLER;
        log.warn("AI Service 触发降级 caller={} errType={} msg={}",
                c, cause == null ? "null" : cause.getClass().getSimpleName(),
                cause == null ? "null" : cause.getMessage());
        return fallbackText(c);
    }

    /**
     * 构造降级 {@link AdvisedResponse}：兜底文案 → AssistantMessage → Generation → ChatResponse → AdvisedResponse。
     * <p>参考 {@link SemanticCacheAdvisor#buildCachedResponse} 的构造方式。
     */
    private AdvisedResponse buildDegradedResponse(AdvisedRequest request, String caller) {
        String text = fallbackText(caller);
        AssistantMessage assistantMessage = new AssistantMessage(text);
        Generation generation = new Generation(assistantMessage);
        ChatResponse chatResponse = new ChatResponse(List.of(generation));
        Map<String, Object> adviseContext = request.adviseContext();
        return new AdvisedResponse(chatResponse, adviseContext);
    }

    /**
     * 按 caller 返回降级文案。
     */
    private String fallbackText(String caller) {
        if (caller == null) {
            return FALLBACK_DEFAULT;
        }
        return switch (caller) {
            case "shopping-assistant" -> FALLBACK_SHOPPING;
            case "aigc" -> FALLBACK_AIGC;
            case "customer-service" -> FALLBACK_CUSTOMER_SERVICE;
            default -> FALLBACK_DEFAULT;
        };
    }

    /** 从 request.toolContext() 读取 caller，缺失时返回默认值。 */
    private static String readCaller(AdvisedRequest request) {
        Map<String, Object> ctx = request.toolContext();
        if (ctx == null) {
            return DEFAULT_CALLER;
        }
        Object value = ctx.get(CTX_CALLER);
        if (value == null) {
            return DEFAULT_CALLER;
        }
        String s = String.valueOf(value).trim();
        return StringUtils.hasText(s) ? s : DEFAULT_CALLER;
    }
}
