package com.seckill.mall.ai.assistant.service;

import com.seckill.mall.ai.gateway.service.AiGatewayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.function.FunctionCallback;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * AI 导购助手核心服务（T12 实现）。
 * <p>用户自然语言描述需求 → AI 用 function-calling 调 {@link ProductSearchTool} 搜索真实商品
 * → 流式返回基于真实数据的推荐文案。
 *
 * <h3>流程</h3>
 * <ol>
 *   <li>从 {@link ChatHistoryService} 获取多轮历史（未登录或无 conversationId 时为空）</li>
 *   <li>拼接历史 + 用户消息为完整 prompt</li>
 *   <li>调 {@link AiGatewayService#stream(String, String, String, FunctionCallback...)}
 *       带 {@link ProductSearchTool} 工具的流式调用</li>
 *   <li>{@code doOnNext} 追加每个 token 到对话历史（流式累积）</li>
 *   <li>{@code onErrorResume} 降级：返回关键词搜索提示文案</li>
 * </ol>
 *
 * <h3>系统提示词</h3>
 * <p>引导大模型：理解需求 → 调 searchProducts 工具搜索真实商品 → 自然语言总结推荐 → 支持多轮对话。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AssistantService.java
 * 邮箱：nj651217@163.com
 */
@Service
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    /** 调用方标识（用于审计/限流/降级路由） */
    private static final String CALLER = "shopping-assistant";

    /**
     * 系统提示词：引导大模型作为秒杀商城 AI 导购助手，调用商品搜索工具并基于真实数据推荐。
     * <p>关键约束：
     * <ul>
     *   <li>必须调用 searchProducts 工具获取真实商品，禁止编造商品名/价格</li>
     *   <li>基于工具返回的真实数据用自然语言总结推荐</li>
     *   <li>支持多轮对话，结合历史理解上下文</li>
     *   <li>推荐时突出商品卖点（销量/价格/库存），引导用户决策</li>
     * </ul>
     */
    private static final String SYSTEM_PROMPT = """
            你是秒杀商城的 AI 导购助手。你的职责是理解用户的购物需求，并推荐合适的商品。

            【工作流程】
            1. 理解用户需求（关键词、分类、价格区间、用途等）
            2. 调用 searchProducts 工具搜索真实在售商品
            3. 基于工具返回的真实商品数据，用自然语言总结推荐

            【严格约束】
            - 必须调用 searchProducts 工具获取真实商品，禁止凭空编造商品名称、价格、库存等信息
            - 推荐内容必须基于工具返回的真实数据，不得添加未返回的商品
            - 若工具返回空列表，告知用户暂无匹配商品并建议调整筛选条件

            【推荐风格】
            - 简明扼要，每轮推荐 3-5 件商品
            - 突出商品卖点（销量高/价格优惠/库存紧张等）
            - 主动询问是否需要进一步筛选或查看详情
            - 支持多轮对话，结合历史上下文理解用户意图

            【输出格式】
            - 用中文自然语言回复，不要使用 markdown 表格
            - 推荐商品时列出：商品名、价格、销量，并简述推荐理由
            """;

    /** 降级文案：AI 不可用时引导用户切换到关键词搜索 */
    private static final String DEGRADE_MESSAGE =
            "AI 助手暂不可用，已为您切换到关键词搜索。请尝试在搜索框输入关键词，或稍后重试。";

    private final AiGatewayService aiGatewayService;
    private final ProductSearchTool productSearchTool;
    private final ChatHistoryService chatHistoryService;

    public AssistantService(AiGatewayService aiGatewayService,
                            ProductSearchTool productSearchTool,
                            ChatHistoryService chatHistoryService) {
        this.aiGatewayService = aiGatewayService;
        this.productSearchTool = productSearchTool;
        this.chatHistoryService = chatHistoryService;
    }

    /**
     * AI 导购对话（流式）。
     * <p>用户消息 + 历史 → AI 网关（带商品搜索工具） → 流式 token → 追加历史 + 降级兜底。
     *
     * @param message        用户自然语言消息
     * @param userId         用户 ID（未登录为 null，无历史）
     * @param conversationId 会话 ID（未传为 null，无历史）
     * @return 流式响应 Flux<String>，每个元素为一个 token
     */
    public Flux<String> chat(String message, Long userId, String conversationId) {
        log.info("AI 导购对话开始 userId={} convId={} msgLen={}",
                userId, conversationId, message == null ? 0 : message.length());

        // 1. 获取历史并追加本轮用户消息
        String history = chatHistoryService.getHistory(userId, conversationId);
        chatHistoryService.appendUserMessage(userId, conversationId, message);
        // 标记 assistant 回复开始
        chatHistoryService.startAssistantTurn(userId, conversationId);

        // 2. 拼接完整 prompt：历史 + 用户消息
        String fullPrompt = buildFullPrompt(history, message);

        // 3. 构建工具回调
        FunctionCallback toolCallback = productSearchTool.buildFunctionCallback();

        // 4. 调 AI 网关流式接口（带工具），追加历史 + 降级
        return aiGatewayService.stream(SYSTEM_PROMPT, fullPrompt, CALLER, toolCallback)
                .doOnNext(token -> {
                    // 流式追加 token 到历史
                    chatHistoryService.appendToken(userId, conversationId, token);
                })
                .doOnComplete(() -> {
                    // 一轮 assistant 回复结束，追加换行分隔下一轮
                    chatHistoryService.finishAssistantTurn(userId, conversationId);
                    log.info("AI 导购对话完成 userId={} convId={}", userId, conversationId);
                })
                .doOnError(e -> log.error("AI 导购对话异常 userId={} convId={} err={}",
                        userId, conversationId, e.getMessage(), e))
                .onErrorResume(e -> Flux.just(DEGRADE_MESSAGE));
    }

    /**
     * 拼接完整 prompt：历史 + 用户消息。
     * <p>无历史时直接返回用户消息。
     */
    private String buildFullPrompt(String history, String message) {
        if (history == null || history.isBlank()) {
            return message;
        }
        return history + "user: " + message;
    }
}