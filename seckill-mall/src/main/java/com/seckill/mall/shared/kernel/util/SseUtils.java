package com.seckill.mall.shared.kernel.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicReference;

/**
 * SSE 流式响应工具类。
 * <p>
 * 统一 Flux&lt;String&gt; → SseEmitter 转换逻辑，消除 Controller 中重复的 SSE 代码。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SseUtils.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
public final class SseUtils {

    /** SSE 超时时间 60s */
    public static final long SSE_TIMEOUT = 60_000L;

    private SseUtils() {
    }

    /**
     * 将 Flux&lt;String&gt; 流式转换为 SseEmitter。
     * <p>
     * 每个 token 作为一个 data 事件发送；流完成时 complete emitter；
     * 流异常或客户端断开时 completeWithError 并清理订阅。
     *
     * @param flux     token 流
     * @param logTag   日志标识（如 "AI 导购" 或 "客服"）
     * @param logParams 日志参数（如 userId, conversationId）
     * @return SseEmitter
     */
    public static SseEmitter stream(Flux<String> flux, String logTag, Object... logParams) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        AtomicReference<Disposable> subscriptionRef = new AtomicReference<>();

        Disposable subscription = flux.doOnNext(token -> {
                    try {
                        emitter.send(SseEmitter.event().data(token));
                    } catch (Exception e) {
                        log.warn("{} SSE 发送 token 失败，客户端可能已断开 params={} err={}",
                                logTag, logParams, e.getMessage());
                        disposeQuietly(subscriptionRef);
                        completeWithErrorQuietly(emitter, e);
                    }
                })
                .doOnComplete(() -> completeQuietly(emitter))
                .doOnError(e -> {
                    log.error("{} SSE 流异常，关闭 emitter params={} err={}",
                            logTag, logParams, e.getMessage(), e);
                    disposeQuietly(subscriptionRef);
                    completeWithErrorQuietly(emitter, e);
                })
                .subscribe(null, e -> log.warn("{} SSE 订阅 onError params={} err={}",
                        logTag, logParams, e.getMessage()), () -> {});
        subscriptionRef.set(subscription);

        emitter.onTimeout(() -> {
            log.warn("{} SSE 超时 params={}", logTag, logParams);
            disposeQuietly(subscriptionRef);
        });
        emitter.onError(e -> {
            log.warn("{} SSE 客户端异常 params={} err={}", logTag, logParams, e.getMessage());
            disposeQuietly(subscriptionRef);
        });
        emitter.onCompletion(() -> disposeQuietly(subscriptionRef));

        return emitter;
    }

    private static void disposeQuietly(AtomicReference<Disposable> ref) {
        Disposable d = ref.getAndSet(null);
        if (d != null && !d.isDisposed()) {
            d.dispose();
        }
    }

    private static void completeQuietly(SseEmitter emitter) {
        try { emitter.complete(); } catch (Exception ignored) { }
    }

    private static void completeWithErrorQuietly(SseEmitter emitter, Throwable e) {
        try { emitter.completeWithError(e); } catch (Exception ignored) { }
    }
}