package com.seckill.mall.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 收藏项视图对象
 * <p>
 * 在收藏夹实体基础上冗余商品展示信息（名称、主图、单价、销量、收藏数、状态），
 * 便于前端收藏列表直接渲染商品卡片。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：FavoriteItemVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class FavoriteItemVO {

    /** 收藏项主键 ID */
    private Long id;

    /** 商品 ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 商品主图 URL */
    private String mainImage;

    /** 商品单价（原价） */
    private BigDecimal originalPrice;

    /** 商品累计销量 */
    private Integer salesCount;

    /** 商品收藏数量（冗余计数） */
    private Integer favoriteCount;

    /** 商品状态：ON_SALE-在售 / OFF_SHELF-下架 */
    private String productStatus;
}