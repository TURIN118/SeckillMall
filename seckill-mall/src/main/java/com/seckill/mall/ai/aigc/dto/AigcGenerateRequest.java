package com.seckill.mall.ai.aigc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AIGC 文案生成请求 DTO（T15）。
 * <p>管理员在商品编辑页一键 AI 生成文案（标题/卖点/详情/SEO）时提交的入参。
 *
 * <h3>字段说明</h3>
 * <ul>
 *   <li>{@code productId} —— 商品 ID（编辑已有商品时传入，新建时可为 null）</li>
 *   <li>{@code categoryId} —— 分类 ID</li>
 *   <li>{@code categoryName} —— 分类名（用于 prompt 上下文，让 LLM 知道商品所属类目）</li>
 *   <li>{@code skuAttributes} —— SKU 属性 JSON，如 {@code {"颜色":"黑","版本":"8+128"}}，用于让 LLM 理解商品规格</li>
 *   <li>{@code price} —— 价格（可为 null，prompt 兜底为 0.0）</li>
 *   <li>{@code generateType} —— 生成类型，取值：TITLE/DESCRIPTION/DETAIL/SEO（必填）</li>
 * </ul>
 *
 * <h3>生成类型对应产物</h3>
 * <ul>
 *   <li>{@code TITLE} —— 商品标题，≤30 字，突出卖点</li>
 *   <li>{@code DESCRIPTION} —— 商品简介/卖点短文，≤100 字</li>
 *   <li>{@code DETAIL} —— 商品详情，HTML 格式（{@code <ul>} 列卖点 + {@code <table>} 列参数）</li>
 *   <li>{@code SEO} —— SEO 关键词和描述，JSON 格式 {@code {"keywords":"...","description":"..."}}</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：AigcGenerateRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class AigcGenerateRequest {

    /** 商品 ID（编辑时） */
    private Long productId;

    /** 分类 ID */
    private Long categoryId;

    /** 分类名 */
    private String categoryName;

    /** SKU 属性 JSON，如 {"颜色":"黑","版本":"8+128"} */
    private String skuAttributes;

    /** 价格（可为 null） */
    private Double price;

    /** 生成类型：TITLE/DESCRIPTION/DETAIL/SEO */
    @NotBlank(message = "生成类型不能为空")
    private String generateType;
}