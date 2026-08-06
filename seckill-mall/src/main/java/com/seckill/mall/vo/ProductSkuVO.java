package com.seckill.mall.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商品 SKU 视图对象（商品详情接口返回）
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：ProductSkuVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class ProductSkuVO {
    private Long id;
    private String skuCode;
    private BigDecimal price;
    private Integer stock;
    private String mainImage;
    /** SKU 属性键值对 JSON 字符串 */
    private String attributes;
    /** 状态：1-启用 / 0-禁用 */
    private Integer status;
}