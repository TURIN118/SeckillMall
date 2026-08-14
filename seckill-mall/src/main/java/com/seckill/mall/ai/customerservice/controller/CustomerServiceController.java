package com.seckill.mall.ai.customerservice.controller;

import com.seckill.mall.ai.customerservice.service.AgentService;
import com.seckill.mall.annotation.RateLimit;
import com.seckill.mall.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Map;

/**
 * AI 智能客服 SSE 接口（T18 实现）。
 * <p>全局浮窗入口，用户自然语言提问 → Agent 编排（FAQ/查单/转人工）→ 流式返回。
 *
 * <h3>接口说明</h3>
 * <ul>
 *   <li>路径：{@code POST /api/v1/ai/customer-service/chat}</li>
 *   <li>返回：{@link SseEmitter}（Server-Sent Events，60s 超时）</li>
 *   <li>限流：{@code @RateLimit(key="ai-cs", capacity=30, rate=15, seconds=60)}，
 *       令牌桶容量 30、补充速率 15/s、时间窗口 60s</li>
 *   <li>鉴权：{@code @PreAuthorize("hasAnyRole('BUYER','ADMIN')")} 需登录，
 *       查单需授权防越权（与 {@code OrderController}/{@code UserController} 同角色集）</li>
 * </ul>
 *
 * <h3>SSE 事件格式</h3>
 * <pre>data:token1
 * data:token2
 * ...</pre>
 * <p>每个 token 作为一个 data 事件发送，前端按到达顺序拼接即得完整回复。
 * FAQ/查单命中时为单事件完整短回复，LLM 兜底时为多事件流式 token。
 *
 * <h3>Agent 编排策略</h3>
 * <p>详见 {@link AgentService#chat(String, Long)}：FAQ 优先 → 查单意图 → LLM 兜底 → 降级转人工。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CustomerServiceController.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/ai/customer-service")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUYER','ADMIN')")
public class CustomerServiceController {

    /** SSE 超时时间 60s */
    private static final long SSE_TIMEOUT = 60_000L;

    private final AgentService agentService;
    private final SecurityUtils securityUtils;

    /**
     * AI 智能客服对话（SSE 流式）。
     * <p>请求体：{@code {"message":"如何退货"}}
     * <p>响应：SSE 流，每个 token 作为一个 data 事件。
     * <p>Agent 编排：FAQ 命中直接返回完整答案；查单意图查最近订单；
     * 否则走 LLM 流式兜底；异常降级转人工。
     *
     * @param req 请求体，含 message 字段
     * @return SseEmitter
     */
    @PostMapping("/chat")
    @RateLimit(key = "ai-cs", capacity = 30, rate = 15, seconds = 60)
    public SseEmitter chat(@RequestBody Map<String, String> req) {
        String message = req == null ? null : req.get("message");
        Long userId = securityUtils.getCurrentUserId();
        log.info("AI 客服 SSE 请求 userId={} msgLen={}",
                userId, message == null ? 0 : message.length());

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        Flux<String> flux = agentService.chat(message, userId);

        // Flux → SseEmitter 转换（参考 ShoppingAssistantController 模式）
        flux.doOnNext(token -> {
                    try {
                        emitter.send(SseEmitter.event().data(token));
                    } catch (IOException e) {
                        log.warn("客服 SSE 发送 token 失败，客户端可能已断开 userId={} err={}",
                                userId, e.getMessage());
                        emitter.completeWithError(e);
                    }
                })
                .doOnComplete(emitter::complete)
                .doOnError(e -> {
                    log.error("客服 SSE 流异常，关闭 emitter userId={} err={}",
                            userId, e.getMessage(), e);
                    emitter.completeWithError(e);
                })
                .subscribe();

        // 客户端断开时清理
        emitter.onTimeout(() -> log.warn("客服 SSE 超时 userId={}", userId));
        emitter.onError(e -> log.warn("客服 SSE 客户端异常 userId={} err={}",
                userId, e.getMessage()));

        return emitter;
    }
}