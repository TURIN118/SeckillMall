package com.seckill.mall.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 购物车项视图对象
 * <p>
 * 在购物车实体基础上冗余商品展示信息（名称、主图、单价、库存、状态），
 * subtotal 为小计金额 = originalPrice × quantity，由 Service 层计算填充。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CartItemVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class CartItemVO {

    /** 购物车项主键 ID */
    private Long id;

    /** 商品 ID */
    private Long productId;

    /** 加购数量 */
    private Integer quantity;

    /** 是否选中：true-选中 / false-未选中（实体层仍以 0/1 存储，由 Service 层转换） */
    private Boolean selected;

    /** 商品名称 */
    private String productName;

    /** 商品主图 URL */
    private String mainImage;

    /** 商品单价（原价） */
    private BigDecimal originalPrice;

    /** 商品库存 */
    private Integer stock;

    /** 商品状态：ON_SALE-在售 / OFF_SHELF-下架 */
    private String productStatus;

    /** 小计金额 = originalPrice × quantity */
    private BigDecimal subtotal;
}