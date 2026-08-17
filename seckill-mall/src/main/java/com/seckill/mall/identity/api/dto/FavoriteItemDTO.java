package com.seckill.mall.identity.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收藏项 DTO，收藏列表展示项（含商品快照信息）。
 *
 * <p>来源映射：UserFavorite Entity + ProductSnapshot → FavoriteItemDTO
 * 商品展示信息通过 {@code product.api.ProductApi.getProductById()} 获取。
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteItemDTO {

    /** 收藏记录 ID */
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 商品 ID */
    private Long productId;

    /** 商品名（来自 ProductSnapshot） */
    private String productName;

    /** 商品主图（来自 ProductSnapshot） */
    private String productMainImage;

    /** 商品价格（来自 ProductSnapshot） */
    private BigDecimal productPrice;

    /** 商品状态（来自 ProductSnapshot） */
    private String productStatus;

    /** 收藏时间 */
    private LocalDateTime createTime;
}