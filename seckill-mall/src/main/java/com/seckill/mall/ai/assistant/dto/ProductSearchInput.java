package com.seckill.mall.ai.assistant.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * AI 导购助手 function-calling 工具入参 DTO。
 * <p>Spring AI 1.0.0-M3 的 {@code FunctionCallbackWrapper} 仅支持单入参 {@code Function<I, O>}，
 * 故将 searchProducts 的多参数（keyword/categoryId/minPrice/maxPrice/pageNum/pageSize）
 * 包装为本 DTO，由 Spring AI 根据 Jackson 注解自动生成 JSON Schema 暴露给大模型。
 *
 * <h3>字段说明</h3>
 * <ul>
 *   <li>{@code keyword} —— 商品关键词（如"手机"），可空</li>
 *   <li>{@code categoryId} —— 分类 ID，可空表示不限分类</li>
 *   <li>{@code minPrice}/{@code maxPrice} —— 价格区间（元），可空表示不限</li>
 *   <li>{@code pageNum}/{@code pageSize} —— 分页参数，默认 1/10</li>
 * </ul>
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductSearchInput.java
 * 邮箱：nj651217@163.com
 */
public class ProductSearchInput {

    @JsonPropertyDescription("商品关键词，例如 '手机'、'运动鞋'，可为空")
    @JsonProperty("keyword")
    private String keyword;

    @JsonPropertyDescription("商品分类 ID，可为空表示不限分类")
    @JsonProperty("categoryId")
    private Long categoryId;

    @JsonPropertyDescription("价格下限（元），可为空表示不限下限")
    @JsonProperty("minPrice")
    private Double minPrice;

    @JsonPropertyDescription("价格上限（元），可为空表示不限上限")
    @JsonProperty("maxPrice")
    private Double maxPrice;

    @JsonPropertyDescription("页码，从 1 开始，默认 1")
    @JsonProperty("pageNum")
    private Integer pageNum;

    @JsonPropertyDescription("每页大小，默认 10，最大 50")
    @JsonProperty("pageSize")
    private Integer pageSize;

    public ProductSearchInput() {
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Double getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(Double minPrice) {
        this.minPrice = minPrice;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(Double maxPrice) {
        this.maxPrice = maxPrice;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}