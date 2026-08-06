package com.seckill.mall.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品 SKU DTO（创建 / 更新商品时携带）
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductSkuDTO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class ProductSkuDTO {

    private Long id;

    private String skuCode;

    @NotNull(message = "SKU 价格不能为空")
    @DecimalMin(value = "0.01", message = "SKU 价格必须大于 0")
    private BigDecimal price;

    @NotNull(message = "SKU 库存不能为空")
    @Min(value = 0, message = "SKU 库存不能小于 0")
    private Integer stock;

    /** SKU 主图 URL，可空表示沿用商品主图 */
    private String mainImage;

    /**
     * SKU 属性键值对 JSON 字符串
     * 如 {"颜色":"曜石黑","版本":"旗舰版","表带":"氟橡胶表带"}
     */
    @NotNull(message = "SKU 属性不能为空")
    private String attributes;

    /** 状态：1-启用 / 0-禁用，默认 1 */
    private Integer status = 1;
}