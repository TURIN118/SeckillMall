package com.seckill.mall.ai.gateway.advisor;

import com.seckill.mall.ai.gateway.entity.AiAudit;
import com.seckill.mall.ai.gateway.entity.AiAuditMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SignalType;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

/**
 * AI 审计 Advisor（T6 实现）
 * <p>记录每次 LLM 调用的 caller/userId/model/promptHash/tokensIn/tokensOut/cost/elapsedMs/success，
 * 落库 {@code t_ai_audit}（append-only）供运营分析与成本治理。
 *
 * <h3>实现说明</h3>
 * <ul>
 *   <li>同步调用 {@link #aroundCall}：记录起始时间 → 调 chain → 从 {@link ChatResponse}
 *       元数据提取 model/tokens → 计算 cost → 构造 {@link AiAudit} 同步落库（try-catch 兜底）。</li>
 *   <li>流式调用 {@link #aroundStream}：调 chain 拿 Flux，用 {@code doFinally} 在流结束时
 *       记录审计（tokens/cost 留空，因流式元数据在末尾 chunk 提取复杂，P0 暂不解析）。</li>
 *   <li>落库异常不影响主流程：{@link #recordAudit} 内 try-catch，仅 warn 日志。</li>
 *   <li>cost 计算：P0 硬编码 DeepSeek 费率（输入 ¥0.001/1K，输出 ¥0.002/1K），后续可配置化。</li>
 * </ul>
 *
 * <h3>toolContext 约定</h3>
 * <ul>
 *   <li>{@code caller} —— 调用方标识（如 "chat"、"seckill-ai"），缺省 "default"</li>
 *   <li>{@code userId} —— 登录用户 ID（Long，可空）</li>
 *   <li>{@code model}  —— 模型名（可选，响应元数据优先）</li>
 * </ul>
 *
 * <p>getOrder()=30（在 RateLimitAdvisor=10、SemanticCacheAdvisor=20 之后执行）；
 * getName()="AuditAdvisor"。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AuditAdvisor.java
 * 邮箱：nj651217@163.com
 */
public class AuditAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private static final Logger log = LoggerFactory.getLogger(AuditAdvisor.class);

    /** toolContext 中调用方标识键 */
    private static final String CTX_CALLER = "caller";
    /** toolContext 中用户 ID 键 */
    private static final String CTX_USER_ID = "userId";
    /** toolContext 中模型名键 */
    private static final String CTX_MODEL = "model";

    private static final String DEFAULT_MODEL = "deepseek-chat";
    private static final String DEFAULT_CALLER = "default";

    /** DeepSeek 输入费率：¥0.001 / 1K tokens */
    private static final double IN_RATE = 0.001;
    /** DeepSeek 输出费率：¥0.002 / 1K tokens */
    private static final double OUT_RATE = 0.002;

    private final AiAuditMapper aiAuditMapper;

    public AuditAdvisor(AiAuditMapper aiAuditMapper) {
        this.aiAuditMapper = aiAuditMapper;
        log.info("AuditAdvisor 初始化：同步落库 t_ai_audit（append-only，异常兜底不影响主流程），" +
                "DeepSeek 费率 in={}¥/1K out={}¥/1K", IN_RATE, OUT_RATE);
    }

    @Override
    public String getName() {
        return "AuditAdvisor";
    }

    @Override
    public int getOrder() {
        // 在 RateLimitAdvisor(10)、SemanticCacheAdvisor(20) 之后执行
        return 30;
    }

    /**
     * 同步调用审计：记录起始时间 → 调 chain → 提取响应元数据 → 落库 → 返回响应。
     * <p>chain 抛异常时记录失败审计后重新抛出，保证异常不丢且审计完整。
     */
    @Override
    public AdvisedResponse aroundCall(AdvisedRequest request, CallAroundAdvisorChain chain) {
        long start = System.currentTimeMillis();
        Map<String, Object> toolContext = request.toolContext();
        String caller = readString(toolContext, CTX_CALLER, DEFAULT_CALLER);
        Long userId = readLong(toolContext, CTX_USER_ID);
        String model = readString(toolContext, CTX_MODEL, DEFAULT_MODEL);
        String promptHash = sha256(extractPrompt(request));

        AdvisedResponse response;
        try {
            response = chain.nextAroundCall(request);
        } catch (RuntimeException e) {
            // 调用失败：记录失败审计后重新抛出
            recordAudit(caller, userId, model, promptHash, 0, 0, BigDecimal.ZERO,
                    elapsedMs(start), 0);
            throw e;
        }

        // 调用成功：从响应元数据提取 model/tokens（覆盖 toolContext 默认值）
        int tokensIn = 0;
        int tokensOut = 0;
        if (response != null && response.response() != null) {
            ChatResponse chatResponse = response.response();
            ChatResponseMetadata metadata = chatResponse.getMetadata();
            if (metadata != null) {
                if (StringUtils.hasText(metadata.getModel())) {
                    model = metadata.getModel();
                }
                Usage usage = metadata.getUsage();
                if (usage != null) {
                    tokensIn = toInt(usage.getPromptTokens());
                    tokensOut = toInt(usage.getGenerationTokens());
                }
            }
        }
        BigDecimal cost = calculateCost(model, tokensIn, tokensOut);
        recordAudit(caller, userId, model, promptHash, tokensIn, tokensOut, cost,
                elapsedMs(start), 1);
        return response;
    }

    /**
     * 流式调用审计：调 chain 拿 Flux，用 {@code doFinally} 在流结束时落库。
     * <p>流式元数据在末尾 chunk 中，提取复杂，P0 暂不解析 tokens/cost（留 0），
     * 仅记录 caller/model/elapsedMs/success。后续可聚合 chunk 元数据完善。
     */
    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest request, StreamAroundAdvisorChain chain) {
        long start = System.currentTimeMillis();
        Map<String, Object> toolContext = request.toolContext();
        String caller = readString(toolContext, CTX_CALLER, DEFAULT_CALLER);
        Long userId = readLong(toolContext, CTX_USER_ID);
        String model = readString(toolContext, CTX_MODEL, DEFAULT_MODEL);
        String promptHash = sha256(extractPrompt(request));

        return chain.nextAroundStream(request)
                .doFinally(signal -> {
                    // ON_COMPLETE=成功，ON_ERROR/CANCEL=失败
                    int success = (signal == SignalType.ON_COMPLETE) ? 1 : 0;
                    recordAudit(caller, userId, model, promptHash, 0, 0, BigDecimal.ZERO,
                            elapsedMs(start), success);
                });
    }

    /**
     * 构造 {@link AiAudit} 并同步插入 t_ai_audit。
     * <p>异常仅 warn 日志，不抛出，保证审计故障不影响主流程。
     * createTime 由 {@link com.seckill.mall.config.MetaObjectHandler} 自动填充。
     */
    private void recordAudit(String caller, Long userId, String model, String promptHash,
                             int tokensIn, int tokensOut, BigDecimal cost,
                             int elapsedMs, int success) {
        try {
            AiAudit audit = new AiAudit();
            audit.setCaller(caller);
            audit.setUserId(userId);
            audit.setModel(model);
            audit.setPromptHash(promptHash);
            audit.setTokensIn(tokensIn);
            audit.setTokensOut(tokensOut);
            audit.setCost(cost);
            audit.setElapsedMs(elapsedMs);
            audit.setSuccess(success);
            audit.setEscalated(0);
            aiAuditMapper.insert(audit);
            if (log.isDebugEnabled()) {
                log.debug("AI 审计落库 caller={} model={} tokensIn={} tokensOut={} cost={} elapsedMs={} success={}",
                        caller, model, tokensIn, tokensOut, cost, elapsedMs, success);
            }
        } catch (Exception e) {
            log.warn("AI 审计落库失败，不影响主流程 caller={} model={} elapsedMs={} err={}",
                    caller, model, elapsedMs, e.getMessage());
        }
    }

    /**
     * 计算本次调用成本（¥）。
     * <p>P0 硬编码 DeepSeek 费率：输入 ¥0.001/1K tokens，输出 ¥0.002/1K tokens。
     * 后续可改为按模型查费率表（配置化）。
     */
    private BigDecimal calculateCost(String model, int tokensIn, int tokensOut) {
        double cost = (tokensIn * IN_RATE + tokensOut * OUT_RATE) / 1000.0;
        return BigDecimal.valueOf(cost);
    }

    /**
     * 提取 prompt 摘要：systemText + userText，用于计算 SHA-256 哈希（去重分析）。
     */
    private String extractPrompt(AdvisedRequest request) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(request.systemText())) {
            sb.append(request.systemText());
        }
        if (StringUtils.hasText(request.userText())) {
            sb.append(request.userText());
        }
        return sb.toString();
    }

    /**
     * 计算 prompt 的 SHA-256 哈希（十六进制，64 字符）。
     * <p>prompt 为空时返回 null；哈希失败时降级为 hashCode 十六进制。
     */
    private String sha256(String prompt) {
        if (!StringUtils.hasText(prompt)) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(prompt.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            log.warn("SHA-256 哈希失败，降级 hashCode err={}", e.getMessage());
            return Integer.toHexString(prompt.hashCode());
        }
    }

    /** 计算耗时（毫秒），防溢出。 */
    private static int elapsedMs(long start) {
        long elapsed = System.currentTimeMillis() - start;
        return elapsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) elapsed;
    }

    /** Long → int 安全转换，null 返回 0。 */
    private static int toInt(Long value) {
        if (value == null) {
            return 0;
        }
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : value.intValue();
    }

    /** 从 toolContext 读取字符串值，缺失或空白时返回默认值。 */
    private static String readString(Map<String, Object> ctx, String key, String defaultValue) {
        if (ctx == null) {
            return defaultValue;
        }
        Object value = ctx.get(key);
        if (value == null) {
            return defaultValue;
        }
        String s = String.valueOf(value).trim();
        return StringUtils.hasText(s) ? s : defaultValue;
    }

    /** 从 toolContext 读取 Long 值，缺失或解析失败时返回 null。 */
    private static Long readLong(Map<String, Object> ctx, String key) {
        if (ctx == null) {
            return null;
        }
        Object value = ctx.get(key);
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            String s = String.valueOf(value).trim();
            return StringUtils.hasText(s) ? Long.parseLong(s) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
