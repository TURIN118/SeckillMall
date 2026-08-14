package com.seckill.mall.ai.customerservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * FAQ 检索服务（T18 实现）。
 * <p>P0 阶段采用内存关键词匹配，从 classpath {@code faq.json} 加载 FAQ 列表，
 * 在 {@code @PostConstruct} 阶段一次性读入内存，运行时 O(n) 遍历匹配。
 *
 * <h3>匹配策略</h3>
 * <ol>
 *   <li>遍历 FAQ 列表，统计每条 FAQ 命中关键词数量</li>
 *   <li>优先级：全部关键词命中 &gt; 部分关键词命中 &gt; 未命中</li>
 *   <li>同优先级取首条（声明顺序），未匹配返回 null（交由 LLM 兜底）</li>
 * </ol>
 *
 * <h3>降级策略</h3>
 * <p>{@code faq.json} 加载失败（文件缺失/格式错误）时降级为空列表，
 * 仅打印 warn 日志不阻断启动，所有问题将走 LLM 兜底。
 *
 * <h3>后续演进</h3>
 * <p>P1 可替换为向量检索（Embedding + 向量库），提升语义匹配能力；
 * FAQ 数据可迁移到 t_faq 表或 Redis Hash 支持运营动态维护。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：FaqService.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
public class FaqService {

    /** FAQ 配置文件路径（classpath） */
    private static final String FAQ_LOCATION = "faq.json";

    /** 内存 FAQ 列表（加载后不可变，运行时只读遍历） */
    private volatile List<FaqItem> faqItems = new ArrayList<>();

    private final ObjectMapper objectMapper;

    public FaqService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 启动时加载 faq.json 到内存。
     * <p>加载失败降级为空列表（log.warn，不阻断启动）。
     */
    @PostConstruct
    public void loadFaq() {
        try (InputStream is = new ClassPathResource(FAQ_LOCATION).getInputStream()) {
            List<FaqItem> loaded = objectMapper.readValue(is, new TypeReference<List<FaqItem>>() {});
            if (loaded == null) {
                loaded = new ArrayList<>();
            }
            this.faqItems = loaded;
            log.info("FAQ 加载成功，共 {} 条", faqItems.size());
        } catch (Exception e) {
            // 降级：空列表，不阻断启动，所有问题走 LLM 兜底
            log.warn("FAQ 加载失败，降级为空列表，所有问题将走 LLM 兜底。location={} err={}",
                    FAQ_LOCATION, e.getMessage());
            this.faqItems = new ArrayList<>();
        }
    }

    /**
     * 关键词匹配 FAQ。
     * <p>遍历 FAQ 列表，检查 question 是否包含任一 keyword，
     * 返回最佳匹配的 answer 或 null（未匹配交由 LLM 兜底）。
     *
     * <h3>匹配优先级</h3>
     * <ol>
     *   <li>关键词全部命中的 FAQ（命中数 == keywords.length）</li>
     *   <li>部分关键词命中的 FAQ（命中数 &gt; 0）</li>
     *   <li>未命中返回 null</li>
     * </ol>
     * <p>同优先级取首条（声明顺序）。
     *
     * @param question 用户问题
     * @return 最佳匹配的 answer，未匹配返回 null
     */
    public String matchFaq(String question) {
        if (question == null || question.isBlank() || faqItems.isEmpty()) {
            return null;
        }

        String bestAnswer = null;
        int bestHitCount = 0;
        boolean bestFullHit = false;

        for (FaqItem item : faqItems) {
            String[] keywords = item.getKeywords();
            if (keywords == null || keywords.length == 0) {
                continue;
            }

            int hitCount = 0;
            for (String kw : keywords) {
                if (kw != null && !kw.isEmpty() && question.contains(kw)) {
                    hitCount++;
                }
            }

            if (hitCount == 0) {
                continue;
            }

            boolean fullHit = (hitCount == keywords.length);
            // 优先级：全命中 > 部分命中；同优先级取首条（不替换）
            if (fullHit && !bestFullHit) {
                // 全命中优先于部分命中，直接替换
                bestAnswer = item.getAnswer();
                bestHitCount = hitCount;
                bestFullHit = true;
            } else if (!bestFullHit && hitCount > bestHitCount) {
                // 同为部分命中，取命中数更多者
                bestAnswer = item.getAnswer();
                bestHitCount = hitCount;
            }
            // 全命中已找到则不再处理部分命中
            if (bestFullHit) {
                break;
            }
        }

        return bestAnswer;
    }

    /**
     * 返回当前内存 FAQ 条数（供监控/调试）。
     */
    public int size() {
        return faqItems.size();
    }

    /**
     * FAQ 条目内部模型。
     * <p>对应 {@code faq.json} 数组元素：question（问题）、answer（答案）、keywords（关键词数组）。
     */
    @Data
    public static class FaqItem {
        /** 问题文本（仅用于人工核对，不参与匹配） */
        private String question;
        /** 答案文本（命中后直接返回） */
        private String answer;
        /** 关键词数组（任一命中即视为匹配） */
        private String[] keywords;
    }
}