package com.seckill.mall.cart.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 删除购物车项命令。
 *
 * <p>业务语义：逻辑删除指定购物车项，校验归属当前用户；
 * 同步递减 {@code t_product.cart_count}。
 *
 * <p>原方法：{@code CartService.removeFromCart(Long userId, Long cartId)}
 *
 * @author wnj
 * @since Phase C.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveCartItemCommand {

    /** 用户 ID（必填） */
    private Long userId;

    /** 购物车项 ID（必填） */
    private Long cartId;
}