package com.seckill.mall.order.application.command;

import com.seckill.mall.order.api.OrderApi;
import com.seckill.mall.order.api.command.BuyNowCommand;
import com.seckill.mall.order.api.command.CancelOrderCommand;
import com.seckill.mall.order.api.command.ConfirmReceiptCommand;
import com.seckill.mall.order.api.command.CreateOrderCommand;
import com.seckill.mall.order.api.command.DeleteOrderCommand;
import com.seckill.mall.order.api.command.PayOrderCommand;
import com.seckill.mall.order.api.command.ShipCommand;
import com.seckill.mall.order.api.result.OrderCancelResult;
import com.seckill.mall.order.api.result.OrderCreateResult;
import com.seckill.mall.order.api.result.OrderPayResult;
import com.seckill.mall.order.application.facade.OrderApiConverter;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.service.OrderLifecycleService;
import com.seckill.mall.service.OrderQueryService;
import com.seckill.mall.service.OrderService;
import com.seckill.mall.vo.NormalOrderDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 订单写操作应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link OrderApi}，内部委托给旧 {@link OrderService}、
 * {@link OrderLifecycleService}、{@link OrderQueryService}，
 * 通过 {@link SecurityUtils} 获取当前用户 ID，
 * 通过 {@link OrderApiConverter} 做 VO→Result 转换。
 *
 * <p>本类不包含任何业务逻辑，仅做：
 * <ol>
 *     <li>从 SecurityUtils 获取 userId</li>
 *     <li>从 Command 提取业务参数</li>
 *     <li>委托旧 Service 执行业务</li>
 *     <li>将旧 Service 返回的 VO 转换为 API 层 Result</li>
 * </ol>
 *
 * @author wnj
 * @since Phase 3.4-A
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandApplicationService implements OrderApi {

    private final OrderService orderService;
    private final OrderLifecycleService orderLifecycleService;
    private final OrderQueryService orderQueryService;
    private final SecurityUtils securityUtils;

    @Override
    public OrderCreateResult buyNow(BuyNowCommand command) {
        Long userId = securityUtils.getCurrentUserId();
        NormalOrderDetailVO vo = orderService.createNormalOrder(
                userId,
                command.getProductId(),
                command.getSkuId(),
                command.getQuantity(),
                command.getAddressId(),
                command.getRemark(),
                command.getUserCouponId()
        );
        return OrderApiConverter.toCreateResult(vo);
    }

    @Override
    public OrderCreateResult createOrder(CreateOrderCommand command) {
        Long userId = securityUtils.getCurrentUserId();
        NormalOrderDetailVO vo = orderService.createOrderFromCart(
                userId,
                command.getAddressId(),
                command.getCartIds(),
                command.getRemark(),
                command.getUserCouponId()
        );
        return OrderApiConverter.toCreateResult(vo);
    }

    @Override
    public OrderPayResult payOrder(PayOrderCommand command) {
        Long userId = securityUtils.getCurrentUserId();
        NormalOrderDetailVO vo = orderLifecycleService.payNormalOrder(
                userId,
                command.getOrderId(),
                command.getPayMethod()
        );
        return OrderApiConverter.toPayResult(vo);
    }

    @Override
    public OrderCancelResult cancelOrder(CancelOrderCommand command) {
        Long userId = securityUtils.getCurrentUserId();
        NormalOrderDetailVO vo = orderLifecycleService.cancelNormalOrder(
                userId,
                command.getOrderId()
        );
        return OrderApiConverter.toCancelResult(vo);
    }

    @Override
    public void ship(ShipCommand command) {
        Long userId = securityUtils.getCurrentUserId();
        orderLifecycleService.shipNormalOrder(
                userId,
                command.getOrderId(),
                command.getShippingCompany(),
                command.getShippingNo()
        );
    }

    @Override
    public void confirmReceipt(ConfirmReceiptCommand command) {
        Long userId = securityUtils.getCurrentUserId();
        orderLifecycleService.confirmNormalOrder(userId, command.getOrderId());
    }

    @Override
    public boolean deleteOrder(DeleteOrderCommand command) {
        Long userId = securityUtils.getCurrentUserId();
        return orderQueryService.deleteOrder(command.getOrderId(), userId);
    }

    @Override
    public boolean timeoutCancel(Long orderId) {
        return orderLifecycleService.timeoutCancelNormalOrder(orderId);
    }
}