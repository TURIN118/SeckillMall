package com.seckill.mall.ai.assistant.controller;

import com.seckill.mall.ai.assistant.dto.ShoppingAssistantRequest;
import com.seckill.mall.ai.assistant.service.AssistantService;
import com.seckill.mall.annotation.RateLimit;
import com.seckill.mall.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AI 导购助手 SSE 接口（T12 实现）。
 * <p>用户自然语言描述需求 → AI 用 function-calling 调商品搜索工具 → 流式返回推荐。
 *
 * <h3>接口说明</h3>
 * <ul>
 *   <li>路径：{@code POST /api/v1/ai/shopping-assistant/chat}</li>
 *   <li>返回：{@link SseEmitter}（Server-Sent Events，60s 超时）</li>
 *   <li>限流：{@code @RateLimit(key="ai-shopping", capacity=20, rate=10, seconds=60)}，
 *       令牌桶容量 20、补充速率 10/s、时间窗口 60s</li>
 *   <li>鉴权：登录用户有多轮历史，未登录用户也可用（userId 为 null，无历史）；
 *       路径已在 {@link com.seckill.mall.config.SecurityConfig} 白名单中 permitAll</li>
 * </ul>
 *
 * <h3>SSE 事件格式</h3>
 * <pre>data:token1
 * data:token2
 * ...</pre>
 * <p>每个 token 作为一个 data 事件发送，前端按到达顺序拼接即得完整回复。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ShoppingAssistantController.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/shopping-assistant")
@RequiredArgsConstructor
public class ShoppingAssistantController {

    /** SSE 超时时间 60s */
    private static final long SSE_TIMEOUT = 60_000L;

    private final AssistantService assistantService;
    private final SecurityUtils securityUtils;

    /**
     * AI 导购对话（SSE 流式）。
     * <p>请求体：{@code {"message":"想买一部 2000 元左右的手机","conversationId":"uuid-xxx"}}
     * <p>响应：SSE 流，每个 token 作为一个 data 事件。
     *
     * @param req 导购请求
     * @return SseEmitter
     */
    @PostMapping("/chat")
    @RateLimit(key = "ai-shopping", capacity = 20, rate = 10, seconds = 60)
    public SseEmitter chat(@Valid @RequestBody ShoppingAssistantRequest req) {
        // 未登录用户也能用导购（userId 为 null），用 try-catch 兜底
        Long userId = getCurrentUserIdSafely();
        String conversationId = req.getConversationId();
        log.info("AI 导购 SSE 请求 userId={} convId={} msgLen={}",
                userId, conversationId, req.getMessage().length());

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        Flux<String> flux = assistantService.chat(req.getMessage(), userId, conversationId);

        java.util.concurrent.atomic.AtomicReference<reactor.core.Disposable> subscriptionRef =
                new java.util.concurrent.atomic.AtomicReference<>();

        // Flux → SseEmitter 转换
        Disposable subscription = flux.doOnNext(token -> {
                    try {
                        emitter.send(SseEmitter.event().data(token));
                    } catch (Exception e) {
                        log.warn("SSE 发送 token 失败，客户端可能已断开 userId={} convId={} err={}",
                                userId, conversationId, e.getMessage());
                        disposeQuietly(subscriptionRef);
                        completeWithErrorQuietly(emitter, e);
                    }
                })
                .doOnComplete(() -> completeQuietly(emitter))
                .doOnError(e -> {
                    log.error("SSE 流异常，关闭 emitter userId={} convId={} err={}",
                            userId, conversationId, e.getMessage(), e);
                    disposeQuietly(subscriptionRef);
                    completeWithErrorQuietly(emitter, e);
                })
                .subscribe(null, e -> log.warn("SSE 订阅 onError userId={} convId={} err={}",
                        userId, conversationId, e.getMessage()), () -> {});
        subscriptionRef.set(subscription);

        // 客户端断开时清理
        emitter.onTimeout(() -> {
            log.warn("SSE 超时 userId={} convId={}", userId, conversationId);
            disposeQuietly(subscriptionRef);
        });
        emitter.onError(e -> {
            log.warn("SSE 客户端异常 userId={} convId={} err={}",
                    userId, conversationId, e.getMessage());
            disposeQuietly(subscriptionRef);
        });
        emitter.onCompletion(() -> disposeQuietly(subscriptionRef));

        return emitter;
    }

    /**
     * 安全获取当前用户 ID。
     * <p>{@link SecurityUtils#getCurrentUserId()} 在未登录时抛 {@code BusinessException}，
     * 本方法用 try-catch 兜底返回 null，使未登录用户也能使用导购（无多轮历史）。
     *
     * @return 当前用户 ID，未登录返回 null
     */
    private Long getCurrentUserIdSafely() {
        try {
            return securityUtils.getCurrentUserId();
        } catch (Exception e) {
            log.debug("未登录用户使用 AI 导购，userId=null err={}", e.getMessage());
            return null;
        }
    }

    private void disposeQuietly(java.util.concurrent.atomic.AtomicReference<reactor.core.Disposable> ref) {
        reactor.core.Disposable d = ref.getAndSet(null);
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
    }

    private void completeQuietly(SseEmitter emitter) {
        try { emitter.complete(); } catch (Exception ignored) { }
    }

    private void completeWithErrorQuietly(SseEmitter emitter, Throwable e) {
        try { emitter.completeWithError(e); } catch (Exception ignored) { }
    }
}