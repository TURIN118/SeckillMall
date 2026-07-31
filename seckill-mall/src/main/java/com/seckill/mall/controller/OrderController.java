package com.seckill.mall.controller;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.common.Result;
import com.seckill.mall.entity.SeckillOrder;
import com.seckill.mall.entity.enums.OrderStatus;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderController.java
 * 邮箱：nj651217@163.com
 */
@Tag(name = "订单管理", description = "订单查询/支付/取消")
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BUYER')")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "我的订单列表（分页+状态筛选）")
    @GetMapping
    public Result<PageResult<SeckillOrder>> list(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "1") Integer pageNum,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(orderService.getOrderList(userId, status, pageNum, pageSize));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{orderId}")
    public Result<SeckillOrder> detail(@PathVariable Long orderId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(orderService.getOrderDetail(userId, orderId));
    }

    @Operation(summary = "确认支付（模拟支付）")
    @PostMapping("/{orderId}/pay")
    public Result<SeckillOrder> pay(@PathVariable Long orderId,
                                    @RequestParam(defaultValue = "ALIPAY") String payMethod) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(orderService.payOrder(userId, orderId, payMethod));
    }

    @Operation(summary = "取消订单（仅待支付）")
    @PostMapping("/{orderId}/cancel")
    public Result<SeckillOrder> cancel(@PathVariable Long orderId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(orderService.cancelOrder(userId, orderId));
    }

    @Operation(summary = "查询订单状态")
    @GetMapping("/{orderId}/status")
    public Result<OrderStatus> status(@PathVariable Long orderId) {
        Long userId = SecurityUtils.getCurrentUserId();
        return Result.success(orderService.getOrderStatus(userId, orderId));
    }
}
