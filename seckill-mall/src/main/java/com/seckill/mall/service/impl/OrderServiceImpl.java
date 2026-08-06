package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.seckill.mall.cache.RedisKeyConstants;
import com.seckill.mall.cache.RedisService;
import com.seckill.mall.common.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.config.RabbitMQConfig;
import com.seckill.mall.entity.Cart;
import com.seckill.mall.entity.NormalOrder;
import com.seckill.mall.entity.NormalOrderItem;
import com.seckill.mall.entity.Product;
import com.seckill.mall.entity.SeckillGoods;
import com.seckill.mall.entity.SeckillOrder;
import com.seckill.mall.entity.User;
import com.seckill.mall.entity.UserAddress;
import com.seckill.mall.entity.enums.OrderStatus;
import com.seckill.mall.entity.enums.ProductStatus;
import com.seckill.mall.mapper.CartMapper;
import com.seckill.mall.mapper.NormalOrderItemMapper;
import com.seckill.mall.mapper.NormalOrderMapper;
import com.seckill.mall.mapper.ProductMapper;
import com.seckill.mall.mapper.SeckillGoodsMapper;
import com.seckill.mall.mapper.SeckillOrderMapper;
import com.seckill.mall.mapper.UserAddressMapper;
import com.seckill.mall.mapper.UserMapper;
import com.seckill.mall.mq.message.OrderDelayMessage;
import com.seckill.mall.service.EmailService;
import com.seckill.mall.service.OrderService;
import com.seckill.mall.vo.NormalOrderDetailVO;
import com.seckill.mall.vo.OrderListItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter PAY_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String CANCEL_REASON_USER = "用户主动取消";
    private static final String CANCEL_REASON_TIMEOUT = "超时未支付，自动取消";

    // 一人一单：当前表结构 uk_user_seckill 约束限购 1 件
    private static final int SECKILL_QUANTITY = 1;

    /** 钱包支付方式标识 */
    private static final String PAY_METHOD_WALLET = "WALLET";
    /** 普通订单默认运费（当前简化为 0，后续可按地址/重量扩展） */
    private static final BigDecimal DEFAULT_FREIGHT = BigDecimal.ZERO;

    private final SeckillOrderMapper seckillOrderMapper;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final ProductMapper productMapper;
    private final RedisService redisService;
    private final UserMapper userMapper;
    private final EmailService emailService;
    private final NormalOrderMapper normalOrderMapper;
    private final NormalOrderItemMapper normalOrderItemMapper;
    private final CartMapper cartMapper;
    private final UserAddressMapper userAddressMapper;
    private final RabbitTemplate rabbitTemplate;

    @Value("${seckill.pay-timeout-minutes:15}")
    private long payTimeoutMinutes;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillOrder createSeckillOrder(Long seckillId, Long userId, String requestId) {
        SeckillGoods goods = seckillGoodsMapper.selectById(seckillId);
        if (goods == null) {
            throw new BusinessException(ErrorCode.SECKILL_NOT_FOUND);
        }
        Product product = productMapper.selectById(goods.getProductId());
        if (product == null) {
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
    public PageResult<SeckillOrder> getOrderList(Long userId, Integer status, Integer pageNum, Integer pageSize) {
        int num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);

        Page<SeckillOrder> page = new Page<>(num, size);
        IPage<SeckillOrder> result = seckillOrderMapper.selectOrderPage(page, userId, null, parseStatus(status));
        return PageResult.of(result.getRecords(), result.getTotal(), num, size);
    }

    @Override
    public SeckillOrder getOrderDetail(Long userId, Long orderId) {
        SeckillOrder order = loadAndCheckOwnership(userId, orderId);
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillOrder payOrder(Long userId, Long orderId, String payMethod) {
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
        BigDecimal payAmount = order.getTotalAmount();
        if (PAY_METHOD_WALLET.equalsIgnoreCase(payMethod)) {
            // 钱包支付：原子扣减余额，余额不足提示去充值
            int rows = userMapper.deductBalance(userId, payAmount);
            if (rows == 0) {
                throw new BusinessException(ErrorCode.WALLET_BALANCE_NOT_ENOUGH);
            }
            log.info("秒杀订单钱包扣款成功，userId={}, orderId={}, amount={}", userId, orderId, payAmount);
        } else {
            // 模拟支付（ALIPAY/WECHAT 等）：直接置 PAID
            log.info("秒杀订单模拟支付成功，userId={}, orderId={}, payMethod={}, amount={}",
                    userId, orderId, payMethod, payAmount);
        }

        order.setStatus(OrderStatus.PAID);
        order.setPayTime(LocalDateTime.now());
        order.setPayMethod(payMethod);
        order.setTransactionId(generateTransactionId());
        seckillOrderMapper.updateById(order);

        // L8: 支付成功邮件：异步发送，失败不影响主流程，添加 try-catch 防止异常冒泡
        String email = getUserEmail(userId);
        if (email != null) {
            try {
                emailService.sendPaySuccess(email, order.getOrderNo(), order.getTotalAmount(),
                        order.getPayTime().format(PAY_TIME_FORMATTER));
            } catch (Exception e) {
                log.error("秒杀订单支付成功邮件发送失败，orderId={}", orderId, e);
            }
        }
        return order;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SeckillOrder cancelOrder(Long userId, Long orderId) {
        SeckillOrder order = loadAndCheckOwnership(userId, orderId);
        if (order.getStatus() != OrderStatus.UNPAID) {
            throw new BusinessException(ErrorCode.ORDER_CANCEL_FAILED);
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(CANCEL_REASON_USER);
        seckillOrderMapper.updateById(order);

        // H10 修复：回补 Redis 库存移到事务提交后执行，避免事务回滚后缓存与 DB 不一致
        Long seckillId = order.getSeckillId();
        registerAfterCommit(() -> rollbackStock(seckillId, userId));

        // 订单取消邮件：异步发送，失败不影响主流程
        String email = getUserEmail(userId);
        if (email != null) {
            try {
                emailService.sendOrderCancel(email, order.getOrderNo(), CANCEL_REASON_USER);
            } catch (Exception e) {
                log.error("订单取消邮件发送失败，orderId={}", orderId, e);
            }
        }
        return order;
    }

    @Override
    public OrderStatus getOrderStatus(Long userId, Long orderId) {
        return loadAndCheckOwnership(userId, orderId).getStatus();
    }

    /**
     * 超时取消（由延迟消费者触发）：UNPAID → TIMEOUT，并回补库存。
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
        order.setStatus(OrderStatus.TIMEOUT);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(CANCEL_REASON_TIMEOUT);
        seckillOrderMapper.updateById(order);
        // H10 修复：回补 Redis 库存移到事务提交后执行
        Long seckillId = order.getSeckillId();
        Long orderUserId = order.getUserId();
        registerAfterCommit(() -> rollbackStock(seckillId, orderUserId));

        // 超时取消邮件：异步发送，失败不影响主流程
        String email = getUserEmail(order.getUserId());
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
     * Bug1修复：普通订单超时取消（延迟消费者触发）。
     * UNPAID → TIMEOUT，回补商品库存与销量，已支付等终态幂等忽略。
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
        // 查询订单明细，用于回补库存
        List<NormalOrderItem> items = normalOrderItemMapper.selectList(
                new LambdaQueryWrapper<NormalOrderItem>()
                        .eq(NormalOrderItem::getOrderId, orderId));
        // 回补商品库存与销量
        rollbackProductStock(items);
        // 更新订单状态为 TIMEOUT
        order.setStatus(OrderStatus.TIMEOUT);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(CANCEL_REASON_TIMEOUT);
        normalOrderMapper.updateById(order);

        log.info("普通订单超时取消成功，orderNo={}, orderId={}", order.getOrderNo(), orderId);

        // 超时取消邮件：异步发送，失败不影响主流程
        String email = getUserEmail(order.getUserId());
        if (email != null) {
            try {
                emailService.sendOrderCancel(email, order.getOrderNo(), CANCEL_REASON_TIMEOUT);
            } catch (Exception e) {
                log.error("普通订单超时取消邮件发送失败，orderId={}", orderId, e);
            }
        }
        return true;
    }

    /**
     * Bug1修复：发送普通订单延迟消息，超时后自动取消。
     */
    private void sendNormalOrderDelayMessage(NormalOrder order) {
        try {
            OrderDelayMessage delay = new OrderDelayMessage();
            delay.setOrderId(order.getId());
            delay.setOrderNo(order.getOrderNo());
            delay.setOrderType("NORMAL");
            delay.setExpireTime(order.getPayExpireTime() == null ? null
                    : order.getPayExpireTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_DELAY_EXCHANGE,
                    RabbitMQConfig.ORDER_DELAY_ROUTING_KEY,
                    delay);
            log.info("普通订单延迟消息已发送，orderNo={}, orderId={}", order.getOrderNo(), order.getId());
        } catch (Exception e) {
            log.error("普通订单延迟消息发送失败，orderNo={}，依赖补偿任务兜底", order.getOrderNo(), e);
        }
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

    private void rollbackStock(Long seckillId, Long userId) {
        try {
            redisService.incr(RedisKeyConstants.seckillStock(seckillId));
            redisService.sRem(RedisKeyConstants.seckillBought(seckillId), String.valueOf(userId));
        } catch (Exception e) {
            // L10: Redis 异常不阻断主流程，由补偿任务兜底；记录 warn 便于监控
            log.warn("回补 Redis 库存失败，需补偿任务兜底 seckillId={} userId={}", seckillId, userId, e);
        }
    }

    private SeckillOrder loadAndCheckOwnership(Long userId, Long orderId) {
        SeckillOrder order = seckillOrderMapper.selectById(orderId);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private String getUserEmail(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = userMapper.selectById(userId);
        return user == null ? null : user.getEmail();
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

    // ==================== 普通订单（需求5 立即购买 + 需求13 购物车结算） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NormalOrderDetailVO createNormalOrder(Long userId, Long productId, Integer quantity,
                                                 Long addressId, String remark) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "购买数量必须大于0");
        }
        // 1. 校验商品状态与库存
        Product product = loadOnSaleProduct(productId);
        if (product.getStock() == null || product.getStock() < quantity) {
            throw new BusinessException(ErrorCode.STOCK_EMPTY);
        }
        // 2. 校验收货地址归属
        checkAddressOwnership(userId, addressId);
        // 3. 扣库存（乐观锁：WHERE stock >= quantity AND status=ON_SALE）
        int updated = deductProductStock(productId, quantity);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.STOCK_EMPTY);
        }
        // 4. 建普通订单 + 明细
        BigDecimal unitPrice = product.getOriginalPrice();
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        NormalOrder order = buildNormalOrder(userId, addressId, remark, subtotal);
        normalOrderMapper.insert(order);

        NormalOrderItem item = buildOrderItem(order.getId(), product, unitPrice, quantity);
        normalOrderItemMapper.insert(item);

        log.info("立即购买创建普通订单成功，orderNo={}, userId={}, productId={}, quantity={}",
                order.getOrderNo(), userId, productId, quantity);
        // Bug1修复：创建普通订单后发送延迟消息，超时自动取消
        sendNormalOrderDelayMessage(order);
        return assembleDetail(order, List.of(item));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NormalOrderDetailVO createOrderFromCart(Long userId, Long addressId,
                                                   List<Long> cartIds, String remark) {
        if (cartIds == null || cartIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "待结算购物车项不能为空");
        }
        // 1. 校验地址归属
        checkAddressOwnership(userId, addressId);
        // 2. 查询购物车项并校验归属
        // L9: 若 cartIds 含重复 ID，selectList(in) 会去重返回，导致 carts.size() != cartIds.size() 误报错
        // 完整方案应在入口去重：cartIds = cartIds.stream().distinct().toList()
        List<Cart> carts = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>().in(Cart::getId, cartIds));
        if (carts.size() != cartIds.size()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        for (Cart c : carts) {
            if (!userId.equals(c.getUserId())) {
                throw new BusinessException(ErrorCode.CART_ITEM_FORBIDDEN);
            }
        }
        // 3. 批量查询商品并校验在售 + 库存
        List<Long> productIds = carts.stream().map(Cart::getProductId).distinct().collect(Collectors.toList());
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>().in(Product::getId, productIds));
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        for (Cart c : carts) {
            Product p = productMap.get(c.getProductId());
            if (p == null) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            if (p.getStatus() != ProductStatus.ON_SALE) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "商品[" + p.getName() + "]已下架");
            }
            if (p.getStock() == null || p.getStock() < c.getQuantity()) {
                throw new BusinessException(ErrorCode.STOCK_EMPTY);
            }
        }
        // 4. 计算总额 & 构造明细
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<NormalOrderItem> items = new ArrayList<>(carts.size());
        // 先建订单占位拿 orderId，再回填 item
        NormalOrder order = buildNormalOrder(userId, addressId, remark, BigDecimal.ZERO);
        normalOrderMapper.insert(order);

        for (Cart c : carts) {
            Product p = productMap.get(c.getProductId());
            BigDecimal unitPrice = p.getOriginalPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(c.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            NormalOrderItem item = buildOrderItem(order.getId(), p, unitPrice, c.getQuantity());
            normalOrderItemMapper.insert(item);
            items.add(item);
        }
        // 5. 回写订单金额
        order.setTotalAmount(totalAmount);
        order.setFreightAmount(DEFAULT_FREIGHT);
        order.setPayAmount(totalAmount.add(DEFAULT_FREIGHT));
        normalOrderMapper.updateById(order);

        // 6. 扣库存（按商品聚合后乐观锁扣减）
        Map<Long, Integer> qtyByProduct = new HashMap<>();
        for (Cart c : carts) {
            qtyByProduct.merge(c.getProductId(), c.getQuantity(), Integer::sum);
        }
        for (Map.Entry<Long, Integer> e : qtyByProduct.entrySet()) {
            int updated = deductProductStock(e.getKey(), e.getValue());
            if (updated == 0) {
                // 并发库存不足，事务回滚
                throw new BusinessException(ErrorCode.STOCK_EMPTY);
            }
        }
        // 7. 删除已结算购物车项（逻辑删除）
        cartMapper.delete(new LambdaQueryWrapper<Cart>().in(Cart::getId, cartIds));

        log.info("购物车结算创建普通订单成功，orderNo={}, userId={}, cartIds={}, itemCount={}",
                order.getOrderNo(), userId, cartIds, carts.size());
        // Bug1修复：创建普通订单后发送延迟消息，超时自动取消
        sendNormalOrderDelayMessage(order);
        return assembleDetail(order, items);
    }

    @Override
    public NormalOrderDetailVO getNormalOrderDetail(Long userId, Long orderId) {
        NormalOrder order = loadAndCheckNormalOrderOwnership(userId, orderId);
        List<NormalOrderItem> items = normalOrderItemMapper.selectList(
                new LambdaQueryWrapper<NormalOrderItem>()
                        .eq(NormalOrderItem::getOrderId, orderId)
                        .orderByAsc(NormalOrderItem::getId));
        return assembleDetail(order, items);
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

        if (PAY_METHOD_WALLET.equalsIgnoreCase(payMethod)) {
            // 钱包支付：原子扣减余额，余额不足提示去充值
            int rows = userMapper.deductBalance(userId, payAmount);
            if (rows == 0) {
                throw new BusinessException(ErrorCode.WALLET_BALANCE_NOT_ENOUGH);
            }
            log.info("钱包扣款成功，userId={}, orderId={}, amount={}", userId, orderId, payAmount);
        } else {
            // 模拟支付（ALIPAY/WECHAT 等）：直接置 PAID
            log.info("模拟支付成功，userId={}, orderId={}, payMethod={}, amount={}",
                    userId, orderId, payMethod, payAmount);
        }

        order.setStatus(OrderStatus.PAID);
        order.setPayTime(LocalDateTime.now());
        order.setPayMethod(payMethod);
        order.setTransactionId(generateTransactionId());
        normalOrderMapper.updateById(order);

        // 邮件通知（异步，失败不影响主流程）
        String email = getUserEmail(userId);
        if (email != null) {
            try {
                emailService.sendPaySuccess(email, order.getOrderNo(), order.getPayAmount(),
                        order.getPayTime().format(PAY_TIME_FORMATTER));
            } catch (Exception e) {
                log.error("普通订单支付成功邮件发送失败，orderId={}", orderId, e);
            }
        }
        return getNormalOrderDetail(userId, orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NormalOrderDetailVO cancelNormalOrder(Long userId, Long orderId) {
        // 1. 加载并校验订单归属
        NormalOrder order = loadAndCheckNormalOrderOwnership(userId, orderId);
        // 2. 校验订单状态：仅 UNPAID 可取消
        if (order.getStatus() != OrderStatus.UNPAID) {
            throw new BusinessException(ErrorCode.ORDER_CANCEL_FAILED);
        }
        // 3. 查询订单明细，用于回补库存
        List<NormalOrderItem> items = normalOrderItemMapper.selectList(
                new LambdaQueryWrapper<NormalOrderItem>()
                        .eq(NormalOrderItem::getOrderId, orderId)
                        .orderByAsc(NormalOrderItem::getId));
        // 4. 回补商品库存与销量（按商品聚合后乐观锁回补）
        rollbackProductStock(items);
        // 5. 更新订单状态为 CANCELLED
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelTime(LocalDateTime.now());
        order.setCancelReason(CANCEL_REASON_USER);
        normalOrderMapper.updateById(order);

        log.info("普通订单取消成功，orderNo={}, userId={}, itemIdCount={}",
                order.getOrderNo(), userId, items.size());

        // 6. 取消邮件通知（异步，失败不影响主流程）
        String email = getUserEmail(userId);
        if (email != null) {
            try {
                emailService.sendOrderCancel(email, order.getOrderNo(), CANCEL_REASON_USER);
            } catch (Exception e) {
                log.error("普通订单取消邮件发送失败，orderId={}", orderId, e);
            }
        }
        return assembleDetail(order, items);
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

    // ==================== 普通订单私有辅助方法 ====================

    /**
     * 加载在售商品，不存在或已下架抛异常。
     */
    private Product loadOnSaleProduct(Long productId) {
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (product.getStatus() != ProductStatus.ON_SALE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "商品已下架");
        }
        return product;
    }

    /**
     * 校验收货地址归属当前用户。
     */
    private void checkAddressOwnership(Long userId, Long addressId) {
        UserAddress addr = userAddressMapper.selectById(addressId);
        if (addr == null) {
            throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
        }
        if (!userId.equals(addr.getUserId())) {
            throw new BusinessException(ErrorCode.ADDRESS_FORBIDDEN);
        }
    }

    /**
     * 乐观锁扣减商品库存与销量：WHERE stock >= quantity AND status=ON_SALE。
     *
     * @return 受影响行数：1=成功，0=库存不足或商品不在售
     */
    private int deductProductStock(Long productId, Integer quantity) {
        LambdaUpdateWrapper<Product> wrapper = new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .eq(Product::getStatus, ProductStatus.ON_SALE)
                .ge(Product::getStock, quantity)
                .setSql("stock = stock - " + quantity)
                .setSql("sales_count = sales_count + " + quantity);
        return productMapper.update(null, wrapper);
    }

    /**
     * 回补商品库存与销量（普通订单取消时调用）。
     * <p>
     * 按商品 ID 聚合各明细数量后，使用乐观锁更新：
     * {@code stock = stock + qty, sales_count = sales_count - qty}。
     * 仅对在售商品回补（已下架商品保留库存不变，避免污染下架态数据）。
     * 回补失败仅记录日志，不阻断取消主流程（库存以商品表为准，可由对账任务兜底）。
     *
     * @param items 订单明细列表
     */
    private void rollbackProductStock(List<NormalOrderItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        // 按商品聚合数量
        Map<Long, Integer> qtyByProduct = new HashMap<>();
        for (NormalOrderItem item : items) {
            if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                continue;
            }
            qtyByProduct.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        // 乐观锁回补：仅对在售商品生效
        for (Map.Entry<Long, Integer> e : qtyByProduct.entrySet()) {
            LambdaUpdateWrapper<Product> wrapper = new LambdaUpdateWrapper<Product>()
                    .eq(Product::getId, e.getKey())
                    .eq(Product::getStatus, ProductStatus.ON_SALE)
                    .setSql("stock = stock + " + e.getValue())
                    .setSql("sales_count = sales_count - " + e.getValue());
            int rows = productMapper.update(null, wrapper);
            if (rows == 0) {
                log.warn("回补商品库存失败（商品不存在或已下架），productId={}, qty={}",
                        e.getKey(), e.getValue());
            }
        }
    }

    /**
     * 构造普通订单基础对象（不含明细），totalAmount 由调用方后续回填或一次性传入。
     *
     * @param userId     用户 ID
     * @param addressId  收货地址 ID
     * @param remark     备注
     * @param totalAmount 商品总金额（购物车结算场景传入 ZERO 占位，后续回写）
     */
    private NormalOrder buildNormalOrder(Long userId, Long addressId, String remark, BigDecimal totalAmount) {
        NormalOrder order = new NormalOrder();
        order.setOrderNo(generateNormalOrderNo());
        order.setUserId(userId);
        order.setAddressId(addressId);
        order.setTotalAmount(totalAmount);
        order.setFreightAmount(DEFAULT_FREIGHT);
        order.setPayAmount(totalAmount.add(DEFAULT_FREIGHT));
        order.setStatus(OrderStatus.UNPAID);
        order.setPayExpireTime(LocalDateTime.now().plusMinutes(payTimeoutMinutes));
        order.setRemark(remark);
        return order;
    }

    /**
     * 构造订单明细（保留下单时刻商品快照）。
     */
    private NormalOrderItem buildOrderItem(Long orderId, Product product,
                                           BigDecimal unitPrice, Integer quantity) {
        NormalOrderItem item = new NormalOrderItem();
        item.setOrderId(orderId);
        item.setProductId(product.getId());
        item.setProductName(product.getName());
        item.setProductImage(product.getMainImage());
        item.setUnitPrice(unitPrice);
        item.setQuantity(quantity);
        item.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(quantity)));
        return item;
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
     * 组装订单详情视图。
     */
    private NormalOrderDetailVO assembleDetail(NormalOrder order, List<NormalOrderItem> items) {
        NormalOrderDetailVO vo = new NormalOrderDetailVO();
        vo.setOrder(order);
        vo.setItems(items);
        return vo;
    }

    /**
     * 普通订单号生成：NO + 时间戳 + 6位随机数字，与秒杀订单号（SK 前缀）区分。
     */
    private String generateNormalOrderNo() {
        return "NO" + LocalDateTime.now().format(ORDER_NO_FORMATTER) + randomDigits(6);
    }

    // ==================== 统一订单列表（需求1 合并秒杀+普通） ====================

    @Override
    public PageResult<OrderListItemVO> getUnifiedOrderList(Long userId, String status,
                                                            Integer pageNum, Integer pageSize) {
        int num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);

        // 1. 解析状态筛选（null 表示不筛选）
        OrderStatus statusFilter = parseStatusFromString(status);

        // 2. 查询秒杀订单（按 userId + status 过滤，按 createTime 降序）
        // H11 修复：原代码全量加载后内存分页，存在性能与内存风险。
        // 此处添加 LIMIT 兜底（最多查 pageNum*pageSize 条），避免大用户量全表加载。
        // 完整方案应改为 DB 层分页（分别按页查询秒杀/普通订单后归并），此处先做限流保护。
        int maxLoad = num * size;
        LambdaQueryWrapper<SeckillOrder> skWrapper = new LambdaQueryWrapper<SeckillOrder>()
                .eq(SeckillOrder::getUserId, userId)
                .orderByDesc(SeckillOrder::getCreateTime)
                .last("LIMIT " + maxLoad);
        if (statusFilter != null) {
            skWrapper.eq(SeckillOrder::getStatus, statusFilter);
        }
        List<SeckillOrder> seckillOrders = seckillOrderMapper.selectList(skWrapper);

        // 3. 查询普通订单（按 userId + status 过滤，按 createTime 降序）
        LambdaQueryWrapper<NormalOrder> normalWrapper = new LambdaQueryWrapper<NormalOrder>()
                .eq(NormalOrder::getUserId, userId)
                .orderByDesc(NormalOrder::getCreateTime)
                .last("LIMIT " + maxLoad);
        if (statusFilter != null) {
            normalWrapper.eq(NormalOrder::getStatus, statusFilter);
        }
        List<NormalOrder> normalOrders = normalOrderMapper.selectList(normalWrapper);

        // 4. 批量查询秒杀订单对应的商品快照（避免 N+1）
        List<Long> skProductIds = seckillOrders.stream()
                .map(SeckillOrder::getProductId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, Product> skProductMap = batchQueryProducts(skProductIds);

        // 5. 批量查询普通订单的明细（避免 N+1）
        List<Long> normalOrderIds = normalOrders.stream()
                .map(NormalOrder::getId)
                .collect(Collectors.toList());
        Map<Long, List<NormalOrderItem>> normalItemMap = batchQueryNormalItems(normalOrderIds);

        // 6. 转换为 OrderListItemVO
        List<OrderListItemVO> allList = new ArrayList<>(seckillOrders.size() + normalOrders.size());
        for (SeckillOrder sk : seckillOrders) {
            allList.add(convertSeckillOrder(sk, skProductMap.get(sk.getProductId())));
        }
        for (NormalOrder no : normalOrders) {
            allList.add(convertNormalOrder(no, normalItemMap.getOrDefault(no.getId(), java.util.Collections.emptyList())));
        }

        // 7. 合并后按 createTime 降序排序
        allList.sort(Comparator.comparing(
                OrderListItemVO::getCreateTime,
                Comparator.nullsLast(Comparator.reverseOrder())));

        // 8. 内存分页
        int total = allList.size();
        int fromIndex = Math.min((num - 1) * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<OrderListItemVO> pageList = allList.subList(fromIndex, toIndex);

        return PageResult.of(pageList, total, num, size);
    }

    /**
     * 字符串状态 → OrderStatus 枚举（不区分大小写，非法值返回 null 表示不筛选）。
     */
    private OrderStatus parseStatusFromString(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.fromCode(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 批量查询商品，返回 id → Product 映射。
     */
    private Map<Long, Product> batchQueryProducts(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>().in(Product::getId, productIds));
        return products.stream().collect(Collectors.toMap(Product::getId, p -> p));
    }

    /**
     * 批量查询普通订单明细，按 orderId 分组。
     */
    private Map<Long, List<NormalOrderItem>> batchQueryNormalItems(List<Long> orderIds) {
        if (orderIds == null || orderIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<NormalOrderItem> items = normalOrderItemMapper.selectList(
                new LambdaQueryWrapper<NormalOrderItem>()
                        .in(NormalOrderItem::getOrderId, orderIds)
                        .orderByAsc(NormalOrderItem::getId));
        return items.stream().collect(Collectors.groupingBy(NormalOrderItem::getOrderId));
    }

    /**
     * 秒杀订单 → 统一订单列表项。
     * <p>
     * 秒杀订单仅含单个商品，items 列表只有一项；商品名称/主图取自商品快照，
     * 若商品已被删除则名称/主图为 null，不影响列表展示。
     */
    private OrderListItemVO convertSeckillOrder(SeckillOrder order, Product product) {
        OrderListItemVO vo = new OrderListItemVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderType("SECKILL");
        vo.setStatus(order.getStatus() != null ? order.getStatus().getCode() : null);
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayMethod(order.getPayMethod());
        vo.setCreateTime(order.getCreateTime());
        vo.setPayTime(order.getPayTime());
        vo.setShipTime(order.getShipTime());

        OrderListItemVO.OrderItemSnapshot snapshot = new OrderListItemVO.OrderItemSnapshot();
        snapshot.setProductId(order.getProductId());
        snapshot.setProductName(product != null ? product.getName() : null);
        snapshot.setProductImage(product != null ? product.getMainImage() : null);
        snapshot.setUnitPrice(order.getSeckillPrice());
        snapshot.setQuantity(order.getQuantity());
        vo.setItems(List.of(snapshot));
        return vo;
    }

    /**
     * 普通订单 → 统一订单列表项。
     * <p>
     * items 列表来自普通订单明细，已保留下单时刻的商品快照（名称/主图/单价）。
     */
    private OrderListItemVO convertNormalOrder(NormalOrder order, List<NormalOrderItem> items) {
        OrderListItemVO vo = new OrderListItemVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderType("NORMAL");
        vo.setStatus(order.getStatus() != null ? order.getStatus().getCode() : null);
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayMethod(order.getPayMethod());
        vo.setCreateTime(order.getCreateTime());
        vo.setPayTime(order.getPayTime());
        vo.setShipTime(order.getShipTime());

        List<OrderListItemVO.OrderItemSnapshot> snapshots = new ArrayList<>(items.size());
        for (NormalOrderItem item : items) {
            OrderListItemVO.OrderItemSnapshot snapshot = new OrderListItemVO.OrderItemSnapshot();
            snapshot.setProductId(item.getProductId());
            snapshot.setProductName(item.getProductName());
            snapshot.setProductImage(item.getProductImage());
            snapshot.setUnitPrice(item.getUnitPrice());
            snapshot.setQuantity(item.getQuantity());
            snapshots.add(snapshot);
        }
        vo.setItems(snapshots);
        return vo;
    }
}
