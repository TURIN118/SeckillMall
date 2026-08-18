package com.seckill.mall.cart.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 购物车项 DTO，替代 Cart Entity 跨模块传递。
 *
 * <p>跨模块只读传递时使用，避免暴露 {@code Cart} Entity。
 * 裁剪掉 {@code isDeleted/createTime/updateTime} 等基础设施字段，
 * 仅保留跨模块传递所需的核心字段。
 *
 * <p>来源映射：
 * <ul>
 *     <li>Cart Entity → CartItemDTO（{@code CartApiConverter.toDTO}）</li>
 *     <li>CartItemVO → CartItemDTO（{@code CartApiConverter.toDTOFromVO}）</li>
 * </ul>
 *
 * <p>OrderServiceImpl 使用字段：userId、productId、skuId、quantity，全部覆盖。
 *
 * @author wnj
 * @since Phase C.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {

    /** 购物车项主键 ID */
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 商品 ID */
    private Long productId;

    /** SKU ID，0 表示无规格商品（NOT NULL DEFAULT 0） */
    private Long skuId;

    /** 加购数量 */
    private Integer quantity;

    /** 是否选中（Entity 层 0/1 → DTO 层 true/false） */
    private Boolean selected;
}