package com.seckill.mall.product.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新加购计数命令。
 *
 * <p>业务语义：递增/递减商品加购计数（cart_count = cart_count + delta，乐观更新避免并发覆盖）。
 *
 * <p>原方法：{@code ProductService.updateCartCount(Long productId, int delta)}
 *
 * @author wnj
 * @since Phase P.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCartCountCommand {

    /** 商品 ID（必填） */
    private Long productId;

    /** 变化量（+1 或 -1） */
    private int delta;
}