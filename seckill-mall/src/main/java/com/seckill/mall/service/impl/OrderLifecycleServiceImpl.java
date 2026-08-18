package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.order.infrastructure.persistence.entity.NormalOrder;
import com.seckill.mall.order.infrastructure.persistence.entity.NormalOrderItem;
import com.seckill.mall.product.api.InventoryApi;
import com.seckill.mall.product.api.SkuApi;
import com.seckill.mall.product.api.command.RestoreStockCommand;
import com.seckill.mall.identity.infrastructure.entity.UserAddress;
import com.seckill.mall.order.infrastructure.persistence.entity.OrderStatus;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.order.infrastructure.persistence.mapper.NormalOrderItemMapper;
import com.seckill.mall.order.infrastructure.persistence.mapper.NormalOrderMapper;
import com.seckill.mall.coupon.api.CouponUsageApi;
import com.seckill.mall.service.EmailService;
import com.seckill.mall.service.OrderLifecycleService;
import com.seckill.mall.service.OrderQueryService;
import com.seckill.mall.service.PaymentService;
import com.seckill.mall.service.UserAddressService;
import com.seckill.mall.identity.api.UserApi;
import com.seckill.mall.vo.NormalOrderDetailVO;
import com.seckill.mall.vo.NormalOrderItemVO;
import com.seckill.mall.vo.NormalOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 订单生命周期领域服务实现（Phase P1-1 从 OrderServiceImpl 沿状态流转职责缝拆分而来）。
 * <p>
 * 仅承载普通订单状态流转用例（支付 / 取消 / 超时取消 / 发货 / 确认收货）。
 * 业务逻辑与原 OrderServiceImpl 完全一致，仅做机械移动。
 * <p>
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderLifecycleServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderLifecycleServiceImpl implements OrderLifecycleService {

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter PAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String CANCEL_REASON_USER = "用户主动取消";
    private static final String CANCEL_REASON_TIMEOUT = "超时未支付，自动取消";

    private final UserApi userApi;
    private final EmailService emailService;
    private final NormalOrderMapper normalOrderMapper;
    private final NormalOrderItemMapper normalOrderItemMapper;
    private final CouponUsageApi couponUsageApi;
    private final PaymentService paymentService;
    private final InventoryApi inventoryApi;
    private final SkuApi skuApi;
    private final UserAddressService userAddressService;
    // Phase P1-1：支付成功后回查订单详情，委托至 OrderQueryService
    private final OrderQueryService orderQueryService;

    /**
     * Bug1修复：普通订单超时取消（延迟消费者触发）。
     * 5.7.3 状态机：UNPAID → CANCELLING → 回补库存 → TIMEOUT，已支付等终态幂等忽略。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean timeoutCancelNormalOrder(Long orderId) {
        NormalOrder order = normalOrderMapper.selectById(orderId);
        if (order == null) {
            return false;
        }
        if (order.getStatus() != OrderStatus.UNPAID) {
            return false;
        }
        // 5.7.3 状态机：乐观锁 UNPAID → CANCELLING，保证只有一个请求能进入回补流程
        int rows = normalOrderMapper.update(null, new LambdaUpdateWrapper<NormalOrder>()
                .eq(NormalOrder::getId, orderId)
                .eq(NormalOrder::getStatus, OrderStatus.UNPAID)
                .set(NormalOrder::getStatus, OrderStatus.CANCELLING));
        if (rows == 0) {
            // 已被其他请求（如用户主动取消）抢占，直接返回
            log.info("普通订单超时取消被并发抢占，幂等返回，orderNo={}", order.getOrderNo());
            return false;
        }
        // 查询订单明细，用于回补库存
        List<NormalOrderItem> items = normalOrderItemMapper.selectList(
                new LambdaQueryWrapper<NormalOrderItem>()
                        .eq(NormalOrderItem::getOrderId, orderId));
        // 5.7.3：回补库存（SKU 库存或商品库存）
        rollbackStockByItems(items);
        // 更新订单状态为 TIMEOUT：CANCELLING → TIMEOUT
        normalOrderMapper.update(null, new LambdaUpdateWrapper<NormalOrder>()
                .eq(NormalOrder::getId, orderId)
                .eq(NormalOrder::getStatus, OrderStatus.CANCELLING)
                .set(NormalOrder::getStatus, OrderStatus.TIMEOUT)
                .set(NormalOrder::getCancelTime, LocalDateTime.now())
                .set(NormalOrder::getCancelReason, CANCEL_REASON_TIMEOUT));

        // 优惠券回退：超时取消时，若订单使用了优惠券则回退（USED → UNUSED，幂等）
        if (order.getUserCouponId() != null) {
            try {
                couponUsageApi.revertCoupon(com.seckill.mall.coupon.api.command.RevertCouponCommand.builder()
                        .userCouponId(order.getUserCouponId())
                        .userId(order.getUserId())
                        .build());
            } catch (Exception e) {
                log.error("超时取消订单后优惠券回退失败，orderId={}, userCouponId={}", orderId, order.getUserCouponId(), e);
            }
        }

        log.info("普通订单超时取消成功，orderNo={}, orderId={}", order.getOrderNo(), orderId);

        // 超时取消邮件：异步发送，失败不影响主流程
        String email = userApi.getUserEmail(order.getUserId());
        if (email != null) {
            try {
                emailService.sendOrderCancel(email, order.getOrderNo(), CANCEL_REASON_TIMEOUT);
            } catch (Exception e) {
                log.error("普通订单超时取消邮件发送失败，orderId={}", orderId, e);
            }
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NormalOrderDetailVO payNormalOrder(Long userId, Long orderId, String payMethod) {
        NormalOrder order = loadAndCheckNormalOrderOwnership(userId, orderId);
        OrderStatus current = order.getStatus();
        if (current == OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID);
        }
        if (current == OrderStatus.TIMEOUT || current == OrderStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ORDER_TIMEOUT);
        }
        if (current != OrderStatus.UNPAID) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }
        // UNPAID 但已过支付截止时间
        if (order.getPayExpireTime() != null && LocalDateTime.now().isAfter(order.getPayExpireTime())) {
            throw new BusinessException(ErrorCode.ORDER_TIMEOUT);
        }

        BigDecimal payAmount = order.getPayAmount();

        // Phase 4b-2：支付扣款逻辑统一委托至 PaymentService
        paymentService.pay(userId, payAmount, payMethod);

        // H-C3 修复：状态判定下沉到 SQL 乐观锁，防并发双扣。
        // UPDATE ... SET status=PAID WHERE id=? AND status='UNPAID'
        // 钱包扣减已在上一步完成，若状态变更失败事务回滚，钱包扣减也回滚。
        int updateRows = normalOrderMapper.update(null, new LambdaUpdateWrapper<NormalOrder>()
                .eq(NormalOrder::getId, orderId)
                .eq(NormalOrder::getStatus, OrderStatus.UNPAID)
                .set(NormalOrder::getStatus, OrderStatus.PAID)
                .set(NormalOrder::getPayTime, LocalDateTime.now())
                .set(NormalOrder::getPayMethod, payMethod)
                .set(NormalOrder::getTransactionId, generateTransactionId()));
        if (updateRows == 0) {
            // 并发支付或状态已变，重新查询返回具体错误
            NormalOrder refreshed = normalOrderMapper.selectById(orderId);
            if (refreshed != null && refreshed.getStatus() == OrderStatus.PAID) {
                throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID);
            }
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        // 优惠券核销：支付成功后，若订单使用了优惠券则核销（UNUSED → USED）
        if (order.getUserCouponId() != null) {
            try {
                couponUsageApi.useCoupon(com.seckill.mall.coupon.api.command.UseCouponCommand.builder()
                        .userCouponId(order.getUserCouponId())
                        .userId(userId)
                        .orderId(orderId)
                        .build());
            } catch (Exception e) {
                // 核销失败不阻断支付主流程（支付已完成），仅记录日志由对账任务兜底
                log.error("支付成功后优惠券核销失败，orderId={}, userCouponId={}", orderId, order.getUserCouponId(), e);
            }
        }

        // 邮件通知（异步，失败不影响主流程）
        String email = userApi.getUserEmail(userId);
        if (email != null) {
            try {
                emailService.sendPaySuccess(email, order.getOrderNo(), order.getPayAmount(),
                        LocalDateTime.now().format(PAY_TIME_FORMATTER));
            } catch (Exception e) {
                log.error("普通订单支付成功邮件发送失败，orderId={}", orderId, e);
            }
        }
        return orderQueryService.getNormalOrderDetail(userId, orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NormalOrderDetailVO cancelNormalOrder(Long userId, Long orderId) {
        // 1. 加载并校验订单归属
        NormalOrder order = loadAndCheckNormalOrderOwnership(userId, orderId);
        // 2. 5.7.3 状态机防并发重复回补：仅 UNPAID 状态可取消
        if (order.getStatus() != OrderStatus.UNPAID) {
            // 幂等：已取消 / 已完成 / 取消中等状态直接返回当前订单
            log.info("普通订单非待支付状态，取消幂等返回，orderNo={}, status={}",
                    order.getOrderNo(), order.getStatus());
            List<NormalOrderItem> currentItems = normalOrderItemMapper.selectList(
                    new LambdaQueryWrapper<NormalOrderItem>()
                            .eq(NormalOrderItem::getOrderId, orderId)
                            .orderByAsc(NormalOrderItem::getId));
            return assembleDetail(order, currentItems);
        }
        // 3. 乐观锁：UNPAID → CANCELLING 只能成功一次（建议7 状态机）
        int rows = normalOrderMapper.update(null, new LambdaUpdateWrapper<NormalOrder>()
                .eq(NormalOrder::getId, orderId)
                .eq(NormalOrder::getStatus, OrderStatus.UNPAID)
                .set(NormalOrder::getStatus, OrderStatus.CANCELLING));
        if (rows == 0) {
            // 已被其他请求（如超时任务）抢占，直接返回
            log.info("普通订单取消被并发抢占，幂等返回，orderNo={}", order.getOrderNo());
            List<NormalOrderItem> currentItems = normalOrderItemMapper.selectList(
                    new LambdaQueryWrapper<NormalOrderItem>()
                            .eq(NormalOrderItem::getOrderId, orderId)
                            .orderByAsc(NormalOrderItem::getId));
            // 重新加载订单状态
            NormalOrder refreshed = normalOrderMapper.selectById(orderId);
            return assembleDetail(refreshed != null ? refreshed : order, currentItems);
        }
        // 4. 查询订单明细，用于回补库存
        List<NormalOrderItem> items = normalOrderItemMapper.selectList(
                new LambdaQueryWrapper<NormalOrderItem>()
                        .eq(NormalOrderItem::getOrderId, orderId)
                        .orderByAsc(NormalOrderItem::getId));
        // 5. 5.7.3：回补库存（SKU 库存或商品库存）
        rollbackStockByItems(items);
        // 6. 置为已取消：CANCELLING → CANCELLED
        normalOrderMapper.update(null, new LambdaUpdateWrapper<NormalOrder>()
                .eq(NormalOrder::getId, orderId)
                .eq(NormalOrder::getStatus, OrderStatus.CANCELLING)
                .set(NormalOrder::getStatus, OrderStatus.CANCELLED)
                .set(NormalOrder::getCancelTime, LocalDateTime.now())
                .set(NormalOrder::getCancelReason, CANCEL_REASON_USER));

        // 优惠券回退：取消订单时，若订单使用了优惠券则回退（USED → UNUSED）
        // 注意：未支付订单的优惠券尚未核销（useCoupon 在支付成功时才调用），
        // 此处回退是兜底处理，revertCoupon 内部会校验状态，非 USED 时幂等返回。
        if (order.getUserCouponId() != null) {
            try {
                couponUsageApi.revertCoupon(com.seckill.mall.coupon.api.command.RevertCouponCommand.builder()
                        .userCouponId(order.getUserCouponId())
                        .userId(userId)
                        .build());
            } catch (Exception e) {
                // 回退失败不阻断取消主流程，仅记录日志由对账任务兜底
                log.error("取消订单后优惠券回退失败，orderId={}, userCouponId={}", orderId, order.getUserCouponId(), e);
            }
        }

        log.info("普通订单取消成功，orderNo={}, userId={}, itemIdCount={}",
                order.getOrderNo(), userId, items.size());

        // 7. 取消邮件通知（异步，失败不影响主流程）
        String email = userApi.getUserEmail(userId);
        if (email != null) {
            try {
                emailService.sendOrderCancel(email, order.getOrderNo(), CANCEL_REASON_USER);
            } catch (Exception e) {
                log.error("普通订单取消邮件发送失败，orderId={}", orderId, e);
            }
        }
        NormalOrder refreshed = normalOrderMapper.selectById(orderId);
        return assembleDetail(refreshed != null ? refreshed : order, items);
    }

    // ==================== 发货与确认收货流程（Bug2修复） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipNormalOrder(Long userId, Long orderId, String shippingCompany, String shippingNo) {
        NormalOrder order = normalOrderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "仅已支付订单可发货");
        }
        order.setStatus(OrderStatus.SHIPPED);
        order.setShippingCompany(shippingCompany);
        order.setShippingNo(shippingNo);
        order.setShipTime(LocalDateTime.now());
        normalOrderMapper.updateById(order);
        log.info("普通订单发货成功，orderId={}, orderNo={}, shippingCompany={}, shippingNo={}",
                orderId, order.getOrderNo(), shippingCompany, shippingNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmNormalOrder(Long userId, Long orderId) {
        NormalOrder order = loadAndCheckNormalOrderOwnership(userId, orderId);
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "仅已发货订单可确认收货");
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setConfirmTime(LocalDateTime.now());
        normalOrderMapper.updateById(order);
        log.info("普通订单确认收货成功，orderId={}, orderNo={}", orderId, order.getOrderNo());
    }

    // ==================== 私有辅助方法 ====================

    private String generateTransactionId() {
        return "PAY" + LocalDateTime.now().format(ORDER_NO_FORMATTER) + randomDigits(6);
    }

    private String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 加载普通订单并校验归属当前用户。
     */
    private NormalOrder loadAndCheckNormalOrderOwnership(Long userId, Long orderId) {
        NormalOrder order = normalOrderMapper.selectById(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    /**
     * 5.7.3：回补库存（SKU 库存或商品库存），用于订单取消 / 超时。
     * <p>
     * 遍历订单明细，对每项：
     * <ul>
     *   <li>skuId != null && skuId != 0：调用 {@code inventoryApi.rollbackSkuStock} 回补 SKU 库存，
     *       并刷新 t_product.total_stock 冗余字段（建议3）</li>
     *   <li>skuId == null || skuId == 0：委托 {@link InventoryApi#rollbackProductStock(RestoreStockCommand)} 回补 t_product.stock 与 sales_count</li>
     * </ul>
     *
     * @param items 订单明细列表
     */
    private void rollbackStockByItems(List<NormalOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        // 1. 回补 SKU 库存
        Map<Long, Integer> qtyBySku = new HashMap<>();
        Map<Long, Long> skuToProduct = new HashMap<>();
        for (NormalOrderItem item : items) {
            if (item.getSkuId() != null && item.getSkuId() != 0L
                    && item.getQuantity() != null && item.getQuantity() > 0) {
                qtyBySku.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
                skuToProduct.putIfAbsent(item.getSkuId(), item.getProductId());
            }
        }
        for (Map.Entry<Long, Integer> e : qtyBySku.entrySet()) {
            // Phase P.5：统一通过 InventoryApi 入口回补 SKU 库存
            inventoryApi.rollbackSkuStock(new RestoreStockCommand(null, e.getKey(), e.getValue()));
            // 建议3：同步刷新 t_product.total_stock
            Long productId = skuToProduct.get(e.getKey());
            if (productId != null) {
                skuApi.refreshTotalStock(productId);
            }
        }
        // 2. 回补无规格商品库存
        Map<Long, Integer> qtyByProduct = new HashMap<>();
        for (NormalOrderItem item : items) {
            if (item.getSkuId() == null || item.getSkuId() == 0L) {
                if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                    continue;
                }
                qtyByProduct.merge(item.getProductId(), item.getQuantity(), Integer::sum);
            }
        }
        for (Map.Entry<Long, Integer> e : qtyByProduct.entrySet()) {
            inventoryApi.rollbackProductStock(new RestoreStockCommand(e.getKey(), null, e.getValue()));
        }
    }

    /**
     * 组装订单详情视图。
     * <p>
     * 将 Entity（NormalOrder / NormalOrderItem）字段复制到独立 VO
     * （NormalOrderVO / NormalOrderItemVO），避免 VO 直接持有 Entity 引用
     * 而违反 ArchUnit VO→Entity 分层规则。字段名一致，JSON 响应结构不变。
     */
    private NormalOrderDetailVO assembleDetail(NormalOrder order, List<NormalOrderItem> items) {
        NormalOrderDetailVO vo = new NormalOrderDetailVO();
        NormalOrderVO orderVO = new NormalOrderVO();
        BeanUtils.copyProperties(order, orderVO);
        vo.setOrder(orderVO);
        List<NormalOrderItemVO> itemVOs = items.stream().map(item -> {
            NormalOrderItemVO itemVO = new NormalOrderItemVO();
            BeanUtils.copyProperties(item, itemVO);
            return itemVO;
        }).collect(Collectors.toList());
        vo.setItems(itemVOs);
        // 查询收货地址
        if (order.getAddressId() != null) {
            try {
                // Phase 7：改用 UserAddressService 跨模块内部调用，消除 UserAddressMapper 依赖
                UserAddress address = userAddressService.getAddressById(order.getAddressId());
                if (address != null) {
                    vo.setReceiverName(address.getReceiverName());
                    vo.setReceiverPhone(address.getReceiverPhone());
                    vo.setProvince(address.getProvince());
                    vo.setCity(address.getCity());
                    vo.setDistrict(address.getDistrict());
                    vo.setDetailAddress(address.getDetailAddress());
                }
            } catch (Exception e) {
                log.warn("查询订单收货地址失败, orderId={}, addressId={}", order.getId(), order.getAddressId(), e);
            }
        }
        return vo;
    }
}