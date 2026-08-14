package com.seckill.mall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seckill.mall.exception.BusinessException;
import com.seckill.mall.common.ErrorCode;
import com.seckill.mall.common.Result;
import com.seckill.mall.entity.Cart;
import com.seckill.mall.entity.Product;
import com.seckill.mall.entity.ProductSku;
import com.seckill.mall.mapper.CartMapper;
import com.seckill.mall.mapper.ProductMapper;
import com.seckill.mall.service.CartService;
import com.seckill.mall.service.ProductSkuService;
import com.seckill.mall.vo.CartItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 购物车服务实现
 * <p>
 * 基于 {@link CartMapper} 进行 CRUD，使用 {@link LambdaQueryWrapper} /
 * {@link LambdaUpdateWrapper} 构造条件。所有写操作均校验购物车项归属当前用户。
 * <p>
 * 加购语义说明：
 * <ul>
 *   <li>若已存在未删除的购物车项，则数量累加，{@code cart_count} 不变；</li>
 *   <li>若存在逻辑删除的记录（唯一约束），则恢复并设置数量，{@code cart_count + 1}；</li>
 *   <li>否则新建购物车项，{@code cart_count + 1}。</li>
 * </ul>
 * 删除购物车项时逻辑删除并 {@code cart_count - 1}；
 * 清空购物车时对每个商品 {@code cart_count - 1}。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：CartServiceImpl.java
 * 邮箱：nj651217@163.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    /** 选中标识：1=选中 */
    private static final int SELECTED_FLAG = 1;
    /** 未选中标识：0=未选中 */
    private static final int UNSELECTED_FLAG = 0;
    /** M15: 单个购物车项数量上限，防止异常加购导致数据异常 */
    private static final int MAX_CART_QUANTITY = 999;

    private final CartMapper cartMapper;
    private final ProductMapper productMapper;
    private final ProductSkuService productSkuService;
    private final ObjectMapper objectMapper;

    @Override
    public Result<List<CartItemVO>> getCartList(Long userId) {
        // 1. 查询用户所有购物车项（按创建时间倒序）
        List<Cart> carts = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .orderByDesc(Cart::getCreateTime));
        if (carts.isEmpty()) {
            return Result.success(Collections.emptyList());
        }
        // 2. 批量查询商品信息（避免 N+1）
        List<Long> productIds = carts.stream()
                .map(Cart::getProductId)
                .distinct()
                .collect(Collectors.toList());
        List<Product> products = productMapper.selectList(
                new LambdaQueryWrapper<Product>().in(Product::getId, productIds));
        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        // 2.1 批量查询 SKU 信息（5.7.2：填充 skuId / skuAttributes / skuMainImage）
        List<Long> skuIds = carts.stream()
                .map(Cart::getSkuId)
                .filter(id -> id != null && id != 0L)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, ProductSku> skuMap = batchQuerySkus(skuIds);
        // 3. 组装 VO（含商品展示信息与小计，含 SKU 信息）
        List<CartItemVO> voList = carts.stream()
                .map(cart -> toVO(cart, productMap.get(cart.getProductId()),
                        skuMap.get(cart.getSkuId())))
                .collect(Collectors.toList());
        return Result.success(voList);
    }

    /**
     * 批量查询 SKU，返回 id → ProductSku 映射。
     * <p>
     * 当前通过循环 getByIdEnabled 实现，性能优化可后续在 ProductSkuService
     * 增加 listByIds 接口。购物车场景 SKU 数量有限，循环可接受。
     */
    private Map<Long, ProductSku> batchQuerySkus(List<Long> skuIds) {
        if (skuIds == null || skuIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ProductSku> skus = skuIds.stream()
                .map(productSkuService::getByIdEnabled)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        return skus.stream().collect(Collectors.toMap(ProductSku::getId, s -> s));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> addToCart(Long userId, Long productId, Long skuId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.CART_QUANTITY_INVALID);
        }
        // 校验商品存在
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        // 5.7.2：校验 SKU 归属与启用，确定库存与价格
        // skuId 为 null 或 0 表示无规格商品，统一转为 0L
        Long effectiveSkuId = (skuId == null || skuId == 0L) ? 0L : skuId;
        Integer availableStock = product.getStock();
        if (effectiveSkuId != 0L) {
            ProductSku sku = productSkuService.getByIdEnabled(effectiveSkuId);
            if (sku == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "SKU 不存在或已禁用");
            }
            if (!sku.getProductId().equals(productId)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "SKU 不属于该商品");
            }
            availableStock = sku.getStock();
        }
        // 查询是否已有未删除的购物车项（按 userId + productId + skuId 三列匹配）
        Cart existCart = cartMapper.selectOne(
                new LambdaQueryWrapper<Cart>()
                        .eq(Cart::getUserId, userId)
                        .eq(Cart::getProductId, productId)
                        .eq(Cart::getSkuId, effectiveSkuId));
        if (existCart != null) {
            // 已存在：数量累加，cart_count 不变
            // M15 修复：累加后校验数量上限，防止异常加购
            int newQuantity = existCart.getQuantity() + quantity;
            if (newQuantity > MAX_CART_QUANTITY) {
                throw new BusinessException(ErrorCode.PARAM_ERROR,
                        "加购数量超过上限 " + MAX_CART_QUANTITY);
            }
            // 若库存不足以上限为准，再校验库存
            if (availableStock != null && newQuantity > availableStock) {
                throw new BusinessException(ErrorCode.STOCK_EMPTY);
            }
            existCart.setQuantity(newQuantity);
            cartMapper.updateById(existCart);
            log.info("购物车数量累加，cartId={}, userId={}, productId={}, skuId={}, quantity=+{}",
                    existCart.getId(), userId, productId, effectiveSkuId, quantity);
            return Result.<Void>success("添加到购物车成功", null);
        }
        // 检查是否存在逻辑删除的记录（唯一约束冲突处理）
        Cart deletedCart = cartMapper.selectByUserAndProductIncludeDeleted(userId, productId);
        if (deletedCart != null) {
            // 恢复并设置数量与 skuId
            cartMapper.restoreAndSetQuantity(deletedCart.getId(), quantity);
            // 更新 skuId（restoreAndSetQuantity 不含 skuId 字段）
            Cart updateSku = new Cart();
            updateSku.setId(deletedCart.getId());
            updateSku.setSkuId(effectiveSkuId);
            cartMapper.updateById(updateSku);
            // cart_count + 1
            updateProductCartCount(productId, 1);
            log.info("恢复逻辑删除的购物车项，cartId={}, userId={}, productId={}, skuId={}",
                    deletedCart.getId(), userId, productId, effectiveSkuId);
        } else {
            // 新建购物车项
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setSkuId(effectiveSkuId);
            cart.setQuantity(quantity);
            cart.setSelected(SELECTED_FLAG);
            cartMapper.insert(cart);
            // cart_count + 1
            updateProductCartCount(productId, 1);
            log.info("新建购物车项，cartId={}, userId={}, productId={}, skuId={}, quantity={}",
                    cart.getId(), userId, productId, effectiveSkuId, quantity);
        }
        return Result.<Void>success("添加到购物车成功", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateQuantity(Long userId, Long cartId, Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new BusinessException(ErrorCode.CART_QUANTITY_INVALID);
        }
        Cart cart = getOwnedCart(userId, cartId);
        cart.setQuantity(quantity);
        cartMapper.updateById(cart);
        log.info("修改购物车数量成功，cartId={}, userId={}, quantity={}", cartId, userId, quantity);
        return Result.<Void>success("修改数量成功", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> removeFromCart(Long userId, Long cartId) {
        Cart cart = getOwnedCart(userId, cartId);
        // 逻辑删除
        cartMapper.deleteById(cartId);
        // cart_count - 1
        updateProductCartCount(cart.getProductId(), -1);
        log.info("删除购物车项成功，cartId={}, userId={}, productId={}", cartId, userId, cart.getProductId());
        return Result.<Void>success("删除购物车项成功", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> clearCart(Long userId) {
        List<Cart> carts = cartMapper.selectList(
                new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
        if (carts.isEmpty()) {
            return Result.<Void>success("购物车已为空", null);
        }
        // L12: 当前先查列表逐项更新 cart_count 再批量删除，可优化为单条 UPDATE 子查询：
        // UPDATE t_product SET cart_count = cart_count - 1 WHERE id IN (SELECT product_id FROM t_cart WHERE user_id=? AND is_deleted=0)
        // 对每个商品 cart_count - 1
        carts.forEach(cart -> updateProductCartCount(cart.getProductId(), -1));
        // 批量逻辑删除
        cartMapper.delete(
                new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
        log.info("清空购物车成功，userId={}, 项数={}", userId, carts.size());
        return Result.<Void>success("清空购物车成功", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> updateSelected(Long userId, Long cartId, Boolean selected) {
        Cart cart = getOwnedCart(userId, cartId);
        cart.setSelected(Boolean.TRUE.equals(selected) ? SELECTED_FLAG : UNSELECTED_FLAG);
        cartMapper.updateById(cart);
        log.info("更新购物车选中状态，cartId={}, userId={}, selected={}", cartId, userId, selected);
        return Result.<Void>success("更新选中状态成功", null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> batchUpdateSelected(Long userId, List<Long> cartIds, Boolean selected) {
        if (cartIds == null || cartIds.isEmpty()) {
            return Result.<Void>success("无待更新项", null);
        }
        int selectedFlag = Boolean.TRUE.equals(selected) ? SELECTED_FLAG : UNSELECTED_FLAG;
        LambdaUpdateWrapper<Cart> wrapper = new LambdaUpdateWrapper<Cart>()
                .eq(Cart::getUserId, userId)
                .in(Cart::getId, cartIds)
                .set(Cart::getSelected, selectedFlag);
        cartMapper.update(null, wrapper);
        log.info("批量更新购物车选中状态，userId={}, count={}, selected={}", userId, cartIds.size(), selected);
        return Result.<Void>success("批量更新选中状态成功", null);
    }

    @Override
    public Result<Integer> getCartCount(Long userId) {
        Long count = cartMapper.selectCount(
                new LambdaQueryWrapper<Cart>().eq(Cart::getUserId, userId));
        return Result.success(count == null ? 0 : count.intValue());
    }

    /**
     * 获取指定用户拥有的购物车项（校验存在 + 归属），否则抛业务异常。
     *
     * @param userId 用户 ID
     * @param cartId 购物车项 ID
     * @return 购物车实体
     */
    private Cart getOwnedCart(Long userId, Long cartId) {
        Cart cart = cartMapper.selectById(cartId);
        if (cart == null) {
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        if (!userId.equals(cart.getUserId())) {
            // 出于安全考虑统一返回"不存在"，避免泄露存在性
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        return cart;
    }

    /**
     * 递增/递减商品加购计数（冗余计数维护）。
     * <p>
     * 使用 {@code setSql} 直接执行 {@code cart_count = cart_count + delta}，
     * 避免并发下的覆盖更新。{@code @TableLogic} 自动追加 {@code is_deleted=0} 条件。
     *
     * @param productId 商品 ID
     * @param delta     变化量（+1 或 -1）
     */
    private void updateProductCartCount(Long productId, int delta) {
        LambdaUpdateWrapper<Product> wrapper = new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, productId)
                .setSql("cart_count = cart_count + " + delta);
        productMapper.update(null, wrapper);
    }

    /**
     * 实体 + 商品 + SKU → 视图对象
     * <p>
     * 5.7.2：若购物车项 skuId != null && skuId != 0，填充 skuId / skuAttributes /
     * skuMainImage，价格取 SKU 价格，库存取 SKU 库存，主图优先取 SKU 主图（空则取商品主图）。
     *
     * @param cart    购物车项
     * @param product 商品（可能为 null，如商品被删除时）
     * @param sku     SKU（可能为 null，如无规格或 SKU 被删除时）
     * @return 购物车项视图
     */
    private CartItemVO toVO(Cart cart, Product product, ProductSku sku) {
        CartItemVO vo = new CartItemVO();
        vo.setId(cart.getId());
        vo.setProductId(cart.getProductId());
        vo.setQuantity(cart.getQuantity());
        vo.setSelected(cart.getSelected() != null && cart.getSelected() == SELECTED_FLAG);
        // 5.7.2：填充 SKU 信息
        if (cart.getSkuId() != null && cart.getSkuId() != 0L) {
            vo.setSkuId(cart.getSkuId());
            if (sku != null) {
                vo.setSkuAttributes(convertAttributesToReadable(sku.getAttributes()));
                vo.setSkuMainImage(sku.getMainImage());
            }
        }
        if (product != null) {
            vo.setProductName(product.getName());
            vo.setMainImage(product.getMainImage());
            vo.setProductStatus(product.getStatus() != null ? product.getStatus().getCode() : null);
            // 5.7.2：有 SKU 时价格取 SKU 价格、库存取 SKU 库存；否则取商品价格/库存
            BigDecimal unitPrice;
            Integer stock;
            if (sku != null && cart.getSkuId() != null && cart.getSkuId() != 0L) {
                unitPrice = sku.getPrice();
                stock = sku.getStock();
            } else {
                unitPrice = product.getOriginalPrice();
                stock = product.getStock();
            }
            vo.setOriginalPrice(unitPrice);
            vo.setStock(stock);
            // 小计 = 单价 × 数量
            if (unitPrice != null) {
                vo.setSubtotal(unitPrice.multiply(BigDecimal.valueOf(cart.getQuantity())));
            }
        }
        return vo;
    }

    /**
     * 将 SKU attributes JSON 转可读字符串
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
}