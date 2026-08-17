package com.seckill.mall.order.application.query;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.order.api.OrderQueryApi;
import com.seckill.mall.order.api.dto.OrderDetailDTO;
import com.seckill.mall.order.api.dto.OrderListItemDTO;
import com.seckill.mall.order.api.dto.OrderStatusDistributionDTO;
import com.seckill.mall.order.api.dto.TrendDTO;
import com.seckill.mall.order.api.query.OrderListQuery;
import com.seckill.mall.order.api.result.OrderSnapshot;
import com.seckill.mall.order.application.facade.OrderApiConverter;
import com.seckill.mall.security.SecurityUtils;
import com.seckill.mall.service.OrderQueryService;
import com.seckill.mall.service.OrderService;
import com.seckill.mall.vo.NormalOrderDetailVO;
import com.seckill.mall.vo.OrderListItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 订单查询应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link OrderQueryApi}，内部委托给旧 {@link OrderQueryService}、
 * {@link OrderService}，通过 {@link OrderApiConverter} 做 VO→DTO 转换。
 *
 * <p>对于 {@link #getOrderDetail}、{@link #hasUserPurchased}、
 * {@link #getWalletPaidOrders} 方法，userId 作为方法入参直接传递（用于归属校验或跨模块调用）。
 * 对于 {@link #listOrders} 方法，userId 从 {@link SecurityUtils} 获取。
 *
 * <p>{@link #getStatusDistribution} 和 {@link #getOrderTrend} 为新增能力，
 * 旧 Service 无对应方法，暂抛 {@link UnsupportedOperationException}，
 * 待后续阶段从 StatsService 迁移或新建实现。
 *
 * @author wnj
 * @since Phase 3.4-A
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderQueryApplicationService implements OrderQueryApi {

    private final OrderQueryService orderQueryService;
    private final OrderService orderService;
    private final SecurityUtils securityUtils;

    @Override
    public OrderDetailDTO getOrderDetail(Long userId, Long orderId) {
        NormalOrderDetailVO vo = orderQueryService.getNormalOrderDetail(userId, orderId);
        return OrderApiConverter.toDetailDTO(vo);
    }

    @Override
    public PageResult<OrderListItemDTO> listOrders(OrderListQuery query) {
        Long userId = securityUtils.getCurrentUserId();
        PageResult<OrderListItemVO> voPage = orderQueryService.getUnifiedOrderList(
                userId,
                query.getStatus(),
                query.getOrderType(),
                query.getPageNum(),
                query.getPageSize()
        );
        if (voPage == null || voPage.getList() == null) {
            return new PageResult<>(Collections.emptyList(), 0, 1, 10, 0);
        }
        List<OrderListItemDTO> dtoList = voPage.getList().stream()
                .map(OrderApiConverter::toListItemDTO)
                .collect(Collectors.toList());
        return new PageResult<>(dtoList, voPage.getTotal(), voPage.getPageNum(),
                voPage.getPageSize(), voPage.getPages());
    }

    @Override
    public boolean hasUserPurchased(Long userId, Long productId, Long skuId) {
        return orderService.hasUserPurchasedProduct(userId, productId, skuId);
    }

    @Override
    public List<OrderSnapshot> getWalletPaidOrders(Long userId) {
        return OrderApiConverter.toSnapshotList(orderService.getWalletPaidOrdersByUser(userId));
    }

    @Override
    public List<OrderStatusDistributionDTO> getStatusDistribution() {
        throw new UnsupportedOperationException("getStatusDistribution 待后续阶段从 StatsService 迁移实现");
    }

    @Override
    public List<TrendDTO> getOrderTrend() {
        throw new UnsupportedOperationException("getOrderTrend 待后续阶段从 StatsService 迁移实现");
    }
}