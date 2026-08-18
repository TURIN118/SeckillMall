package com.seckill.mall.cart.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 修改购物车项数量命令。
 *
 * <p>业务语义：修改指定购物车项的数量，校验购物车项归属当前用户。
 *
 * <p>原方法：{@code CartService.updateQuantity(Long userId, Long cartId, Integer quantity)}
 *
 * @author wnj
 * @since Phase C.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCartQuantityCommand {

    /** 用户 ID（必填） */
    private Long userId;

    /** 购物车项 ID（必填） */
    private Long cartId;

    /** 新数量（必填，必须大于 0） */
    private Integer quantity;
}