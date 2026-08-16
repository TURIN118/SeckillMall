package com.seckill.mall.controller;

import com.seckill.mall.annotation.OperationLog;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.common.Result;
import com.seckill.mall.dto.BuyNowRequest;
import com.seckill.mall.dto.CartCheckoutRequest;
import com.seckill.mall.dto.NormalOrderPayRequest;
import com.seckill.mall.dto.ShipRequest;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.service.OrderService;
import com.seckill.mall.service.SeckillOrderService;
import com.seckill.mall.vo.NormalOrderDetailVO;
import com.seckill.mall.vo.OrderListItemVO;
import com.seckill.mall.vo.SeckillOrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "订单管理", description = "订单查询/支付/取消 + 立即购买/购物车结算")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('BUYER', 'ADMIN', 'SELLER')")
public class OrderController {

    private final OrderService orderService;
    private final SeckillOrderService seckillOrderService;
    private final SecurityUtils securityUtils;

    // ==================== 秒杀订单（原有） ====================

    @Operation(summary = "我的订单列表（分页+状态筛选）")
    @GetMapping
    public Result<PageResult<SeckillOrderVO>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(seckillOrderService.getOrderList(userId, status, pageNum, pageSize));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{orderId}")
    public Result<SeckillOrderVO> detail(@PathVariable Long orderId) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(seckillOrderService.getOrderDetail(userId, orderId));
    }

    @Operation(summary = "确认支付（模拟支付）")
    @OperationLog(module = "ORDER", action = "PAY", targetIdSpEL = "#orderId", targetType = "ORDER")
    @PostMapping("/{orderId}/pay")
    public Result<SeckillOrderVO> pay(@PathVariable Long orderId,
                                      @RequestParam(defaultValue = "ALIPAY") String payMethod) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(seckillOrderService.payOrder(userId, orderId, payMethod));
    }

    @Operation(summary = "取消订单（仅待支付）")
    @OperationLog(module = "ORDER", action = "CANCEL", targetIdSpEL = "#orderId", targetType = "ORDER")
    @PostMapping("/{orderId}/cancel")
    public Result<SeckillOrderVO> cancel(@PathVariable Long orderId) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(seckillOrderService.cancelOrder(userId, orderId));
    }

    @Operation(summary = "查询订单状态")
    @GetMapping("/{orderId}/status")
    public Result<String> status(@PathVariable Long orderId) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(seckillOrderService.getOrderStatus(userId, orderId));
    }

    // ==================== 普通订单（需求5 立即购买 + 需求13 购物车结算） ====================

    @Operation(summary = "立即购买创建订单（需求5）")
    @OperationLog(module = "ORDER", action = "CREATE", targetType = "ORDER")
    @PostMapping
    public Result<NormalOrderDetailVO> createByBuyNow(@Valid @RequestBody BuyNowRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        NormalOrderDetailVO vo = orderService.createNormalOrder(
                userId, req.getProductId(), req.getSkuId(), req.getQuantity(),
                req.getAddressId(), req.getRemark(), req.getUserCouponId());
        return Result.success("下单成功", vo);
    }

    @Operation(summary = "购物车结算创建订单（需求13）")
    @OperationLog(module = "ORDER", action = "CREATE", targetType = "ORDER")
    @PostMapping("/from-cart")
    public Result<NormalOrderDetailVO> createFromCart(@Valid @RequestBody CartCheckoutRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        NormalOrderDetailVO vo = orderService.createOrderFromCart(
                userId, req.getAddressId(), req.getCartIds(), req.getRemark(), req.getUserCouponId());
        return Result.success("下单成功", vo);
    }

    @Operation(summary = "普通订单详情（含明细）")
    @GetMapping("/{orderId}/detail")
    public Result<NormalOrderDetailVO> normalDetail(@PathVariable Long orderId) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(orderService.getNormalOrderDetail(userId, orderId));
    }

    @Operation(summary = "普通订单支付（支持钱包/模拟支付）")
    @OperationLog(module = "ORDER", action = "PAY", targetIdSpEL = "#orderId", targetType = "ORDER")
    @PostMapping("/{orderId}/pay-normal")
    public Result<NormalOrderDetailVO> payNormal(@PathVariable Long orderId,
                                                 @Valid @RequestBody NormalOrderPayRequest req) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(orderService.payNormalOrder(userId, orderId, req.getPayMethod()));
    }

    @Operation(summary = "取消普通订单（仅待支付，BUG-002）")
    @OperationLog(module = "ORDER", action = "CANCEL", targetIdSpEL = "#orderId", targetType = "ORDER")
    @PostMapping("/{orderId}/cancel-normal")
    public Result<NormalOrderDetailVO> cancelNormal(@PathVariable Long orderId) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(orderService.cancelNormalOrder(userId, orderId));
    }

    // ==================== 统一订单列表（需求1 合并秒杀+普通） ====================

    @Operation(summary = "统一订单列表（秒杀+普通，支持orderType筛选）")
    @GetMapping("/unified")
    public Result<PageResult<OrderListItemVO>> unifiedList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String orderType,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(orderService.getUnifiedOrderList(userId, status, orderType, pageNum, pageSize));
    }

    @Operation(summary = "普通订单详情（normal-detail 别名，与 /detail 等价）")
    @GetMapping("/{orderId}/normal-detail")
    public Result<NormalOrderDetailVO> normalDetailAlias(@PathVariable Long orderId) {
        Long userId = securityUtils.getCurrentUserId();
        return Result.success(orderService.getNormalOrderDetail(userId, orderId));
    }

    // ==================== 发货与确认收货（Bug2修复） ====================

    @Operation(summary = "秒杀订单发货（管理员）")
    @OperationLog(module = "ORDER", action = "SHIP", targetIdSpEL = "#orderId", targetType = "ORDER")
    @PostMapping("/{orderId}/ship")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> shipOrder(@PathVariable Long orderId,
                                  @RequestBody @Valid ShipRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        seckillOrderService.shipOrder(userId, orderId, request.getShippingCompany(), request.getShippingNo());
        return Result.success("发货成功", null);
    }

    @Operation(summary = "秒杀订单确认收货（用户）")
    @OperationLog(module = "ORDER", action = "CONFIRM", targetIdSpEL = "#orderId", targetType = "ORDER")
    @PostMapping("/{orderId}/confirm")
    public Result<Void> confirmOrder(@PathVariable Long orderId) {
        Long userId = securityUtils.getCurrentUserId();
        seckillOrderService.confirmOrder(userId, orderId);
        return Result.success("确认收货成功", null);
    }

    @Operation(summary = "普通订单发货（管理员）")
    @OperationLog(module = "ORDER", action = "SHIP", targetIdSpEL = "#orderId", targetType = "ORDER")
    @PostMapping("/{orderId}/normal-ship")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> shipNormalOrder(@PathVariable Long orderId,
                                        @RequestBody @Valid ShipRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        orderService.shipNormalOrder(userId, orderId, request.getShippingCompany(), request.getShippingNo());
        return Result.success("发货成功", null);
    }

    @Operation(summary = "普通订单确认收货（用户）")
    @OperationLog(module = "ORDER", action = "CONFIRM", targetIdSpEL = "#orderId", targetType = "ORDER")
    @PostMapping("/{orderId}/normal-confirm")
    public Result<Void> confirmNormalOrder(@PathVariable Long orderId) {
        Long userId = securityUtils.getCurrentUserId();
        orderService.confirmNormalOrder(userId, orderId);
        return Result.success("确认收货成功", null);
    }

    // ==================== 订单逻辑删除（需求：订单逻辑删除+类型筛选） ====================

    @Operation(summary = "逻辑删除订单（仅已完成/已取消可删除）")
    @OperationLog(module = "ORDER", action = "DELETE", targetIdSpEL = "#orderId", targetType = "ORDER")
    @DeleteMapping("/{orderId}")
    public Result<Void> deleteOrder(@PathVariable Long orderId) {
        Long userId = securityUtils.getCurrentUserId();
        orderService.deleteOrder(orderId, userId);
        return Result.success("订单删除成功", null);
    }
}
