package com.seckill.mall.identity.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 添加收藏命令。
 *
 * <p>业务语义：添加收藏（若已存在含逻辑删除的记录则恢复 is_deleted=0，否则新建；
 * 同步递增 t_product.favorite_count）。
 *
 * <p>原方法：{@code UserFavoriteService.addFavorite(Long, Long)}
 *
 * @author wnj
 * @since Phase I.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddFavoriteCommand {

    /** 用户 ID（必填） */
    private Long userId;

    /** 商品 ID（必填） */
    private Long productId;
}