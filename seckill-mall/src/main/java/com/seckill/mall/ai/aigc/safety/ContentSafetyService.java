package com.seckill.mall.ai.aigc.safety;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 内容安全过滤服务（T16，P0 简化版）。
 * <p>对 AIGC 生成的文案进行敏感词替换，防止违禁词落库。
 *
 * <h3>实现策略</h3>
 * <ul>
 *   <li>{@code @PostConstruct} 启动时从 classpath:{@code sensitive-words.txt} 加载敏感词表，
 *       每行一个词，存入 {@link Set}（HashSet，O(1) 查找）</li>
 *   <li>{@link #filter(String)} 遍历敏感词，将命中词替换为等长 {@code "*"} 号，
 *       不区分大小写（先转小写比较，原文按位置替换）</li>
 *   <li>加载失败时降级为空 Set（不阻断主流程，仅 {@code log.warn}），保证 AIGC 可用性优先</li>
 * </ul>
 *
 * <h3>P1 演进</h3>
 * <p>当前为本地敏感词表 P0 简化版，P1 接华为云 Moderation 服务做 AI 内容审核，
 * 覆盖政治/色情/暴力/广告法违规等多维度，本类的 {@link #filter(String)} 将改为调用 Moderation SDK。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ContentSafetyService.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
public class ContentSafetyService {

    /** 敏感词资源文件路径（classpath 下） */
    private static final String SENSITIVE_WORDS_RESOURCE = "sensitive-words.txt";

    /** 敏感词表，加载失败时为空 Set */
    private Set<String> sensitiveWords = Collections.emptySet();

    /**
     * 启动时加载敏感词表。
     * <p>读取 classpath:{@code sensitive-words.txt}，每行一个词，忽略空行与 # 开头注释行。
     * <p>加载失败降级为空 Set，不抛异常，保证 AIGC 主流程可用。
     */
    @PostConstruct
    public void init() {
        Set<String> loaded = new HashSet<>();
        ClassPathResource resource = new ClassPathResource(SENSITIVE_WORDS_RESOURCE);
        if (!resource.exists()) {
            log.warn("敏感词表资源不存在：{}，降级为空 Set，内容安全过滤将不生效", SENSITIVE_WORDS_RESOURCE);
            this.sensitiveWords = Collections.emptySet();
            return;
        }
        try (InputStream is = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                loaded.add(trimmed);
            }
            this.sensitiveWords = loaded;
            log.info("敏感词表加载完成，共 {} 个词", loaded.size());
        } catch (Exception e) {
            log.warn("敏感词表加载失败：{}，降级为空 Set，原因：{}", SENSITIVE_WORDS_RESOURCE, e.getMessage());
            this.sensitiveWords = Collections.emptySet();
        }
    }

    /**
     * 对内容进行敏感词过滤。
     * <p>遍历敏感词表，将命中的词替换为等长 {@code "*"} 号，不区分大小写。
     * <p>content 为 null 或空时原样返回。
     *
     * @param content 待过滤内容
     * @return 过滤后内容，敏感词已替换为 * 号
     */
    public String filter(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        if (sensitiveWords.isEmpty()) {
            return content;
        }
        String result = content;
        for (String word : sensitiveWords) {
            if (word.isEmpty()) {
                continue;
            }
            // 不区分大小写：用 (?i) 大小写无关匹配 + 等长 * 替换
            result = result.replaceAll("(?i)" + java.util.regex.Pattern.quote(word),
                    "*".repeat(word.length()));
        }
        return result;
    }

    /**
     * 获取已加载的敏感词数量（用于健康检查/调试）。
     *
     * @return 敏感词数量
     */
    public int getSensitiveWordCount() {
        return sensitiveWords.size();
    }
}