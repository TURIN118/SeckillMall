package com.seckill.mall.cart.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 批量更新购物车项选中状态命令。
 *
 * <p>业务语义：批量更新多个购物车项的选中状态，校验归属当前用户。
 *
 * <p>原方法：{@code CartService.batchUpdateSelected(Long userId, List<Long> cartIds, Boolean selected)}
 *
 * @author wnj
 * @since Phase C.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUpdateSelectedCommand {

    /** 用户 ID（必填） */
    private Long userId;

    /** 购物车项 ID 列表（必填） */
    private List<Long> cartIds;

    /** 是否选中 */
    private Boolean selected;
}