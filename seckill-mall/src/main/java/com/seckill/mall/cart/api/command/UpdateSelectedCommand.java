package com.seckill.mall.cart.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新购物车项选中状态命令。
 *
 * <p>业务语义：更新单个购物车项的选中状态，校验归属当前用户。
 *
 * <p>原方法：{@code CartService.updateSelected(Long userId, Long cartId, Boolean selected)}
 *
 * @author wnj
 * @since Phase C.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSelectedCommand {

    /** 用户 ID（必填） */
    private Long userId;

    /** 购物车项 ID（必填） */
    private Long cartId;

    /** 是否选中 */
    private Boolean selected;
}