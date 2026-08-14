package com.seckill.mall.ai.aigc.service;

import com.seckill.mall.ai.aigc.dto.AigcGenerateRequest;
import com.seckill.mall.ai.aigc.safety.ContentSafetyService;
import com.seckill.mall.ai.gateway.service.AiGatewayService;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * AIGC 文案生成服务（T15）。
 * <p>管理员在商品编辑页一键 AI 生成文案（标题/卖点/详情/SEO）的核心业务逻辑。
 *
 * <h3>处理流程</h3>
 * <ol>
 *   <li>按 {@code generateType} 选择对应 prompt 模板（TITLE/DESCRIPTION/DETAIL/SEO）</li>
 *   <li>调 {@link AiGatewayService#call(String, String, String)} 同步调用大模型，
 *       caller 固定为 {@code "aigc"}，用于网关审计/限流/预算/兜底</li>
 *   <li>调 {@link ContentSafetyService#filter(String)} 对生成结果做敏感词过滤</li>
 *   <li>返回过滤后的文案（前端预览，人工确认后才落库）</li>
 * </ol>
 *
 * <h3>降级策略</h3>
 * <ul>
 *   <li>LLM 调用异常：由 {@link AiGatewayService} 内部 {@code FallbackAdvisor} 兜底返回降级文案</li>
 *   <li>敏感词表加载失败：{@link ContentSafetyService} 降级为空 Set，过滤不生效但不阻断</li>
 *   <li>未知 generateType：抛 {@link BusinessException}（PARAM_ERROR）</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AigcService.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AigcService {

    /** AI 网关调用方标识，用于审计/限流/预算/兜底 */
    private static final String CALLER = "aigc";

    private final AiGatewayService aiGatewayService;
    private final PromptTemplates promptTemplates;
    private final ContentSafetyService contentSafetyService;

    /**
     * 生成 AIGC 文案。
     * <p>根据 {@link AigcGenerateRequest#getGenerateType()} 选择 prompt 模板，
     * 调 AI 网关同步生成，再经内容安全过滤后返回。
     *
     * @param req 生成请求
     * @return 过滤后的文案（标题/简介/HTML 详情/SEO JSON）
     * @throws BusinessException 当 generateType 不是 TITLE/DESCRIPTION/DETAIL/SEO 时
     */
    public String generate(AigcGenerateRequest req) {
        String generateType = req.getGenerateType();
        String categoryName = req.getCategoryName();
        String skuAttributes = req.getSkuAttributes();
        Double price = req.getPrice();

        // 1. 按 generateType 选择 prompt 模板
        String userPrompt = switch (generateType) {
            case "TITLE" -> promptTemplates.buildTitle(categoryName, skuAttributes, price);
            case "DESCRIPTION" -> promptTemplates.buildDescription(categoryName, skuAttributes, price);
            case "DETAIL" -> promptTemplates.buildDetail(categoryName, skuAttributes, price);
            case "SEO" -> promptTemplates.buildSeo(categoryName, skuAttributes, price);
            default -> throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "不支持的生成类型：" + generateType + "，仅支持 TITLE/DESCRIPTION/DETAIL/SEO");
        };

        log.info("AIGC 文案生成请求 type={} productId={} categoryId={}",
                generateType, req.getProductId(), req.getCategoryId());

        // 2. 调 AI 网关同步生成
        String rawResult = aiGatewayService.call(PromptTemplates.SYSTEM_PROMPT, userPrompt, CALLER);

        // 3. 内容安全过滤
        String filtered = contentSafetyService.filter(rawResult);

        if (!rawResult.equals(filtered)) {
            log.warn("AIGC 文案命中敏感词已过滤 type={} productId={} rawLen={} filteredLen={}",
                    generateType, req.getProductId(), rawResult.length(), filtered.length());
        }

        return filtered;
    }
}