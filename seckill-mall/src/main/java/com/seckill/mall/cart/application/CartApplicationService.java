package com.seckill.mall.cart.application;

import com.seckill.mall.cart.api.CartApi;
import com.seckill.mall.cart.api.command.AddToCartCommand;
import com.seckill.mall.cart.api.command.BatchUpdateSelectedCommand;
import com.seckill.mall.cart.api.command.RemoveCartItemCommand;
import com.seckill.mall.cart.api.command.UpdateCartQuantityCommand;
import com.seckill.mall.cart.api.command.UpdateSelectedCommand;
import com.seckill.mall.cart.api.dto.CartItemDTO;
import com.seckill.mall.cart.application.facade.CartApiConverter;
import com.seckill.mall.cart.interfaces.vo.CartItemVO;
import com.seckill.mall.common.Result;
import com.seckill.mall.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Cart 应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link CartApi}，内部委托给旧 {@link CartService}，
 * 通过 {@link CartApiConverter} 做 VO/Entity ↔ DTO 转换。
 *
 * <p>本类不包含任何业务逻辑，仅做：
 * <ol>
 *     <li>从 Command/Query 提取业务参数</li>
 *     <li>委托旧 Service 执行业务</li>
 *     <li>将旧 Service 返回的 VO/Entity 转换为 API 层 DTO</li>
 * </ol>
 *
 * <p>说明：旧 {@link CartService} 返回 {@code Result<T>} 包装类型，
 * 本类解包后取 {@code data} 字段返回。旧 Service 异常时 {@code Result.code != 200}，
 * 过渡期保留此行为，后续 Phase 再细化异常映射。
 *
 * @author wnj
 * @since Phase C.4-A
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartApplicationService implements CartApi {

    private final CartService cartService;

    @Override
    public List<CartItemDTO> getCartList(Long userId) {
        Result<List<CartItemVO>> result = cartService.getCartList(userId);
        if (result == null || result.getData() == null) {
            return List.of();
        }
        return CartApiConverter.toDTOListFromVO(result.getData());
    }

    @Override
    public void addToCart(AddToCartCommand command) {
        cartService.addToCart(command.getUserId(), command.getProductId(),
                command.getSkuId(), command.getQuantity());
    }

    @Override
    public void updateQuantity(UpdateCartQuantityCommand command) {
        cartService.updateQuantity(command.getUserId(), command.getCartId(),
                command.getQuantity());
    }

    @Override
    public void removeFromCart(RemoveCartItemCommand command) {
        cartService.removeFromCart(command.getUserId(), command.getCartId());
    }

    @Override
    public void clearCart(Long userId) {
        cartService.clearCart(userId);
    }

    @Override
    public void updateSelected(UpdateSelectedCommand command) {
        cartService.updateSelected(command.getUserId(), command.getCartId(),
                command.getSelected());
    }

    @Override
    public void batchUpdateSelected(BatchUpdateSelectedCommand command) {
        cartService.batchUpdateSelected(command.getUserId(), command.getCartIds(),
                command.getSelected());
    }

    @Override
    public int getCartCount(Long userId) {
        Result<Integer> result = cartService.getCartCount(userId);
        return result != null && result.getData() != null ? result.getData() : 0;
    }

    @Override
    public List<CartItemDTO> getCartItemsByIds(List<Long> cartIds) {
        return CartApiConverter.toDTOList(cartService.getCartsByIds(cartIds));
    }

    @Override
    public void deleteCartItemsByIds(List<Long> cartIds) {
        cartService.deleteCartsByIds(cartIds);
    }
}