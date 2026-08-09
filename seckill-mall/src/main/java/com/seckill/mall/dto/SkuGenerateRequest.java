package com.seckill.mall.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * SKU 笛卡尔积生成请求
 * <p>
 * 前端"生成SKU组合"按钮提交的请求体。attributes 描述每个销售属性及其可选值，
 * 后端按笛卡尔积展开为 {@link ProductSkuDTO} 列表。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SkuGenerateRequest.java
 * 邮箱：nj651217@163.com
 */
@Data
public class SkuGenerateRequest {

    /** 商品 ID，编辑时携带，新增时为 null */
    private Long productId;

    /** 销售属性定义列表，至少一个属性 */
    @NotEmpty(message = "属性不能为空")
    @Valid
    private List<AttributeDefinition> attributes;

    /** 默认价格，生成的每个 SKU 初始价格 */
    @NotNull(message = "默认价格不能为空")
    private BigDecimal defaultPrice;

    /**
     * 单个销售属性定义，如：颜色 -> [黑, 白]
     */
    @Data
    public static class AttributeDefinition {

        /** 属性名，如 "颜色" */
        @NotEmpty(message = "属性名不能为空")
        private String name;

        /** 属性类型（前端扩展字段，后端生成 SKU 时不强制使用） */
        private String type;

        /** 该属性的可选值列表 */
        @NotEmpty(message = "属性值不能为空")
        @Valid
        private List<AttributeValue> values;
    }

    /**
     * 单个属性值，如：黑（可附带图片 URL，作为 SKU 主图候选）
     */
    @Data
    public static class AttributeValue {

        /** 属性值文本，如 "黑" */
        @NotEmpty(message = "属性值不能为空")
        private String value;

        /** 属性值对应的图片 URL，可空 */
        private String imageUrl;
    }
}