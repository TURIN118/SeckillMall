package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.converter.SeckillOrderConverter;
import com.seckill.mall.entity.SeckillGoods;
import com.seckill.mall.entity.SeckillOrder;
import com.seckill.mall.entity.enums.OrderStatus;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.mapper.SeckillGoodsMapper;
import com.seckill.mall.mapper.SeckillOrderMapper;
import com.seckill.mall.service.EmailService;
import com.seckill.mall.service.PaymentService;
import com.seckill.mall.service.ProductService;
import com.seckill.mall.service.SeckillInventoryPort;
import com.seckill.mall.service.SeckillOrderService;
import com.seckill.mall.service.UserService;
import com.seckill.mall.vo.SeckillOrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 秒杀订单领域服务实现（Phase 4b-1 从 OrderServiceImpl 沿自然缝拆分而来）。
 * <p>
 * 仅承载秒杀订单相关用例，与普通订单（{@link OrderServiceImpl}）完全独立。
 * 业务逻辑与原 OrderServiceImpl 完全一致，仅做机械移动。
 * <p>
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillOrderServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillOrderServiceImpl implements SeckillOrderService {

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter PAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String CANCEL_REASON_USER = "用户主动取消";
    private static final String CANCEL_REASON_TIMEOUT = "超时未支付，自动取消";

    // 一人一单：当前表结构 uk_user_seckill 约束限购 1 件
    private static final int SECKILL_QUANTITY = 1;

    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final ProductService productService;
    private final SeckillInventoryPort seckillInventoryPort;
    private final UserService userService;
    private final EmailService emailService;
    // Phase 4b-2：支付扣款逻辑抽取至 PaymentService（钱包扣款 + 模拟支付）
    private final PaymentService paymentService;
    // 问题4修复：使用 MapStruct Converter 替代手工 SeckillOrderVO.from()，统一转换逻辑
    private final SeckillOrderConverter seckillOrderConverter;

    @Value("${seckill.pay-timeout-minutes:15}")
    private long payTimeoutMinutes;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillOrder createSeckillOrder(Long seckillId, Long userId, String requestId) {
        SeckillGoods goods = seckillGoodsMapper.selectById(seckillId);
        if (goods == null) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_FOUND);
        }
        if (!productService.existsById(goods.getProductId())) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        SeckillOrder order = new SeckillOrder();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setSeckillId(seckillId);
        order.setProductId(goods.getProductId());
        order.setSeckillPrice(goods.getSeckillPrice());
        order.setQuantity(SECKILL_QUANTITY);
        order.setTotalAmount(goods.getSeckillPrice().multiply(BigDecimal.valueOf(SECKILL_QUANTITY)));
        order.setStatus(OrderStatus.UNPAID);
        order.setPayExpireTime(LocalDateTime.now().plusMinutes(payTimeoutMinutes));

        // 物理删除同一(userId, seckillId)的终态订单，允许超时/取消后重新秒杀
        seckillOrderMapper.deleteTerminalOrders(userId, seckillId);

        try {
            seckillOrderMapper.insert(order);
        } catch (DuplicateKeyException e) {
            // uk_user_seckill 兜底：并发重复下单
            throw new BusinessException(ErrorCode.REPEAT_SECKILL);
        }
        return order;
    }

    @Override
    public PageResult<SeckillOrderVO> getOrderList(Long userId, Integer status, Integer pageNum, Integer pageSize) {
        int num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);

        Page<SeckillOrder> page = new Page<>(num, size);
        IPage<SeckillOrder> result = seckillOrderMapper.selectOrderPage(page, userId, null, parseStatus(status));
        // B4 修复：Entity → VO 转换，避免直接序列化 Entity
        List<SeckillOrderVO> voList = result.getRecords().stream()
                .map(seckillOrderConverter::toVO)
                .collect(Collectors.toList());
        return PageResult.of(voList, result.getTotal(), num, size);
    }

    @Override
    public SeckillOrderVO getOrderDetail(Long userId, Long orderId) {
        SeckillOrder order = loadAndCheckOwnership(userId, orderId);
        // 超时检查：UNPAID 且已过支付截止时间，自动更新为 TIMEOUT
        checkAndHandleSeckillOrderTimeout(order);
        // B4 修复：返回 VO 而非 Entity
        return seckillOrderConverter.toVO(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillOrderVO payOrder(Long userId, Long orderId, String payMethod) {
        SeckillOrder order = loadAndCheckOwnership(userId, orderId);
        OrderStatus current = order.getStatus();

        if (current == OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID);
        }
        if (current == OrderStatus.TIMEOUT) {
            throw new BusinessException(ErrorCode.ORDER_TIMEOUT);
        }
        if (current != OrderStatus.UNPAID) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }
        // UNPAID 但已过支付截止时间（延迟取消任务尚未处理）
        if (order.getPayExpireTime() != null && LocalDateTime.now().isAfter(order.getPayExpireTime())) {
            throw new BusinessException(ErrorCode.ORDER_TIMEOUT);
        }

        // M12 修复：秒杀订单支付支持钱包扣款，与 payNormalOrder 保持一致
        // Phase 4b-2：支付扣款逻辑统一委托至 PaymentService
        BigDecimal payAmount = order.getTotalAmount();
        paymentService.pay(userId, payAmount, payMethod);

        // H-C3 修复：状态判定下沉到 SQL 乐观锁，防并发双扣。
        // UPDATE ... SET status=PAID WHERE id=? AND status='UNPAID'
        // 钱包扣减已在上一步完成，若状态变更失败（rows==0）事务回滚，钱包扣减也回滚。
        int updateRows = seckillOrderMapper.update(null, new LambdaUpdateWrapper<SeckillOrder>()
                .eq(SeckillOrder::getId, orderId)
                .eq(SeckillOrder::getStatus, OrderStatus.UNPAID)
                .set(SeckillOrder::getStatus, OrderStatus.PAID)
                .set(SeckillOrder::getPayTime, LocalDateTime.now())
                .set(SeckillOrder::getPayMethod, payMethod)
                .set(SeckillOrder::getTransactionId, generateTransactionId()));
        if (updateRows == 0) {
            // 并发支付或状态已变，重新查询返回具体错误
            SeckillOrder refreshed = seckillOrderMapper.selectById(orderId);
            if (refreshed != null && refreshed.getStatus() == OrderStatus.PAID) {
                throw new BusinessException(ErrorCode.ORDER_ALREADY_PAID);
            }
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR);
        }

        // 重新加载订单（含更新后的字段）
        SeckillOrder paidOrder = seckillOrderMapper.selectById(orderId);

        // L8: 支付成功邮件：异步发送，失败不影响主流程，添加 try-catch 防止异常冒泡
        String email = userService.getEmail(userId);
        if (email != null) {
            try {
                emailService.sendPaySuccess(email, paidOrder.getOrderNo(), paidOrder.getTotalAmount(),
                        paidOrder.getPayTime().format(PAY_TIME_FORMATTER));
            } catch (Exception e) {
                log.error("秒杀订单支付成功邮件发送失败，orderId={}", orderId, e);
            }
        }
        // B4 修复：返回 VO 而非 Entity
        return seckillOrderConverter.toVO(paidOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillOrderVO cancelOrder(Long userId, Long orderId) {
        SeckillOrder order = loadAndCheckOwnership(userId, orderId);
        if (order.getStatus() != OrderStatus.UNPAID) {
            throw new BusinessException(ErrorCode.ORDER_CANCEL_FAILED);
        }

        // H-C7 修复：引入 UNPAID→CANCELLING→CANCELLED 乐观锁状态机，防并发双回补。
        // 步骤1：UNPAID → CANCELLING（只有一个请求能成功）
        int rows = seckillOrderMapper.update(null, new LambdaUpdateWrapper<SeckillOrder>()
                .eq(SeckillOrder::getId, orderId)
                .eq(SeckillOrder::getStatus, OrderStatus.UNPAID)
                .set(SeckillOrder::getStatus, OrderStatus.CANCELLING));
        if (rows == 0) {
            // 被并发抢占（如超时取消任务）
            log.info("秒杀订单取消被并发抢占，幂等返回 orderNo={}", order.getOrderNo());
            throw new BusinessException(ErrorCode.ORDER_CANCEL_FAILED);
        }

        // H-C1 修复：回补 DB + Redis 库存移到事务提交后执行，避免事务回滚后不一致
        Long seckillId = order.getSeckillId();
        registerAfterCommit(() -> seckillInventoryPort.rollback(seckillId, userId));

        // 步骤2：CANCELLING → CANCELLED
        // 问题6修复：检查第二步 update 返回行数，若为 0 记录 warn 日志。
        // 此时已持有 CANCELLING 行锁，理论上不会失败；若失败可能是行被并发修改，
        // 不抛异常以保持幂等，由后续 selectById 返回最新状态。
        int cancelRows = seckillOrderMapper.update(null, new LambdaUpdateWrapper<SeckillOrder>()
                .eq(SeckillOrder::getId, orderId)
                .eq(SeckillOrder::getStatus, OrderStatus.CANCELLING)
                .set(SeckillOrder::getStatus, OrderStatus.CANCELLED)
                .set(SeckillOrder::getCancelTime, LocalDateTime.now())
                .set(SeckillOrder::getCancelReason, CANCEL_REASON_USER));
        if (cancelRows == 0) {
            log.warn("秒杀订单 CANCELLING→CANCELLED 更新未生效，orderId={}，可能被并发修改", orderId);
        }

        // 重新加载订单
        SeckillOrder cancelledOrder = seckillOrderMapper.selectById(orderId);

        // 订单取消邮件：异步发送，失败不影响主流程
        String email = userService.getEmail(userId);
        if (email != null) {
            try {
                emailService.sendOrderCancel(email, cancelledOrder.getOrderNo(), CANCEL_REASON_USER);
            } catch (Exception e) {
                log.error("订单取消邮件发送失败，orderId={}", orderId, e);
            }
        }
        // B4 修复：返回 VO 而非 Entity
        return seckillOrderConverter.toVO(cancelledOrder);
    }

    @Override
    public String getOrderStatus(Long userId, Long orderId) {
        // B4 修复：返回状态字符串而非枚举
        OrderStatus status = loadAndCheckOwnership(userId, orderId).getStatus();
        return status != null ? status.getCode() : null;
    }

    /**
     * 超时取消（由延迟消费者触发）：UNPAID → CANCELLING → TIMEOUT，并回补库存。
     * H-C7 修复：引入乐观锁状态机，防并发双回补。
     * 已支付/已取消等终态视为幂等忽略。
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean timeoutCancel(Long orderId) {
        SeckillOrder order = seckillOrderMapper.selectById(orderId);
        if (order == null) {
            return false;
        }
        if (order.getStatus() != OrderStatus.UNPAID) {
            return false;
        }
        // H-C7 修复：UNPAID → CANCELLING 乐观锁，保证只有一个请求能进入回补流程
        int rows = seckillOrderMapper.update(null, new LambdaUpdateWrapper<SeckillOrder>()
                .eq(SeckillOrder::getId, orderId)
                .eq(SeckillOrder::getStatus, OrderStatus.UNPAID)
                .set(SeckillOrder::getStatus, OrderStatus.CANCELLING));
        if (rows == 0) {
            // 已被其他请求（如用户主动取消）抢占，直接返回
            log.info("秒杀订单超时取消被并发抢占，幂等返回 orderNo={}", order.getOrderNo());
            return false;
        }
        // H-C1 修复：回补 DB + Redis 库存移到事务提交后执行
        Long seckillId = order.getSeckillId();
        Long orderUserId = order.getUserId();
        registerAfterCommit(() -> seckillInventoryPort.rollback(seckillId, orderUserId));

        // CANCELLING → TIMEOUT
        seckillOrderMapper.update(null, new LambdaUpdateWrapper<SeckillOrder>()
                .eq(SeckillOrder::getId, orderId)
                .eq(SeckillOrder::getStatus, OrderStatus.CANCELLING)
                .set(SeckillOrder::getStatus, OrderStatus.TIMEOUT)
                .set(SeckillOrder::getCancelTime, LocalDateTime.now())
                .set(SeckillOrder::getCancelReason, CANCEL_REASON_TIMEOUT));

        log.info("秒杀订单超时取消成功，orderNo={}, orderId={}", order.getOrderNo(), orderId);

        // 超时取消邮件：异步发送，失败不影响主流程
        String email = userService.getEmail(order.getUserId());
        if (email != null) {
            try {
                emailService.sendOrderCancel(email, order.getOrderNo(), CANCEL_REASON_TIMEOUT);
            } catch (Exception e) {
                log.error("超时取消邮件发送失败，orderId={}", orderId, e);
            }
        }
        return true;
    }

    /**
     * H10: 将 Redis 操作注册到事务提交后执行；无事务上下文时直接执行。
     * 避免事务回滚后缓存与 DB 不一致。
     */
    private void registerAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }

    /**

     * H-C2 修复：物理删除秒杀订单（用于消费者 DB 扣减失败时撤销幽灵单）。
     * <p>
     * 仅在订单为 UNPAID 状态时删除，避免误删已进入业务流程的订单。
     * 使用物理删除而非逻辑删除，因为 uk_user_seckill 唯一索引不包含 is_deleted。
     *
     * @param orderId 秒杀订单 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSeckillOrderPhysically(Long orderId) {
        SeckillOrder order = seckillOrderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        if (order.getStatus() != OrderStatus.UNPAID) {
            log.warn("物理删除秒杀订单跳过：订单非 UNPAID 状态 orderId={} status={}",
                    orderId, order.getStatus());
            return;
        }
        // 物理删除（绕过 MyBatis-Plus 逻辑删除）
        seckillOrderMapper.deletePhysical(orderId);
        log.info("物理删除秒杀订单成功 orderId={} orderNo={}", orderId, order.getOrderNo());
    }

    private SeckillOrder loadAndCheckOwnership(Long userId, Long orderId) {
        SeckillOrder order = seckillOrderMapper.selectById(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }


    private OrderStatus parseStatus(Integer status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case 0 -> OrderStatus.UNPAID;
            case 1 -> OrderStatus.PAID;
            case 2 -> OrderStatus.SHIPPED;
            case 3 -> OrderStatus.CANCELLED;
            case 4 -> OrderStatus.TIMEOUT;
            case 5 -> OrderStatus.COMPLETED;
            default -> null;
        };
    }

    private String generateOrderNo() {
        return "SK" + LocalDateTime.now().format(ORDER_NO_FORMATTER) + randomDigits(6);
    }

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

    // ==================== 发货与确认收货流程（Bug2修复） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void shipOrder(Long userId, Long orderId, String shippingCompany, String shippingNo) {
        SeckillOrder order = seckillOrderMapper.selectById(orderId);
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
        seckillOrderMapper.updateById(order);
        log.info("秒杀订单发货成功，orderId={}, orderNo={}, shippingCompany={}, shippingNo={}",
                orderId, order.getOrderNo(), shippingCompany, shippingNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmOrder(Long userId, Long orderId) {
        SeckillOrder order = loadAndCheckOwnership(userId, orderId);
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_ERROR, "仅已发货订单可确认收货");
        }
        order.setStatus(OrderStatus.COMPLETED);
        order.setConfirmTime(LocalDateTime.now());
        seckillOrderMapper.updateById(order);
        log.info("秒杀订单确认收货成功，orderId={}, orderNo={}", orderId, order.getOrderNo());
    }

    /**
     * 检查秒杀订单超时：如果状态为UNPAID且已过支付截止时间，更新为TIMEOUT。
     * 兜底RabbitMQ延迟消息可能丢失的场景。
     */
    private void checkAndHandleSeckillOrderTimeout(SeckillOrder order) {
        if (order.getStatus() == OrderStatus.UNPAID
                && order.getPayExpireTime() != null
                && LocalDateTime.now().isAfter(order.getPayExpireTime())) {
            int rows = seckillOrderMapper.update(null, new LambdaUpdateWrapper<SeckillOrder>()
                    .eq(SeckillOrder::getId, order.getId())
                    .eq(SeckillOrder::getStatus, OrderStatus.UNPAID)
                    .set(SeckillOrder::getStatus, OrderStatus.TIMEOUT)
                    .set(SeckillOrder::getCancelTime, LocalDateTime.now())
                    .set(SeckillOrder::getCancelReason, "支付超时自动取消"));
            if (rows > 0) {
                order.setStatus(OrderStatus.TIMEOUT);
                order.setCancelTime(LocalDateTime.now());
                order.setCancelReason("支付超时自动取消");
                log.info("秒杀订单超时自动关闭, orderId={}", order.getId());
            }
        }
    }

    // ==================== Phase 7：跨模块内部调用入口（供 OrderServiceImpl 使用） ====================

    @Override
    public SeckillOrder getSeckillOrderById(Long orderId) {
        return seckillOrderMapper.selectById(orderId);
    }

    @Override
    public List<SeckillOrder> getSeckillOrdersForUnifiedList(Long userId, Integer status, int limit) {
        LambdaQueryWrapper<SeckillOrder> wrapper = new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getUserId, userId)
                .orderByDesc(SeckillOrder::getCreateTime)
                .last("LIMIT " + limit);
        OrderStatus statusFilter = parseStatus(status);
        if (statusFilter != null) {
            wrapper.eq(SeckillOrder::getStatus, statusFilter);
        }
        return seckillOrderMapper.selectList(wrapper);
    }

    @Override
    public boolean logicalDeleteSeckillOrder(Long orderId) {
        int rows = seckillOrderMapper.deleteById(orderId);
        return rows > 0;
    }
}