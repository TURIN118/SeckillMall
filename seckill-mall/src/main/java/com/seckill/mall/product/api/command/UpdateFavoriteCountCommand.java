package com.seckill.mall.product.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新收藏计数命令。
 *
 * <p>业务语义：递增/递减商品收藏计数（favorite_count = favorite_count + delta）。
 *
 * <p>原方法：{@code ProductService.updateFavoriteCount(Long productId, int delta)}
 *
 * @author wnj
 * @since Phase P.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFavoriteCountCommand {

    /** 商品 ID（必填） */
    private Long productId;

    /** 变化量（+1 或 -1） */
    private int delta;
}