package com.seckill.mall.service;

import com.seckill.mall.entity.NormalOrder;
import com.seckill.mall.vo.NormalOrderDetailVO;

import java.util.List;

/**
 * 普通订单领域服务（Phase P1-1 沿职责缝拆分而来）。
 * <p>
 * 仅承载普通订单创建用例（立即购买 / 购物车结算）+ 购买校验 / 钱包支付订单查询。
 * 查询用例见 {@link OrderQueryService}，状态流转用例见 {@link OrderLifecycleService}。
 * 秒杀订单相关用例见 {@link SeckillOrderService}。
 * <p>
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderService.java
 * 邮箱：nj651217@163.com
 */
public interface OrderService {

    // ==================== 普通订单（需求5 立即购买 + 需求13 购物车结算） ====================

    /**
     * 立即购买创建普通订单（需求5）。
     * <p>
     * 校验商品状态/库存 → 扣库存（乐观锁）→ 建普通订单与明细 → 返回订单。
     * <p>
     * 5.7.3：若 skuId 非空，校验 SKU 启用且库存 ≥ quantity，调用
     * {@code inventoryService.deductSkuStock(skuId, quantity)} 扣减 SKU 库存（乐观锁）；
     * 价格取 SKU 价格；SKU 属性快照由 SKU 的 attributes JSON 转可读字符串。
     * 若 skuId 为空，按原逻辑扣减 t_product.stock，价格取 t_product.original_price。
     * <p>
     * 优惠券：若 userCouponId 非空，调用 {@code couponUsageService.calculateDiscount} 计算优惠金额，
     * 实付金额 = 商品总额 - 优惠金额，订单记录 userCouponId 与 discountAmount。
     *
     * @param userId       用户 ID
     * @param productId    商品 ID
     * @param skuId        SKU ID（可选，null 表示无规格商品）
     * @param quantity     购买数量
     * @param addressId    收货地址 ID
     * @param remark       备注（可空）
     * @param userCouponId 用户优惠券ID（可选，null 表示不使用优惠券）
     * @return 普通订单（含明细）
     */
    NormalOrderDetailVO createNormalOrder(Long userId, Long productId, Long skuId, Integer quantity,
                                          Long addressId, String remark, Long userCouponId);

    /**
     * 从购物车结算创建普通订单（需求13）。
     * <p>
     * 校验地址归属 → 校验购物车项归属 + 商品在售 + 库存 → 计算总额 →
     * 建普通订单与多个明细（事务）→ 扣库存 → 删除已结算购物车项 → 返回订单。
     * <p>
     * 优惠券：若 userCouponId 非空，计算优惠金额并抵扣实付金额。
     *
     * @param userId       用户 ID
     * @param addressId    收货地址 ID
     * @param cartIds      待结算购物车项 ID 列表
     * @param remark       备注（可空）
     * @param userCouponId 用户优惠券ID（可选，null 表示不使用优惠券）
     * @return 普通订单（含明细）
     */
    NormalOrderDetailVO createOrderFromCart(Long userId, Long addressId,
                                            List<Long> cartIds, String remark, Long userCouponId);

    /**
     * 校验用户是否通过普通订单购买了该商品（用于商品评价权限校验）。
     * <p>
     * 查询该商品的订单明细，关联订单表确认存在 PAID / SHIPPED / COMPLETED 状态订单。
     * skuId = 0 时不校验 SKU 维度，仅校验商品维度。
     *
     * @param userId    用户 ID
     * @param productId 商品 ID
     * @param skuId     SKU ID（0 表示无规格）
     * @return true=已购买
     */
    boolean hasUserPurchasedProduct(Long userId, Long productId, Long skuId);

    /**
     * Phase 12：查询用户钱包支付的已支付普通订单。
     * <p>
     * 条件：payMethod=WALLET 且 status in (PAID, SHIPPED, COMPLETED)。
     * 仅供 WalletServiceImpl 跨模块内部调用，封装原 LambdaQueryWrapper 构造逻辑，
     * 消除 WalletServiceImpl 对 NormalOrderMapper 的直接依赖。
     *
     * @param userId 用户 ID
     * @return 用户钱包支付的已支付普通订单列表
     */
    List<NormalOrder> getWalletPaidOrdersByUser(Long userId);
}
