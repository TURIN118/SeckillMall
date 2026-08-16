package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.PageResult;
import com.seckill.mall.config.RabbitMQConfig;
import com.seckill.mall.entity.Cart;
import com.seckill.mall.entity.NormalOrder;
import com.seckill.mall.entity.NormalOrderItem;
import com.seckill.mall.entity.Product;
import com.seckill.mall.entity.ProductSku;
import com.seckill.mall.entity.SeckillOrder;
import com.seckill.mall.entity.UserAddress;
import com.seckill.mall.entity.enums.OrderStatus;
import com.seckill.mall.entity.enums.ProductStatus;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.mapper.NormalOrderItemMapper;
import com.seckill.mall.mapper.NormalOrderMapper;
import com.seckill.mall.mq.message.OrderDelayMessage;
import com.seckill.mall.service.CartService;
import com.seckill.mall.service.CouponService;
import com.seckill.mall.service.EmailService;
import com.seckill.mall.service.InventoryService;
import com.seckill.mall.service.OrderService;
import com.seckill.mall.service.PaymentService;
import com.seckill.mall.service.ProductService;
import com.seckill.mall.service.ProductSkuService;
import com.seckill.mall.service.SeckillOrderService;
import com.seckill.mall.service.UserAddressService;
import com.seckill.mall.service.UserService;
import com.seckill.mall.shared.kernel.port.MessageBusPort;
import com.seckill.mall.vo.NormalOrderDetailVO;
import com.seckill.mall.vo.NormalOrderItemVO;
import com.seckill.mall.vo.NormalOrderVO;
import com.seckill.mall.vo.OrderListItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * 普通订单领域服务实现（Phase 4b-1 从原 OrderServiceImpl 沿自然缝拆分而来）。
 * <p>
 * 仅承载普通订单相关用例 + 统一订单列表（秒杀+普通合并展示）+ 跨类型逻辑删除。
 * 秒杀订单相关用例已迁移至 {@link SeckillOrderServiceImpl}。
 * 业务逻辑与原 OrderServiceImpl 完全一致，仅做机械移动。
 * <p>
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

    /** 普通订单默认运费（当前简化为 0，后续可按地址/重量扩展） */
    private static final BigDecimal DEFAULT_FREIGHT = BigDecimal.ZERO;

    private final UserService userService;
    private final EmailService emailService;
    private final NormalOrderMapper normalOrderMapper;
    private final NormalOrderItemMapper normalOrderItemMapper;
    private final MessageBusPort messageBusPort;
    private final ProductSkuService productSkuService;
    // Phase 6：消除跨模块 Mapper 依赖，改用 ProductService 查询商品
    private final ProductService productService;
    private final ObjectMapper objectMapper;
    // 优惠券服务：普通订单创建/支付/取消时接入优惠券核销与回退
    private final CouponService couponService;
    // Phase 4b-2：支付扣款逻辑抽取至 PaymentService（钱包扣款 + 模拟支付）
    private final PaymentService paymentService;
    // Phase 4b-3：商品库存扣减/回补抽取至 InventoryService
    private final InventoryService inventoryService;
    // Phase 7：消除剩余跨模块 Mapper 依赖，改用对应领域 Service 内部调用入口
    private final SeckillOrderService seckillOrderService;
    private final CartService cartService;
    private final UserAddressService userAddressService;

    @Value("${seckill.pay-timeout-minutes:15}")
    private long payTimeoutMinutes;

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
                couponService.revertCoupon(order.getUserCouponId(), order.getUserId());
            } catch (Exception e) {
                log.error("超时取消订单后优惠券回退失败，orderId={}, userCouponId={}", orderId, order.getUserCouponId(), e);
            }
        }

        log.info("普通订单超时取消成功，orderNo={}, orderId={}", order.getOrderNo(), orderId);

        // 超时取消邮件：异步发送，失败不影响主流程
        String email = userService.getEmail(order.getUserId());
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
            messageBusPort.publish(
                    RabbitMQConfig.ORDER_DELAY_EXCHANGE,
                    RabbitMQConfig.ORDER_DELAY_ROUTING_KEY,
                    delay);
            log.info("普通订单延迟消息已发送，orderNo={}, orderId={}", order.getOrderNo(), order.getId());
        } catch (Exception e) {
            log.error("普通订单延迟消息发送失败，orderNo={}，依赖补偿任务兜底", order.getOrderNo(), e);
        }
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
    public NormalOrderDetailVO createNormalOrder(Long userId, Long productId, Long skuId, Integer quantity,
                                                 Long addressId, String remark, Long userCouponId) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "购买数量必须大于0");
        }
        // 1. 校验商品状态
        Product product = loadOnSaleProduct(productId);
        // 2. 校验收货地址归属
        checkAddressOwnership(userId, addressId);

        // 5.7.3：SKU 库存扣减 / 价格 / 属性快照
        Long effectiveSkuId = (skuId == null || skuId == 0L) ? 0L : skuId;
        BigDecimal unitPrice;
        String skuAttributes;
        if (effectiveSkuId != 0L) {
            ProductSku sku = productSkuService.getByIdEnabled(effectiveSkuId);
            if (sku == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "SKU 不存在或已禁用");
            }
            if (!sku.getProductId().equals(productId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "SKU 不属于该商品");
            }
            if (sku.getStock() == null || sku.getStock() < quantity) {
                throw new BusinessException(ErrorCode.STOCK_EMPTY);
            }
            // 扣减 SKU 库存（乐观锁）
            // Phase 8：统一通过 InventoryService 入口操作 SKU 库存
            if (!inventoryService.deductSkuStock(effectiveSkuId, quantity)) {
                throw new BusinessException(ErrorCode.STOCK_EMPTY);
            }
            unitPrice = sku.getPrice();
            skuAttributes = convertAttributesToReadable(sku.getAttributes());
        } else {
            // 无规格商品：扣减 t_product.stock
            if (product.getStock() == null || product.getStock() < quantity) {
                throw new BusinessException(ErrorCode.STOCK_EMPTY);
            }
            int updated = inventoryService.deductProductStock(productId, quantity);
            if (updated == 0) {
                throw new BusinessException(ErrorCode.STOCK_EMPTY);
            }
            unitPrice = product.getOriginalPrice();
            skuAttributes = null;
        }

        // 3. 建普通订单 + 明细
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        NormalOrder order = buildNormalOrder(userId, addressId, remark, subtotal);
        // 优惠券：计算优惠金额并抵扣实付金额
        applyCouponToOrder(order, userCouponId, userId, subtotal, List.of(productId));
        normalOrderMapper.insert(order);

        NormalOrderItem item = buildOrderItem(order.getId(), product, unitPrice, quantity);
        item.setSkuId(effectiveSkuId);
        item.setSkuAttributes(skuAttributes);
        normalOrderItemMapper.insert(item);

        log.info("立即购买创建普通订单成功，orderNo={}, userId={}, productId={}, skuId={}, quantity={}, userCouponId={}, discount={}",
                order.getOrderNo(), userId, productId, effectiveSkuId, quantity, userCouponId, order.getDiscountAmount());
        // Bug1修复：创建普通订单后发送延迟消息，超时自动取消
        sendNormalOrderDelayMessage(order);
        return assembleDetail(order, List.of(item));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NormalOrderDetailVO createOrderFromCart(Long userId, Long addressId,
                                                   List<Long> cartIds, String remark, Long userCouponId) {
        if (cartIds == null || cartIds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "待结算购物车项不能为空");
        }
        // 1. 校验地址归属
        checkAddressOwnership(userId, addressId);
        // 2. 查询购物车项并校验归属
        // L9: 若 cartIds 含重复 ID，selectList(in) 会去重返回，导致 carts.size() != cartIds.size() 误报错
        // 完整方案应在入口去重：cartIds = cartIds.stream().distinct().toList()
        // Phase 7：改用 CartService 跨模块内部调用，消除 CartMapper 依赖
        List<Cart> carts = cartService.getCartsByIds(cartIds);
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
        List<Product> products = productService.getProductsByIds(productIds);
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        // 3.1 批量查询 SKU 信息（5.7.3）
        List<Long> skuIds = carts.stream()
                .map(Cart::getSkuId)
                .filter(id -> id != null && id != 0L)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, ProductSku> skuMap = batchQuerySkus(skuIds);
        for (Cart c : carts) {
            Product p = productMap.get(c.getProductId());
            if (p == null) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            if (p.getStatus() != ProductStatus.ON_SALE) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "商品[" + p.getName() + "]已下架");
            }
            // 5.7.3：校验 SKU 库存或商品库存
            Long cSkuId = c.getSkuId();
            if (cSkuId != null && cSkuId != 0L) {
                ProductSku sku = skuMap.get(cSkuId);
                if (sku == null) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "SKU 不存在或已禁用");
                }
                if (sku.getStock() == null || sku.getStock() < c.getQuantity()) {
                    throw new BusinessException(ErrorCode.STOCK_EMPTY);
                }
            } else {
                if (p.getStock() == null || p.getStock() < c.getQuantity()) {
                    throw new BusinessException(ErrorCode.STOCK_EMPTY);
                }
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
            Long cSkuId = c.getSkuId();
            BigDecimal unitPrice;
            String skuAttributes;
            if (cSkuId != null && cSkuId != 0L) {
                ProductSku sku = skuMap.get(cSkuId);
                unitPrice = sku.getPrice();
                skuAttributes = convertAttributesToReadable(sku.getAttributes());
            } else {
                unitPrice = p.getOriginalPrice();
                skuAttributes = null;
            }
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(c.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            NormalOrderItem item = buildOrderItem(order.getId(), p, unitPrice, c.getQuantity());
            item.setSkuId(cSkuId != null ? cSkuId : 0L);
            item.setSkuAttributes(skuAttributes);
            normalOrderItemMapper.insert(item);
            items.add(item);
        }
        // 5. 回写订单金额
        order.setTotalAmount(totalAmount);
        order.setFreightAmount(DEFAULT_FREIGHT);
        order.setPayAmount(totalAmount.add(DEFAULT_FREIGHT));
        // 优惠券：计算优惠金额并抵扣实付金额
        List<Long> orderProductIds = carts.stream().map(Cart::getProductId).distinct().collect(Collectors.toList());
        applyCouponToOrder(order, userCouponId, userId, totalAmount, orderProductIds);
        normalOrderMapper.updateById(order);

        // 6. 扣库存（5.7.3：按 SKU 或商品聚合后乐观锁扣减）
        // 6.1 扣减 SKU 库存
        Map<Long, Integer> qtyBySku = new HashMap<>();
        for (Cart c : carts) {
            if (c.getSkuId() != null && c.getSkuId() != 0L) {
                qtyBySku.merge(c.getSkuId(), c.getQuantity(), Integer::sum);
            }
        }
        for (Map.Entry<Long, Integer> e : qtyBySku.entrySet()) {
            // Phase 8：统一通过 InventoryService 入口操作 SKU 库存
            if (!inventoryService.deductSkuStock(e.getKey(), e.getValue())) {
                // 并发库存不足，事务回滚
                throw new BusinessException(ErrorCode.STOCK_EMPTY);
            }
        }
        // 6.2 扣减无规格商品库存
        Map<Long, Integer> qtyByProduct = new HashMap<>();
        for (Cart c : carts) {
            if (c.getSkuId() == null || c.getSkuId() == 0L) {
                qtyByProduct.merge(c.getProductId(), c.getQuantity(), Integer::sum);
            }
        }
        for (Map.Entry<Long, Integer> e : qtyByProduct.entrySet()) {
            int updated = inventoryService.deductProductStock(e.getKey(), e.getValue());
            if (updated == 0) {
                // 并发库存不足，事务回滚
                throw new BusinessException(ErrorCode.STOCK_EMPTY);
            }
        }
        // 7. 删除已结算购物车项（逻辑删除）
        // Phase 7：改用 CartService 跨模块内部调用，消除 CartMapper 依赖
        cartService.deleteCartsByIds(cartIds);

        log.info("购物车结算创建普通订单成功，orderNo={}, userId={}, cartIds={}, itemCount={}, userCouponId={}, discount={}",
                order.getOrderNo(), userId, cartIds, carts.size(), userCouponId, order.getDiscountAmount());
        // Bug1修复：创建普通订单后发送延迟消息，超时自动取消
        sendNormalOrderDelayMessage(order);
        return assembleDetail(order, items);
    }

    /**
     * 批量查询 SKU，返回 id → ProductSku 映射。
     */
    private Map<Long, ProductSku> batchQuerySkus(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return new HashMap<>();
        }
        List<ProductSku> skus = skuIds.stream()
                .map(productSkuService::getByIdEnabled)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        return skus.stream().collect(Collectors.toMap(ProductSku::getId, s -> s));
    }

    /**
     * 5.7.3：将 SKU attributes JSON 转可读字符串
     * 如 {"颜色":"曜石黑","版本":"旗舰版"} → "颜色: 曜石黑 / 版本: 旗舰版"
     */
    private String convertAttributesToReadable(String attributesJson) {
        if (attributesJson == null || attributesJson.isEmpty()) {
            return null;
        }
        try {
            Map<String, String> map = objectMapper.readValue(attributesJson,
                    new TypeReference<Map<String, String>>() {});
            return map.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue())
                    .collect(Collectors.joining(" / "));
        } catch (Exception e) {
            log.warn("转换 SKU 属性 JSON 失败: {}", attributesJson, e);
            return attributesJson;
        }
    }

    @Override
    public NormalOrderDetailVO getNormalOrderDetail(Long userId, Long orderId) {
        NormalOrder order = loadAndCheckNormalOrderOwnership(userId, orderId);
        // 超时检查：UNPAID 且已过支付截止时间，自动更新为 TIMEOUT
        checkAndHandleNormalOrderTimeout(order);
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
                couponService.useCoupon(order.getUserCouponId(), userId, orderId);
            } catch (Exception e) {
                // 核销失败不阻断支付主流程（支付已完成），仅记录日志由对账任务兜底
                log.error("支付成功后优惠券核销失败，orderId={}, userCouponId={}", orderId, order.getUserCouponId(), e);
            }
        }

        // 邮件通知（异步，失败不影响主流程）
        String email = userService.getEmail(userId);
        if (email != null) {
            try {
                emailService.sendPaySuccess(email, order.getOrderNo(), order.getPayAmount(),
                        LocalDateTime.now().format(PAY_TIME_FORMATTER));
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
                couponService.revertCoupon(order.getUserCouponId(), userId);
            } catch (Exception e) {
                // 回退失败不阻断取消主流程，仅记录日志由对账任务兜底
                log.error("取消订单后优惠券回退失败，orderId={}, userCouponId={}", orderId, order.getUserCouponId(), e);
            }
        }

        log.info("普通订单取消成功，orderNo={}, userId={}, itemIdCount={}",
                order.getOrderNo(), userId, items.size());

        // 7. 取消邮件通知（异步，失败不影响主流程）
        String email = userService.getEmail(userId);
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

    // ==================== 普通订单私有辅助方法 ====================

    /**
     * 加载在售商品，不存在或已下架抛异常。
     */
    private Product loadOnSaleProduct(Long productId) {
        Product product = productService.getProductById(productId);
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
        // Phase 7：改用 UserAddressService 跨模块内部调用，消除 UserAddressMapper 依赖
        UserAddress addr = userAddressService.getAddressById(addressId);
        if (addr == null) {
            throw new BusinessException(ErrorCode.ADDRESS_NOT_FOUND);
        }
        if (!userId.equals(addr.getUserId())) {
            throw new BusinessException(ErrorCode.ADDRESS_FORBIDDEN);
        }
    }

    /**
     * 5.7.3：回补库存（SKU 库存或商品库存），用于订单取消 / 超时。
     * <p>
     * 遍历订单明细，对每项：
     * <ul>
     *   <li>skuId != null && skuId != 0：调用 {@code inventoryService.rollbackSkuStock} 回补 SKU 库存，
     *       并刷新 t_product.total_stock 冗余字段（建议3）</li>
     *   <li>skuId == null || skuId == 0：委托 {@link InventoryService#rollbackProductStock} 回补 t_product.stock 与 sales_count</li>
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
            // Phase 8：统一通过 InventoryService 入口回补 SKU 库存
            inventoryService.rollbackSkuStock(e.getKey(), e.getValue());
            // 建议3：同步刷新 t_product.total_stock
            Long productId = skuToProduct.get(e.getKey());
            if (productId != null) {
                productSkuService.refreshTotalStock(productId);
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
            inventoryService.rollbackProductStock(e.getKey(), e.getValue());
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
        // 优惠券默认值：未使用
        order.setDiscountAmount(BigDecimal.ZERO);
        return order;
    }

    /**
     * 优惠券抵扣：计算优惠金额并更新订单的 userCouponId / discountAmount / payAmount。
     * <p>
     * 若 userCouponId 为空，discountAmount 置 0，payAmount 不变。
     * 若 userCouponId 非空，调用 {@code couponService.calculateDiscount} 计算优惠金额，
     * 实付金额 = 商品总额 + 运费 - 优惠金额。
     * <p>
     * 当前简化处理：传入的 orderAmount 为适用商品小计（前端按 scope 预计算），
     * 对于立即购买场景即为商品小计，对于购物车结算场景为订单总额。
     *
     * @param order        普通订单（已设置 totalAmount / freightAmount / payAmount）
     * @param userCouponId 用户优惠券ID（可空）
     * @param userId       用户ID
     * @param orderAmount  适用商品小计（参与优惠的金额）
     * @param productIds   订单商品ID列表（预留）
     */
    private void applyCouponToOrder(NormalOrder order, Long userCouponId, Long userId,
                                    BigDecimal orderAmount, List<Long> productIds) {
        if (userCouponId == null) {
            order.setUserCouponId(null);
            order.setDiscountAmount(BigDecimal.ZERO);
            // payAmount 已在 buildNormalOrder 中设置为 totalAmount + freight
            return;
        }
        BigDecimal discount = couponService.calculateDiscount(userCouponId, userId, orderAmount, productIds);
        order.setUserCouponId(userCouponId);
        order.setDiscountAmount(discount);
        // 实付金额 = 商品总额 + 运费 - 优惠金额
        BigDecimal payAmount = order.getTotalAmount()
                .add(order.getFreightAmount() == null ? BigDecimal.ZERO : order.getFreightAmount())
                .subtract(discount);
        // 实付金额不能为负
        if (payAmount.compareTo(BigDecimal.ZERO) < 0) {
            payAmount = BigDecimal.ZERO;
        }
        order.setPayAmount(payAmount);
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

    /**
     * 普通订单号生成：NO + 时间戳 + 6位随机数字，与秒杀订单号（SK 前缀）区分。
     */
    private String generateNormalOrderNo() {
        return "NO" + LocalDateTime.now().format(ORDER_NO_FORMATTER) + randomDigits(6);
    }

    /**
     * 检查普通订单超时：如果状态为UNPAID且已过支付截止时间，更新为TIMEOUT。
     * 兜底RabbitMQ延迟消息可能丢失的场景。
     */
    private void checkAndHandleNormalOrderTimeout(NormalOrder order) {
        if (order.getStatus() == OrderStatus.UNPAID
                && order.getPayExpireTime() != null
                && LocalDateTime.now().isAfter(order.getPayExpireTime())) {
            int rows = normalOrderMapper.update(null, new LambdaUpdateWrapper<NormalOrder>()
                    .eq(NormalOrder::getId, order.getId())
                    .eq(NormalOrder::getStatus, OrderStatus.UNPAID)
                    .set(NormalOrder::getStatus, OrderStatus.TIMEOUT)
                    .set(NormalOrder::getCancelTime, LocalDateTime.now())
                    .set(NormalOrder::getCancelReason, "支付超时自动取消"));
            if (rows > 0) {
                order.setStatus(OrderStatus.TIMEOUT);
                order.setCancelTime(LocalDateTime.now());
                order.setCancelReason("支付超时自动取消");
                log.info("普通订单超时自动关闭, orderId={}", order.getId());
            }
        }
    }

    // ==================== 统一订单列表（需求1 合并秒杀+普通） ====================

    @Override
    public PageResult<OrderListItemVO> getUnifiedOrderList(Long userId, String status, String orderType,
                                                            Integer pageNum, Integer pageSize) {
        int num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 50);

        // 1. 解析状态筛选（null 表示不筛选）
        OrderStatus statusFilter = parseStatusFromString(status);
        // 1.1 解析订单类型筛选（null/空 表示不筛选，仅查询对应类型；NORMAL-仅普通订单，SECKILL-仅秒杀订单）
        String typeFilter = parseOrderType(orderType);

        // 2. 查询秒杀订单（按 userId + status 过滤，按 createTime 降序）
        // H11 修复：原代码全量加载后内存分页，存在性能与内存风险。
        // 此处添加 LIMIT 兜底（最多查 pageNum*pageSize 条），避免大用户量全表加载。
        // 完整方案应改为 DB 层分页（分别按页查询秒杀/普通订单后归并），此处先做限流保护。
        int maxLoad = num * size;
        List<SeckillOrder> seckillOrders = java.util.Collections.emptyList();
        if (typeFilter == null || "SECKILL".equals(typeFilter)) {
            // Phase 7：改用 SeckillOrderService 跨模块内部调用，消除 SeckillOrderMapper 依赖
            // 原本在此处构造 LambdaQueryWrapper，现已下沉至 SeckillOrderServiceImpl#getSeckillOrdersForUnifiedList
            seckillOrders = seckillOrderService.getSeckillOrdersForUnifiedList(userId, toStatusInt(statusFilter), maxLoad);
        }

        // 3. 查询普通订单（按 userId + status 过滤，按 createTime 降序）
        List<NormalOrder> normalOrders = java.util.Collections.emptyList();
        if (typeFilter == null || "NORMAL".equals(typeFilter)) {
            LambdaQueryWrapper<NormalOrder> normalWrapper = new LambdaQueryWrapper<NormalOrder>()
                    .eq(NormalOrder::getUserId, userId)
                    .orderByDesc(NormalOrder::getCreateTime)
                    .last("LIMIT " + maxLoad);
            if (statusFilter != null) {
                normalWrapper.eq(NormalOrder::getStatus, statusFilter);
            }
            normalOrders = normalOrderMapper.selectList(normalWrapper);
        }

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
     * Phase 7：OrderStatus 枚举 → Integer 序号（与 SeckillOrderService#getSeckillOrdersForUnifiedList 契约对齐）。
     * <p>
     * 序号映射：0=UNPAID 1=PAID 2=SHIPPED 3=CANCELLED 4=TIMEOUT 5=COMPLETED，
     * 与 {@link SeckillOrderServiceImpl#parseStatus(Integer)} 互为逆函数。
     * CANCELLING 为中间状态，不应作为列表筛选条件，返回 null 表示不筛选。
     *
     * @param status 订单状态枚举，null 返回 null
     * @return 状态序号
     */
    private Integer toStatusInt(OrderStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case UNPAID -> 0;
            case PAID -> 1;
            case SHIPPED -> 2;
            case CANCELLED -> 3;
            case TIMEOUT -> 4;
            case COMPLETED -> 5;
            case CANCELLING -> null;
        };
    }

    /**
     * 解析订单类型参数（不区分大小写，非法值返回 null 表示不筛选）。
     *
     * @param orderType 订单类型字符串
     * @return "NORMAL" / "SECKILL" / null（不筛选）
     */
    private String parseOrderType(String orderType) {
        if (orderType == null || orderType.isBlank()) {
            return null;
        }
        String upper = orderType.toUpperCase();
        if ("NORMAL".equals(upper) || "SECKILL".equals(upper)) {
            return upper;
        }
        return null;
    }

    /**
     * 批量查询商品，返回 id → Product 映射。
     */
    private Map<Long, Product> batchQueryProducts(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<Product> products = productService.getProductsByIds(productIds);
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

    /**
     * 逻辑删除订单（需求：订单逻辑删除+类型筛选）。
     * <p>
     * 自动识别秒杀订单与普通订单：先查秒杀订单表，未命中再查普通订单表。
     * 仅允许 COMPLETED 或 CANCELLED 状态的订单逻辑删除，其他状态抛 {@code ORDER_DELETE_FAILED}。
     * 利用 MyBatis-Plus {@code @TableLogic} 注解，调用 deleteById/updateById 即可自动逻辑删除。
     *
     * @param orderId 订单 ID
     * @param userId  当前操作用户 ID
     * @return true 表示删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteOrder(Long orderId, Long userId) {
        if (orderId == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "订单ID/用户ID不能为空");
        }

        // 1. 先尝试秒杀订单表
        // Phase 7：改用 SeckillOrderService 跨模块内部调用，消除 SeckillOrderMapper 依赖
        SeckillOrder seckillOrder = seckillOrderService.getSeckillOrderById(orderId);
        if (seckillOrder != null) {
            // 校验归属
            if (!userId.equals(seckillOrder.getUserId())) {
                throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
            }
            // 校验状态：仅 COMPLETED / CANCELLED 可删除
            checkOrderDeletable(seckillOrder.getStatus());
            // 逻辑删除：@TableLogic 注解使 deleteById 自动 set is_deleted=1
            log.info("逻辑删除秒杀订单成功 orderId={} orderNo={} userId={}", orderId, seckillOrder.getOrderNo(), userId);
            return seckillOrderService.logicalDeleteSeckillOrder(orderId);
        }

        // 2. 秒杀订单表未命中，尝试普通订单表
        NormalOrder normalOrder = normalOrderMapper.selectById(orderId);
        if (normalOrder != null) {
            // 校验归属
            if (!userId.equals(normalOrder.getUserId())) {
                throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
            }
            // 校验状态：仅 COMPLETED / CANCELLED 可删除
            checkOrderDeletable(normalOrder.getStatus());
            // 逻辑删除：@TableLogic 注解使 deleteById 自动 set is_deleted=1
            int rows = normalOrderMapper.deleteById(orderId);
            log.info("逻辑删除普通订单成功 orderId={} orderNo={} userId={}", orderId, normalOrder.getOrderNo(), userId);
            return rows > 0;
        }

        // 3. 两张表都未命中，订单不存在
        throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
    }

    /**
     * 校验订单状态是否允许逻辑删除。
     * <p>
     * 仅 COMPLETED（已完成）与 CANCELLED（已取消）状态允许删除，
     * 其他状态（UNPAID/PAID/SHIPPED/TIMEOUT/CANCELLING）抛 {@code ORDER_DELETE_FAILED}。
     *
     * @param status 订单状态
     */
    private void checkOrderDeletable(OrderStatus status) {
        if (status != OrderStatus.COMPLETED && status != OrderStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ORDER_DELETE_FAILED);
        }
    }

    @Override
    public boolean hasUserPurchasedProduct(Long userId, Long productId, Long skuId) {
        // 查询该商品的订单明细
        List<NormalOrderItem> items = normalOrderItemMapper.selectList(
                new LambdaQueryWrapper<NormalOrderItem>()
                        .eq(NormalOrderItem::getProductId, productId)
                        .eq(skuId != 0L, NormalOrderItem::getSkuId, skuId));
        if (items.isEmpty()) {
            return false;
        }
        // 收集订单 ID，查询订单状态是否为 PAID / SHIPPED / COMPLETED
        List<Long> orderIds = items.stream()
                .map(NormalOrderItem::getOrderId)
                .distinct()
                .collect(Collectors.toList());
        List<NormalOrder> orders = normalOrderMapper.selectList(
                new LambdaQueryWrapper<NormalOrder>()
                        .eq(NormalOrder::getUserId, userId)
                        .in(NormalOrder::getId, orderIds)
                        .in(NormalOrder::getStatus, OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.COMPLETED));
        return !orders.isEmpty();
    }

    @Override
    public List<NormalOrder> getWalletPaidOrdersByUser(Long userId) {
        return normalOrderMapper.selectList(new LambdaQueryWrapper<NormalOrder>()
                .eq(NormalOrder::getUserId, userId)
                .eq(NormalOrder::getPayMethod, "WALLET")
                .in(NormalOrder::getStatus, OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.COMPLETED));
    }
}
