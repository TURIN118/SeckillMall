package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.config.RabbitMQConfig;
import com.seckill.mall.cart.api.CartApi;
import com.seckill.mall.cart.api.dto.CartItemDTO;
import com.seckill.mall.order.infrastructure.persistence.entity.NormalOrder;
import com.seckill.mall.order.infrastructure.persistence.entity.NormalOrderItem;
import com.seckill.mall.product.api.InventoryApi;
import com.seckill.mall.product.api.ProductApi;
import com.seckill.mall.product.api.SkuApi;
import com.seckill.mall.product.api.command.DeductStockCommand;
import com.seckill.mall.product.api.dto.ProductSnapshot;
import com.seckill.mall.product.api.dto.SkuSnapshot;
import com.seckill.mall.identity.infrastructure.entity.UserAddress;
import com.seckill.mall.order.infrastructure.persistence.entity.OrderStatus;
import com.seckill.mall.product.domain.ProductStatus;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.order.infrastructure.persistence.mapper.NormalOrderItemMapper;
import com.seckill.mall.order.infrastructure.persistence.mapper.NormalOrderMapper;
import com.seckill.mall.mq.message.OrderDelayMessage;

import com.seckill.mall.coupon.api.CouponUsageApi;
import com.seckill.mall.service.OrderService;
import com.seckill.mall.service.UserAddressService;
import com.seckill.mall.shared.kernel.port.MessageBusPort;
import com.seckill.mall.vo.NormalOrderDetailVO;
import com.seckill.mall.vo.NormalOrderItemVO;
import com.seckill.mall.vo.NormalOrderVO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 普通订单领域服务实现（Phase P1-1 沿职责缝拆分而来）。
 * <p>
 * 仅承载普通订单创建用例（立即购买 / 购物车结算）+ 购买校验 / 钱包支付订单查询。
 * 查询用例见 {@link OrderQueryServiceImpl}，状态流转用例见 {@link OrderLifecycleServiceImpl}。
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

    /** 普通订单默认运费（当前简化为 0，后续可按地址/重量扩展） */
    private static final BigDecimal DEFAULT_FREIGHT = BigDecimal.ZERO;

    private final NormalOrderMapper normalOrderMapper;
    private final NormalOrderItemMapper normalOrderItemMapper;
    private final MessageBusPort messageBusPort;
    private final SkuApi skuApi;
    // Phase P.5：消除跨模块 Service 依赖，改用 ProductApi 查询商品快照
    private final ProductApi productApi;
    private final ObjectMapper objectMapper;
    // 优惠券核销 API：普通订单创建时接入优惠券抵扣（Phase CP.5 从 CouponUsageService 切换）
    private final CouponUsageApi couponUsageApi;
    // Phase P.5：商品库存扣减/回补改用 InventoryApi
    private final InventoryApi inventoryApi;
    // Phase C.5：消除跨模块 CartService/Cart entity 依赖，改用 CartApi + CartItemDTO
    private final CartApi cartApi;
    private final UserAddressService userAddressService;

    @Value("${seckill.pay-timeout-minutes:15}")
    private long payTimeoutMinutes;

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
        ProductSnapshot product = loadOnSaleProduct(productId);
        // 2. 校验收货地址归属
        checkAddressOwnership(userId, addressId);

        // 5.7.3：SKU 库存扣减 / 价格 / 属性快照
        Long effectiveSkuId = (skuId == null || skuId == 0L) ? 0L : skuId;
        BigDecimal unitPrice;
        String skuAttributes;
        if (effectiveSkuId != 0L) {
            SkuSnapshot sku = skuApi.getSkuById(effectiveSkuId);
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
            // Phase P.5：统一通过 InventoryApi 入口操作 SKU 库存
            if (!inventoryApi.deductSkuStock(new DeductStockCommand(null, effectiveSkuId, quantity))) {
                throw new BusinessException(ErrorCode.STOCK_EMPTY);
            }
            unitPrice = sku.getPrice();
            skuAttributes = convertAttributesToReadable(sku.getAttributes());
        } else {
            // 无规格商品：扣减 t_product.stock
            if (product.getStock() == null || product.getStock() < quantity) {
                throw new BusinessException(ErrorCode.STOCK_EMPTY);
            }
            int updated = inventoryApi.deductProductStock(new DeductStockCommand(productId, null, quantity));
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
        // Phase C.5：改用 CartApi 跨模块调用，消除 Cart entity 依赖
        List<CartItemDTO> carts = cartApi.getCartItemsByIds(cartIds);
        if (carts.size() != cartIds.size()) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        for (CartItemDTO c : carts) {
            if (!userId.equals(c.getUserId())) {
                throw new BusinessException(ErrorCode.CART_ITEM_FORBIDDEN);
            }
        }
        // 3. 批量查询商品并校验在售 + 库存
        List<Long> productIds = carts.stream().map(CartItemDTO::getProductId).distinct().collect(Collectors.toList());
        List<ProductSnapshot> products = productApi.getProductsByIds(productIds);
        Map<Long, ProductSnapshot> productMap = products.stream()
                .collect(Collectors.toMap(ProductSnapshot::getId, p -> p));
        // 3.1 批量查询 SKU 信息（5.7.3）
        List<Long> skuIds = carts.stream()
                .map(CartItemDTO::getSkuId)
                .filter(id -> id != null && id != 0L)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, SkuSnapshot> skuMap = batchQuerySkus(skuIds);
        for (CartItemDTO c : carts) {
            ProductSnapshot p = productMap.get(c.getProductId());
            if (p == null) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            if (!ProductStatus.ON_SALE.getCode().equals(p.getStatus())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "商品[" + p.getName() + "]已下架");
            }
            // 5.7.3：校验 SKU 库存或商品库存
            Long cSkuId = c.getSkuId();
            if (cSkuId != null && cSkuId != 0L) {
                SkuSnapshot sku = skuMap.get(cSkuId);
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

        for (CartItemDTO c : carts) {
            ProductSnapshot p = productMap.get(c.getProductId());
            Long cSkuId = c.getSkuId();
            BigDecimal unitPrice;
            String skuAttributes;
            if (cSkuId != null && cSkuId != 0L) {
                SkuSnapshot sku = skuMap.get(cSkuId);
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
        List<Long> orderProductIds = carts.stream().map(CartItemDTO::getProductId).distinct().collect(Collectors.toList());
        applyCouponToOrder(order, userCouponId, userId, totalAmount, orderProductIds);
        normalOrderMapper.updateById(order);

        // 6. 扣库存（5.7.3：按 SKU 或商品聚合后乐观锁扣减）
        // 6.1 扣减 SKU 库存
        Map<Long, Integer> qtyBySku = new HashMap<>();
        for (CartItemDTO c : carts) {
            if (c.getSkuId() != null && c.getSkuId() != 0L) {
                qtyBySku.merge(c.getSkuId(), c.getQuantity(), Integer::sum);
            }
        }
        for (Map.Entry<Long, Integer> e : qtyBySku.entrySet()) {
            // Phase P.5：统一通过 InventoryApi 入口操作 SKU 库存
            if (!inventoryApi.deductSkuStock(new DeductStockCommand(null, e.getKey(), e.getValue()))) {
                // 并发库存不足，事务回滚
                throw new BusinessException(ErrorCode.STOCK_EMPTY);
            }
        }
        // 6.2 扣减无规格商品库存
        Map<Long, Integer> qtyByProduct = new HashMap<>();
        for (CartItemDTO c : carts) {
            if (c.getSkuId() == null || c.getSkuId() == 0L) {
                qtyByProduct.merge(c.getProductId(), c.getQuantity(), Integer::sum);
            }
        }
        for (Map.Entry<Long, Integer> e : qtyByProduct.entrySet()) {
            int updated = inventoryApi.deductProductStock(new DeductStockCommand(e.getKey(), null, e.getValue()));
            if (updated == 0) {
                // 并发库存不足，事务回滚
                throw new BusinessException(ErrorCode.STOCK_EMPTY);
            }
        }
        // 7. 删除已结算购物车项（逻辑删除）
        // Phase C.5：改用 CartApi 跨模块调用
        cartApi.deleteCartItemsByIds(cartIds);

        log.info("购物车结算创建普通订单成功，orderNo={}, userId={}, cartIds={}, itemCount={}, userCouponId={}, discount={}",
                order.getOrderNo(), userId, cartIds, carts.size(), userCouponId, order.getDiscountAmount());
        // Bug1修复：创建普通订单后发送延迟消息，超时自动取消
        sendNormalOrderDelayMessage(order);
        return assembleDetail(order, items);
    }

    /**
     * 批量查询 SKU，返回 id → SkuSnapshot 映射。
     */
    private Map<Long, SkuSnapshot> batchQuerySkus(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return new HashMap<>();
        }
        List<SkuSnapshot> skus = skuIds.stream()
                .map(skuApi::getSkuById)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        return skus.stream().collect(Collectors.toMap(SkuSnapshot::getId, s -> s));
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


    // ==================== 普通订单私有辅助方法 ====================

    /**
     * 加载在售商品，不存在或已下架抛异常。
     */
    private ProductSnapshot loadOnSaleProduct(Long productId) {
        ProductSnapshot product = productApi.getProductById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        if (!ProductStatus.ON_SALE.getCode().equals(product.getStatus())) {
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
     * 若 userCouponId 非空，调用 {@code couponUsageService.calculateDiscount} 计算优惠金额，
     * 实付金额 = 商品总额 + 运费 - 优惠金额（不能为负）。
     */
    private void applyCouponToOrder(NormalOrder order, Long userCouponId, Long userId,
                                    BigDecimal orderAmount, List<Long> productIds) {
        if (userCouponId == null) {
            order.setUserCouponId(null);
            order.setDiscountAmount(BigDecimal.ZERO);
            // payAmount 已在 buildNormalOrder 中设置为 totalAmount + freight
            return;
        }
        BigDecimal discount = couponUsageApi.calculateDiscount(
                com.seckill.mall.coupon.api.command.UseCouponCommand.builder()
                        .userCouponId(userCouponId)
                        .userId(userId)
                        .orderAmount(orderAmount)
                        .productIds(productIds)
                        .build())
                .getDiscount();
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
    private NormalOrderItem buildOrderItem(Long orderId, ProductSnapshot product,
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
