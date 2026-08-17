package com.seckill.mall.order.api;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.order.api.dto.AdminOrderDTO;
import com.seckill.mall.order.api.dto.OrderDetailDTO;
import com.seckill.mall.order.api.dto.OrderListItemDTO;
import com.seckill.mall.order.api.dto.OrderStatusDistributionDTO;
import com.seckill.mall.order.api.dto.TrendDTO;
import com.seckill.mall.order.api.query.OrderListQuery;
import com.seckill.mall.order.api.result.OrderSnapshot;

import java.util.List;

/**
 * Order 模块订单查询能力 API（读操作）。
 *
 * <p>对外暴露订单查询契约，包括订单详情、统一订单列表、用户购买校验、
 * 钱包支付订单查询、状态分布统计、订单趋势统计。
 *
 * <p>设计原则：
 * <ul>
 *     <li>入参用 Query 对象或裸参数（仅查询条件，非用户身份）</li>
 *     <li>出参用 DTO/Result，禁止暴露 Entity/Mapper/PO</li>
 *     <li>用户身份参数保留为方法入参（用于归属校验或跨模块调用）</li>
 * </ul>
 *
 * @author wnj
 * @since Phase 3.2
 */
public interface OrderQueryApi {

    /**
     * 查询订单详情。
     *
     * <p>查询普通订单详情（含明细列表+收货地址信息；自动处理超时状态）。
     *
     * @param userId 用户 ID（用于归属校验）
     * @param orderId 普通订单 ID
     * @return 订单详情 DTO
     * @throws com.seckill.mall.exception.BusinessException 异常错误码：
     *         {@code ORDER_NOT_FOUND}、{@code ORDER_NOT_BELONG_TO_USER}
     */
    OrderDetailDTO getOrderDetail(Long userId, Long orderId);

    /**
     * 统一订单列表查询。
     *
     * <p>统一查询秒杀订单+普通订单，合并后按 createTime 降序，内存分页。
     *
     * @param query 订单列表查询条件
     * @return 分页结果
     */
    PageResult<OrderListItemDTO> listOrders(OrderListQuery query);

    /**
     * 查询用户是否购买指定商品（评价权限校验用）。
     *
     * @param userId 用户 ID
     * @param productId 商品 ID
     * @param skuId SKU ID（0 表示不校验 SKU 维度）
     * @return {@code true} 用户已通过普通订单购买该商品
     */
    boolean hasUserPurchased(Long userId, Long productId, Long skuId);

    /**
     * 查询用户钱包支付的已支付普通订单。
     *
     * <p>查询条件：payMethod=WALLET 且 status in PAID/SHIPPED/COMPLETED。
     * 用于 payment 模块（WalletService）展示钱包支付流水。
     *
     * @param userId 用户 ID
     * @return 订单快照列表（替代 NormalOrder Entity，避免 Entity 泄露）
     */
    List<OrderSnapshot> getWalletPaidOrders(Long userId);

    /**
     * 查询普通订单各状态数量分布。
     *
     * <p>用于统计概览与系统监控。
     *
     * @return 状态分布列表
     */
    List<OrderStatusDistributionDTO> getStatusDistribution();

    /**
     * 查询普通订单近 N 天下单趋势。
     *
     * <p>用于统计概览展示。
     *
     * @return 趋势列表
     */
    List<TrendDTO> getOrderTrend();
}