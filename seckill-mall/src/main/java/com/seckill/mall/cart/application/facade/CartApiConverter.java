package com.seckill.mall.cart.application.facade;

import com.seckill.mall.cart.api.dto.CartItemDTO;
import com.seckill.mall.cart.infrastructure.entity.Cart;
import com.seckill.mall.cart.interfaces.vo.CartItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Cart API 转换辅助类。
 *
 * <p>集中存放旧 VO/Entity 与新 API 层 DTO 之间的转换方法，
 * 供 CartApplicationService 调用。所有方法均为无状态静态方法，
 * 标注 {@code @Component} 仅为便于未来扩展为 Bean 注入方式。
 *
 * <p>转换原则：
 * <ul>
 *     <li>Entity → DTO：仅提取核心字段，展示字段设为 null（Entity 不含商品展示信息）</li>
 *     <li>VO → DTO：全字段映射（核心 + 展示），保留前端展示信息</li>
 *     <li>DTO → VO：全字段映射（核心 + 展示），保留前端契约</li>
 * </ul>
 *
 * <p>selected 字段转换：Entity 层 Integer(0/1) ↔ DTO 层 Boolean(true/false) ↔ VO 层 Boolean(true/false)。
 *
 * @author wnj
 * @since Phase C.4-A
 */
@Slf4j
@Component
public class CartApiConverter {

    /** 选中标识：1=选中 */
    private static final int SELECTED_FLAG = 1;

    // ============================================================
    // Cart Entity → CartItemDTO 转换（跨模块只读快照，仅核心字段）
    // ============================================================

    /**
     * 将 {@link Cart} Entity 转换为 {@link CartItemDTO}。
     *
     * <p>仅提取核心字段，展示字段设为 null（Entity 不含商品展示信息）。
     * selected 字段：Integer(0/1) → Boolean(true/false)。
     *
     * @param entity 购物车项 Entity
     * @return 购物车项 DTO
     */
    public static CartItemDTO toDTO(Cart entity) {
        if (entity == null) {
            return null;
        }
        return CartItemDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .productId(entity.getProductId())
                .skuId(entity.getSkuId())
                .quantity(entity.getQuantity())
                .selected(entity.getSelected() != null && entity.getSelected() == SELECTED_FLAG)
                .build();
    }

    /**
     * 将 {@link Cart} Entity 列表转换为 {@link CartItemDTO} 列表。
     *
     * @param entities 购物车项 Entity 列表
     * @return 购物车项 DTO 列表
     */
    public static List<CartItemDTO> toDTOList(List<Cart> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        return entities.stream()
                .map(CartApiConverter::toDTO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // CartItemVO → CartItemDTO 转换（全字段，保留展示信息）
    // ============================================================

    /**
     * 将 {@link CartItemVO} 转换为 {@link CartItemDTO}。
     *
     * <p>全字段映射（核心 + 展示），保留前端展示信息。
     * 注意：CartItemVO 不含 userId 字段，转换后 userId 为 null。
     *
     * @param vo 购物车项 VO
     * @return 购物车项 DTO
     */
    public static CartItemDTO toDTOFromVO(CartItemVO vo) {
        if (vo == null) {
            return null;
        }
        return CartItemDTO.builder()
                .id(vo.getId())
                .userId(null)
                .productId(vo.getProductId())
                .skuId(vo.getSkuId())
                .quantity(vo.getQuantity())
                .selected(vo.getSelected())
                .skuAttributes(vo.getSkuAttributes())
                .productName(vo.getProductName())
                .mainImage(vo.getMainImage())
                .skuMainImage(vo.getSkuMainImage())
                .originalPrice(vo.getOriginalPrice())
                .stock(vo.getStock())
                .productStatus(vo.getProductStatus())
                .subtotal(vo.getSubtotal())
                .build();
    }

    /**
     * 将 {@link CartItemVO} 列表转换为 {@link CartItemDTO} 列表。
     *
     * @param voList 购物车项 VO 列表
     * @return 购物车项 DTO 列表
     */
    public static List<CartItemDTO> toDTOListFromVO(List<CartItemVO> voList) {
        if (voList == null) {
            return Collections.emptyList();
        }
        return voList.stream()
                .map(CartApiConverter::toDTOFromVO)
                .collect(Collectors.toList());
    }

    // ============================================================
    // CartItemDTO → CartItemVO 转换（Controller 层前端契约适配，全字段）
    // ============================================================

    /**
     * 将 {@link CartItemDTO} 转换为 {@link CartItemVO}。
     *
     * <p>全字段映射（核心 + 展示），保留前端契约。
     * 供 Controller /list 端点调用 CartApi 后转回 VO 返回前端。
     *
     * @param dto 购物车项 DTO
     * @return 购物车项 VO
     */
    public static CartItemVO toVO(CartItemDTO dto) {
        if (dto == null) {
            return null;
        }
        CartItemVO vo = new CartItemVO();
        vo.setId(dto.getId());
        vo.setProductId(dto.getProductId());
        vo.setSkuId(dto.getSkuId());
        vo.setQuantity(dto.getQuantity());
        vo.setSelected(dto.getSelected());
        vo.setSkuAttributes(dto.getSkuAttributes());
        vo.setProductName(dto.getProductName());
        vo.setMainImage(dto.getMainImage());
        vo.setSkuMainImage(dto.getSkuMainImage());
        vo.setOriginalPrice(dto.getOriginalPrice());
        vo.setStock(dto.getStock());
        vo.setProductStatus(dto.getProductStatus());
        vo.setSubtotal(dto.getSubtotal());
        return vo;
    }

    /**
     * 将 {@link CartItemDTO} 列表转换为 {@link CartItemVO} 列表。
     *
     * @param dtoList 购物车项 DTO 列表
     * @return 购物车项 VO 列表
     */
    public static List<CartItemVO> toVOList(List<CartItemDTO> dtoList) {
        if (dtoList == null) {
            return Collections.emptyList();
        }
        return dtoList.stream()
                .map(CartApiConverter::toVO)
                .collect(Collectors.toList());
    }
}
