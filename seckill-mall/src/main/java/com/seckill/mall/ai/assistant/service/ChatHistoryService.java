package com.seckill.mall.ai.assistant.service;

import com.seckill.mall.cache.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * AI 导购助手对话历史服务（T13 实现）。
 * <p>基于 Redis 缓存多轮对话历史，key 格式 {@code seckill:ai:chat:{userId}:{conversationId}}，
 * TTL=1800 秒（30 分钟），过期自动清理避免占用内存。
 *
 * <h3>设计说明</h3>
 * <ul>
 *   <li>未登录用户（userId 为 null）或未传 conversationId 时，不读不写历史，等价于单轮对话。</li>
 *   <li>历史以纯文本拼接形式存储（user/assistant 角色标记），简单高效；
 *       如需结构化可后续升级为 JSON 数组存储 Message 列表。</li>
 *   <li>{@link #appendToken} 在流式响应每个 token 到达时追加，最终形成完整 assistant 回复。</li>
 *   <li>Redis 不可用时本服务不阻断主流程：读历史异常返回空串，写历史异常仅记日志。</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ChatHistoryService.java
 * 邮箱：nj651217@163.com
 */
@Service
public class ChatHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ChatHistoryService.class);

    /** Redis key 前缀 */
    private static final String KEY_PREFIX = "seckill:ai:chat:";
    /** TTL 30 分钟 */
    private static final long TTL_SECONDS = 1800L;

    /** 历史文本最大长度（超出截断旧内容，防止无限增长） */
    private static final int MAX_HISTORY_LENGTH = 8000;

    private final RedisService redisService;

    public ChatHistoryService(RedisService redisService) {
        this.redisService = redisService;
    }

    /**
     * 获取对话历史。
     * <p>userId 或 conversationId 为 null 时返回空串（无历史）。
     * <p>Redis 异常时返回空串，不阻断主流程。
     *
     * @param userId        用户 ID（未登录为 null）
     * @param conversationId 会话 ID（未传为 null）
     * @return 历史文本，无历史时返回空串
     */
    public String getHistory(Long userId, String conversationId) {
        if (userId == null || conversationId == null) {
            return "";
        }
        String key = buildKey(userId, conversationId);
        try {
            String history = redisService.get(key);
            return history == null ? "" : history;
        } catch (Exception e) {
            log.warn("读取对话历史失败，降级为空历史 userId={} convId={} err={}",
                    userId, conversationId, e.getMessage());
            return "";
        }
    }

    /**
     * 追加用户消息到历史。
     * <p>userId 或 conversationId 为 null 时不操作。
     *
     * @param userId        用户 ID
     * @param conversationId 会话 ID
     * @param userMessage   用户消息文本
     */
    public void appendUserMessage(Long userId, String conversationId, String userMessage) {
        append(userId, conversationId, "user: " + userMessage + "\n");
    }

    /**
     * 追加 assistant 回复 token 到历史（流式累积）。
     * <p>userId 或 conversationId 为 null 时不操作。
     * <p>注意：流式场景每个 token 都会触发一次 Redis 读改写，
     * 生产环境如性能敏感可改为本地缓冲 + onComplete 一次性写入。
     *
     * @param userId        用户 ID
     * @param conversationId 会话 ID
     * @param token         assistant 回复 token
     */
    public void appendToken(Long userId, String conversationId, String token) {
        // 流式 token 不带换行和前缀，由调用方在完成时统一追加 assistant: 前缀
        append(userId, conversationId, token);
    }

    /**
     * 标记一轮 assistant 回复结束（追加换行符分隔多轮）。
     *
     * @param userId        用户 ID
     * @param conversationId 会话 ID
     */
    public void finishAssistantTurn(Long userId, String conversationId) {
        append(userId, conversationId, "\nassistant: ");
    }

    /**
     * 在 assistant 回复开始前追加 "assistant: " 前缀。
     *
     * @param userId        用户 ID
     * @param conversationId 会话 ID
     */
    public void startAssistantTurn(Long userId, String conversationId) {
        append(userId, conversationId, "assistant: ");
    }

    /**
     * 内部追加方法：读出历史 + 追加内容 + 截断 + 写回（带 TTL）。
     * <p>Redis 异常时仅记日志，不抛异常。
     */
    private void append(Long userId, String conversationId, String content) {
        if (userId == null || conversationId == null || content == null || content.isEmpty()) {
            return;
        }
        String key = buildKey(userId, conversationId);
        try {
            String existing = redisService.get(key);
            String updated = (existing == null ? "" : existing) + content;
            // 截断旧内容防止无限增长：保留尾部 MAX_HISTORY_LENGTH 字符
            if (updated.length() > MAX_HISTORY_LENGTH) {
                updated = updated.substring(updated.length() - MAX_HISTORY_LENGTH);
            }
            redisService.set(key, updated, TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("追加对话历史失败，跳过本次写入 userId={} convId={} err={}",
                    userId, conversationId, e.getMessage());
        }
    }

    /**
     * 清除指定会话历史。
     *
     * @param userId        用户 ID
     * @param conversationId 会话 ID
     */
    public void clearHistory(Long userId, String conversationId) {
        if (userId == null || conversationId == null) {
            return;
        }
        String key = buildKey(userId, conversationId);
        try {
            redisService.del(key);
        } catch (Exception e) {
            log.warn("清除对话历史失败 userId={} convId={} err={}",
                    userId, conversationId, e.getMessage());
        }
    }

    private static String buildKey(Long userId, String conversationId) {
        return KEY_PREFIX + userId + ":" + conversationId;
    }
}