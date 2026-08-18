package com.seckill.mall.seckill.application;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.order.infrastructure.persistence.entity.OrderStatus;
import com.seckill.mall.seckill.api.SeckillOrderApi;
import com.seckill.mall.seckill.api.command.SeckillPayCommand;
import com.seckill.mall.seckill.api.command.SeckillShipCommand;
import com.seckill.mall.seckill.api.dto.SeckillOrderDTO;
import com.seckill.mall.seckill.api.dto.SeckillRankingDTO;
import com.seckill.mall.seckill.api.query.SeckillOrderQuery;
import com.seckill.mall.seckill.application.facade.SeckillApiConverter;
import com.seckill.mall.service.SeckillOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * SeckillOrder 应用服务（Strangler Pattern 门面）。
 *
 * <p>实现 {@link SeckillOrderApi}，内部委托给旧 {@link SeckillOrderService}，
 * 通过 {@link SeckillApiConverter} 做 VO/Entity ↔ DTO 转换。
 *
 * <p>本类不包含任何业务逻辑，仅做：
 * <ol>
 *     <li>从 Command/Query 提取业务参数</li>
 *     <li>委托旧 Service 执行业务</li>
 *     <li>将旧 Service 返回的 VO/Entity/裸值 转换为 API 层 DTO/裸值</li>
 * </ol>
 *
 * <p>委托映射参见 SECKILL-API-CONTRACT.md 第 9 节。
 *
 * @author wnj
 * @since Phase SK.4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillOrderApplicationService implements SeckillOrderApi {

    private final SeckillOrderService seckillOrderService;

    // === 用户端 ===

    @Override
    public PageResult<SeckillOrderDTO> listOrders(SeckillOrderQuery query) {
        return SeckillApiConverter.toOrderDTOPageResult(
                seckillOrderService.getOrderList(
                        query.getUserId(), query.getStatus(),
                        query.getPageNum(), query.getPageSize()));
    }

    @Override
    public SeckillOrderDTO getOrderDetail(Long userId, Long orderId) {
        return SeckillApiConverter.toDTO(
                seckillOrderService.getOrderDetail(userId, orderId));
    }

    @Override
    public SeckillOrderDTO payOrder(SeckillPayCommand command) {
        return SeckillApiConverter.toDTO(
                seckillOrderService.payOrder(
                        command.getUserId(), command.getOrderId(), command.getPayMethod()));
    }

    @Override
    public SeckillOrderDTO cancelOrder(Long userId, Long orderId) {
        return SeckillApiConverter.toDTO(
                seckillOrderService.cancelOrder(userId, orderId));
    }

    @Override
    public String getOrderStatus(Long userId, Long orderId) {
        return seckillOrderService.getOrderStatus(userId, orderId);
    }

    // === 管理端 ===

    @Override
    public boolean timeoutCancel(Long orderId) {
        return seckillOrderService.timeoutCancel(orderId);
    }

    @Override
    public void shipOrder(SeckillShipCommand command) {
        seckillOrderService.shipOrder(
                command.getUserId(), command.getOrderId(),
                command.getShippingCompany(), command.getShippingNo());
    }

    @Override
    public void confirmOrder(Long userId, Long orderId) {
        seckillOrderService.confirmOrder(userId, orderId);
    }

    // === 跨模块查询 ===

    @Override
    public boolean hasUserPurchasedSeckill(Long userId, Long productId) {
        return seckillOrderService.hasUserPurchasedSeckill(userId, productId);
    }

    @Override
    public List<SeckillOrderDTO> getWalletPaidOrdersByUser(Long userId) {
        return SeckillApiConverter.toOrderDTOListFromEntity(
                seckillOrderService.getWalletPaidOrdersByUser(userId));
    }

    @Override
    public List<SeckillOrderDTO> getSeckillOrdersForUnifiedList(Long userId, Integer status, int limit) {
        return SeckillApiConverter.toOrderDTOListFromEntity(
                seckillOrderService.getSeckillOrdersForUnifiedList(userId, status, limit));
    }

    @Override
    public boolean logicalDeleteSeckillOrder(Long orderId) {
        return seckillOrderService.logicalDeleteSeckillOrder(orderId);
    }

    // === 统计（stats 模块用）===

    @Override
    public long countAll() {
        return seckillOrderService.countAll();
    }

    @Override
    public BigDecimal sumSalesAmount(List<OrderStatus> statuses) {
        return seckillOrderService.sumSalesAmount(statuses);
    }

    @Override
    public Long countTodayOrders(LocalDate today) {
        return seckillOrderService.countTodayOrders(today);
    }

    @Override
    public List<Map<String, Object>> selectOrderTrend(LocalDate startDate, LocalDate endDate,
                                                       List<OrderStatus> statuses) {
        return seckillOrderService.selectOrderTrend(startDate, endDate, statuses);
    }

    @Override
    public List<SeckillRankingDTO> selectSeckillRanking(List<OrderStatus> statuses, int limit) {
        return SeckillApiConverter.toRankingDTOList(
                seckillOrderService.selectSeckillRanking(statuses, limit));
    }

    @Override
    public List<Map<String, Object>> selectStatusDistribution(LocalDateTime startTime, LocalDateTime endTime) {
        return seckillOrderService.selectStatusDistribution(startTime, endTime);
    }
}