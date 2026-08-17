package com.seckill.mall.identity.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 移除收藏命令。
 *
 * <p>业务语义：取消收藏（逻辑删除；同步递减 t_product.favorite_count）。
 *
 * <p>原方法：{@code UserFavoriteService.removeFavorite(Long, Long)}
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemoveFavoriteCommand {

    /** 用户 ID（必填） */
    private Long userId;

    /** 商品 ID（必填） */
    private Long productId;
}