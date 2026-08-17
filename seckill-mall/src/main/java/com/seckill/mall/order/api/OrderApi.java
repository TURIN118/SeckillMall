package com.seckill.mall.order.api;

import com.seckill.mall.order.api.command.CancelOrderCommand;
import com.seckill.mall.order.api.command.ConfirmReceiptCommand;
import com.seckill.mall.order.api.command.CreateOrderCommand;
import com.seckill.mall.order.api.command.BuyNowCommand;
import com.seckill.mall.order.api.command.DeleteOrderCommand;
import com.seckill.mall.order.api.command.PayOrderCommand;
import com.seckill.mall.order.api.command.ShipCommand;
import com.seckill.mall.order.api.result.OrderCancelResult;
import com.seckill.mall.order.api.result.OrderCreateResult;
import com.seckill.mall.order.api.result.OrderPayResult;

/**
 * Order 模块订单业务能力 API（写操作）。
 *
 * <p>合并原 {@code OrderService}（写方法）+ {@code OrderLifecycleService}（状态流转）
 * + {@code OrderQueryService.deleteOrder}，对外暴露统一的订单业务能力契约。
 *
 * <p>设计原则：
 * <ul>
 *     <li>方法用业务语言命名，禁止 {@code save/update/delete} 等 CRUD 命名</li>
 *     <li>入参用 Command 对象，禁止裸露多参数</li>
 *     <li>出参用 Result/DTO，禁止暴露 Entity/Mapper/PO</li>
 *     <li>用户身份由 {@code CurrentUserContext} 注入，不作为 Command 字段</li>
 *     <li>异常通过 {@code BusinessException(ErrorCode)} 抛出，不泄露堆栈</li>
 * </ul>
 *
 * <p>向后兼容：契约一旦发布，方法签名只增不改不删，DTO 字段只增不删不改变类型。
 *
 * @author wnj
 * @since Phase 3.2
 */
public interface OrderApi {

    /**
     * 立即购买（商品详情页直接下单）。
     *
     * <p>业务流程：校验商品状态/库存 → 扣库存 → 建订单与明细 → 发延迟取消消息。
     *
     * @param command 立即购买命令
     * @return 创建订单结果
     * @throws com.seckill.mall.exception.BusinessException 异常错误码：
     *         {@code PRODUCT_NOT_FOUND}、{@code PRODUCT_OFF_SHELF}、{@code STOCK_NOT_ENOUGH}、
     *         {@code ADDRESS_NOT_FOUND}、{@code COUPON_INVALID}、{@code PARAM_ERROR}
     */
    OrderCreateResult buyNow(BuyNowCommand command);

    /**
     * 购物车结算创建订单。
     *
     * <p>业务流程：校验地址+购物车项 → 计算总额 → 建订单与明细 → 扣库存 → 删购物车项 → 发延迟消息。
     *
     * @param command 创建订单命令
     * @return 创建订单结果
     * @throws com.seckill.mall.exception.BusinessException 异常错误码：
     *         {@code ADDRESS_NOT_FOUND}、{@code CART_ITEM_NOT_FOUND}、{@code PRODUCT_OFF_SHELF}、
     *         {@code STOCK_NOT_ENOUGH}、{@code COUPON_INVALID}、{@code PARAM_ERROR}
     */
    OrderCreateResult createOrder(CreateOrderCommand command);

    /**
     * 支付订单。
     *
     * <p>业务流程：WALLET 方式扣钱包余额，其他方式模拟支付；UNPAID → PAID。
     *
     * @param command 支付订单命令
     * @return 支付订单结果
     * @throws com.seckill.mall.exception.BusinessException 异常错误码：
     *         {@code ORDER_NOT_FOUND}、{@code ORDER_STATUS_NOT_UNPAID}、
     *         {@code WALLET_BALANCE_NOT_ENOUGH}、{@code PAY_FAILED}
     */
    OrderPayResult payOrder(PayOrderCommand command);

    /**
     * 取消订单。
     *
     * <p>业务流程：仅 UNPAID 可取消；回补库存；退优惠券；异步发取消邮件。
     *
     * @param command 取消订单命令
     * @return 取消订单结果
     * @throws com.seckill.mall.exception.BusinessException 异常错误码：
     *         {@code ORDER_NOT_FOUND}、{@code ORDER_STATUS_NOT_UNPAID}、{@code CANCEL_FAILED}
     */
    OrderCancelResult cancelOrder(CancelOrderCommand command);

    /**
     * 发货（管理员操作）。
     *
     * <p>业务流程：PAID → SHIPPED，设置物流信息。
     *
     * @param command 发货命令
     * @throws com.seckill.mall.exception.BusinessException 异常错误码：
     *         {@code ORDER_NOT_FOUND}、{@code ORDER_STATUS_NOT_PAID}、{@code PARAM_ERROR}
     */
    void ship(ShipCommand command);

    /**
     * 确认收货。
     *
     * <p>业务流程：SHIPPED → COMPLETED。
     *
     * @param command 确认收货命令
     * @throws com.seckill.mall.exception.BusinessException 异常错误码：
     *         {@code ORDER_NOT_FOUND}、{@code ORDER_STATUS_NOT_SHIPPED}
     */
    void confirmReceipt(ConfirmReceiptCommand command);

    /**
     * 逻辑删除订单。
     *
     * <p>业务流程：仅 COMPLETED/CANCELLED 可删除，支持秒杀+普通两类。
     *
     * @param command 删除订单命令
     * @return {@code true} 删除成功
     * @throws com.seckill.mall.exception.BusinessException 异常错误码：
     *         {@code ORDER_NOT_FOUND}、{@code ORDER_DELETE_FAILED}
     */
    boolean deleteOrder(DeleteOrderCommand command);

    /**
     * 超时取消（内部 MQ 消费者调用）。
     *
     * <p>业务流程：UNPAID → TIMEOUT，回补库存；已支付等终态幂等忽略。
     * 异常内部捕获，返回 {@code false}。
     *
     * @param orderId 普通订单 ID
     * @return {@code true} 本次执行了取消；{@code false} 订单不存在或已终态
     */
    boolean timeoutCancel(Long orderId);
}