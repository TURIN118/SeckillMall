package com.seckill.mall.cart.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 购物车项 DTO，替代 Cart Entity / CartItemVO 跨模块与跨层传递。
 *
 * <p>包含两类字段：
 * <ul>
 *     <li><b>核心字段</b>（id/userId/productId/skuId/quantity/selected）：跨模块只读传递所需，
 *         供 OrderServiceImpl 等模块使用。来源：Cart Entity。</li>
 *     <li><b>展示字段</b>（skuAttributes/productName/mainImage/skuMainImage/originalPrice/stock/
 *         productStatus/subtotal）：前端列表展示所需，供 Controller /list 端点使用。
 *         来源：CartItemVO（由旧 CartService 内部查询 ProductApi/SkuApi 填充）。</li>
 * </ul>
 *
 * <p>来源映射：
 * <ul>
 *     <li>Cart Entity → CartItemDTO（{@code CartApiConverter.toDTO}，仅核心字段，展示字段 null）</li>
 *     <li>CartItemVO → CartItemDTO（{@code CartApiConverter.toDTOFromVO}，全字段）</li>
 * </ul>
 *
 * <p>OrderServiceImpl 使用字段：userId、productId、skuId、quantity，全部覆盖（核心字段）。
 *
 * @author wnj
 * @since Phase C.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDTO {

    // ==================== 核心字段（跨模块只读传递） ====================

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

    // ==================== 展示字段（前端列表展示，来源 CartItemVO） ====================

    /** SKU 属性快照（如：曜石黑 / 旗舰版 / 氟橡胶表带） */
    private String skuAttributes;

    /** 商品名称 */
    private String productName;

    /** 商品主图 URL */
    private String mainImage;

    /** SKU 主图 URL（优先于 mainImage 展示） */
    private String skuMainImage;

    /** 商品单价（原价） */
    private BigDecimal originalPrice;

    /** 商品库存 */
    private Integer stock;

    /** 商品状态：ON_SALE-在售 / OFF_SHELF-下架 */
    private String productStatus;

    /** 小计金额 = originalPrice × quantity */
    private BigDecimal subtotal;
}
