package com.seckill.mall.ai.customerservice.service;

import com.seckill.mall.ai.gateway.service.AiGatewayService;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.service.OrderService;
import com.seckill.mall.vo.OrderListItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 客服 Agent 编排服务（T18 实现）。
 * <p>统一编排 FAQ 检索、查单意图、LLM 兜底、降级转人工四条路径，
 * 按优先级短路返回，兼顾响应速度、成本与体验。
 *
 * <h3>编排策略（按优先级从高到低）</h3>
 * <ol>
 *   <li><b>FAQ 优先</b>：调 {@link FaqService#matchFaq(String)} 内存关键词匹配，
 *       命中直接 {@code Flux.just(answer)} 返回（最快且免费，不消耗 LLM token）</li>
 *   <li><b>查单意图</b>：消息含"订单"关键词时按登录状态分流：
 *       <ul>
 *         <li>已登录（userId != null）：调 {@link OrderService#getUnifiedOrderList}
 *             查最近 1 条订单，返回自然语言描述"您的最近订单：{orderNo} 状态：{status}"，
 *             未查到返回"您最近没有订单"</li>
 *         <li>未登录（userId == null）：返回"请先登录后查看订单信息"（防越权）</li>
 *       </ul>
 *       查单走本地 DB 不消耗 LLM token，响应快且数据准确</li>
 *   <li><b>LLM 兜底</b>：FAQ 与查单均未命中时，调
 *       {@link AiGatewayService#stream(String, String, String)} 流式生成回复，
 *       系统提示词约束大模型作为秒杀商城客服的职责边界</li>
 *   <li><b>降级转人工</b>：LLM 异常时 {@code onErrorResume} 返回
 *       "智能客服暂不可用，正在为您转接人工客服..."，保证用户体验</li>
 * </ol>
 *
 * <h3>设计权衡</h3>
 * <ul>
 *   <li>FAQ/查单用 {@code Flux.just} 返回，与 LLM 流式统一为 {@code Flux<String>}，
 *       Controller 层无需区分处理</li>
 *   <li>查单意图用关键词"订单"识别，P0 简单可靠；P1 可改为 LLM 意图分类或
 *       function-calling 让大模型主动调查单工具</li>
 *   <li>LLM 兜底已由 {@link AiGatewayService#stream} 内置 {@code onErrorResume} 降级，
 *       本方法再包一层兜底以防极端异常</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AgentService.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
public class AgentService {

    /** 调用方标识（用于 AI 网关审计/限流/降级路由） */
    private static final String CALLER = "customer-service";

    /** 查单意图关键词 */
    private static final String ORDER_INTENT_KEYWORD = "订单";

    /**
     * 系统提示词：引导大模型作为秒杀商城智能客服，在 FAQ 与查单之外的开放域问题兜底。
     * <p>关键约束：
     * <ul>
     *   <li>仅回答商城相关问题（商品/订单/支付/售后/活动等），拒绝无关问题</li>
     *   <li>无法确定的问题引导用户联系人工客服或描述具体订单</li>
     *   <li>不编造订单号、价格、库存等具体数据，引导用户在「我的订单」查看</li>
     *   <li>语气友好专业，回复简明扼要</li>
     * </ul>
     */
    private static final String SYSTEM_PROMPT = """
            你是秒杀商城的智能客服助手。你的职责是解答用户关于商城使用、商品、订单、支付、售后、活动等问题。

            【工作原则】
            1. 仅回答与商城相关的问题，对无关问题礼貌拒绝并引导用户咨询商城业务
            2. 回答简明扼要、语气友好专业，使用中文
            3. 不编造具体订单号、价格、库存等数据，涉及具体订单信息时引导用户在「我的订单」查看
            4. 无法确定的问题主动引导用户联系人工客服或提供更具体的信息

            【常见问题指引】
            - 退货/退款：收到商品7天内可申请，在「我的订单」操作
            - 收货地址：在「我的-收货地址」管理
            - 秒杀规则：限时限量，每人限购1件，未支付15分钟自动取消
            - 支付方式：支持微信/支付宝/余额支付
            - 会员等级：累计消费满1000元银卡、满5000元金卡
            """;

    /** 降级文案：LLM 不可用时引导转人工 */
    private static final String DEGRADE_MESSAGE =
            "智能客服暂不可用，正在为您转接人工客服，请稍候...";

    private final AiGatewayService aiGatewayService;
    private final FaqService faqService;
    private final OrderService orderService;

    public AgentService(AiGatewayService aiGatewayService,
                        FaqService faqService,
                        OrderService orderService) {
        this.aiGatewayService = aiGatewayService;
        this.faqService = faqService;
        this.orderService = orderService;
    }

    /**
     * 客服对话编排（流式）。
     * <p>按 FAQ → 查单 → LLM → 降级 优先级短路返回，
     * 统一为 {@code Flux<String>} 供 Controller SSE 输出。
     *
     * @param message 用户消息
     * @param userId  用户 ID（未登录为 null，查单需登录）
     * @return 流式响应 Flux<String>，每个元素为一个 token 或完整短回复
     */
    public Flux<String> chat(String message, Long userId) {
        if (message == null || message.isBlank()) {
            return Flux.just("请描述您的问题，我会为您解答。");
        }
        log.info("客服 Agent 编排开始 userId={} msgLen={}", userId, message.length());

        // 1. FAQ 优先：内存关键词匹配，命中直接返回（最快且免费）
        String faqAnswer = faqService.matchFaq(message);
        if (faqAnswer != null) {
            log.info("客服 FAQ 命中 userId={} msgLen={} ansLen={}",
                    userId, message.length(), faqAnswer.length());
            return Flux.just(faqAnswer);
        }

        // 2. 查单意图：消息含"订单"关键词
        if (message.contains(ORDER_INTENT_KEYWORD)) {
            // 2.1 未登录：提示登录（防越权）
            if (userId == null) {
                log.info("客服查单意图未登录，提示登录 msgLen={}", message.length());
                return Flux.just("请先登录后查看订单信息。");
            }
            // 2.2 已登录：查最近订单并返回自然语言描述
            String orderReply = queryRecentOrder(userId);
            log.info("客服查单意图命中 userId={} replyLen={}", userId, orderReply.length());
            return Flux.just(orderReply);
        }

        // 3. LLM 兜底：FAQ 与查单均未命中，调 AI 网关流式生成
        //    传入 userId 供 RateLimitAdvisor 按用户维度限流（避免退化为全局共享桶）
        log.info("客服走 LLM 兜底 userId={} msgLen={}", userId, message.length());
        return aiGatewayService.stream(SYSTEM_PROMPT, message, CALLER, userId)
                // 4. 降级：极端异常时转人工（AiGatewayService 已内置一层降级，此处再兜底）
                .onErrorResume(e -> {
                    log.error("客服 LLM 兜底异常，转人工 userId={} err={}",
                            userId, e.getMessage(), e);
                    return Flux.just(DEGRADE_MESSAGE);
                });
    }

    /**
     * 查询用户最近一条订单并返回自然语言描述。
     * <p>调 {@link OrderService#getUnifiedOrderList} 取第 1 页第 1 条（按 createTime 降序），
     * 查到返回"您的最近订单：{orderNo} 状态：{status}"，未查到返回"您最近没有订单"。
     * <p>异常时返回友好提示而非抛出，避免阻断客服对话。
     *
     * @param userId 用户 ID（非 null）
     * @return 自然语言订单描述
     */
    private String queryRecentOrder(Long userId) {
        try {
            PageResult<OrderListItemVO> page = orderService.getUnifiedOrderList(
                    userId, null, null, 1, 1);
            List<OrderListItemVO> list = page == null ? null : page.getList();
            if (list == null || list.isEmpty()) {
                return "您最近没有订单。";
            }
            OrderListItemVO order = list.get(0);
            String orderNo = order.getOrderNo();
            String status = order.getStatus();
            String orderType = order.getOrderType();
            // 状态中文映射，提升可读性
            String statusCn = mapStatusToChinese(status);
            String typeCn = "SECKILL".equals(orderType) ? "秒杀订单" : "普通订单";
            return String.format("您的最近订单：%s（%s），状态：%s。可在「我的订单」查看详情。",
                    orderNo, typeCn, statusCn);
        } catch (Exception e) {
            log.warn("客服查单异常 userId={} err={}", userId, e.getMessage());
            return "查询订单信息失败，请稍后重试或联系人工客服。";
        }
    }

    /**
     * 订单状态码映射为中文描述。
     * <p>状态码来自 {@link OrderListItemVO#getStatus()}，
     * 取值 UNPAID/PAID/SHIPPED/CANCELLED/TIMEOUT/COMPLETED。
     * 未知状态码原样返回，避免误导。
     */
    private String mapStatusToChinese(String status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case "UNPAID" -> "待支付";
            case "PAID" -> "已支付";
            case "SHIPPED" -> "已发货";
            case "CANCELLED" -> "已取消";
            case "TIMEOUT" -> "已超时取消";
            case "COMPLETED" -> "已完成";
            default -> status;
        };
    }
}