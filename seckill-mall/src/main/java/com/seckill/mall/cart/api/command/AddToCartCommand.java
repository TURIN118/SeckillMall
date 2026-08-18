package com.seckill.mall.cart.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 添加商品到购物车命令。
 *
 * <p>业务语义：若已存在相同商品（同 productId + skuId）则数量累加，否则新建购物车项；
 * 同步递增 {@code t_product.cart_count}。
 *
 * <p>原方法：{@code CartService.addToCart(Long userId, Long productId, Long skuId, Integer quantity)}
 *
 * @author wnj
 * @since Phase C.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartCommand {

    /** 用户 ID（必填） */
    private Long userId;

    /** 商品 ID（必填） */
    private Long productId;

    /** SKU ID（可选，null 或 0 表示无规格商品） */
    private Long skuId;

    /** 加购数量（必填，必须大于 0） */
    private Integer quantity;
}