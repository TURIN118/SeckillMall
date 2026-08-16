package com.seckill.mall.service;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.vo.NormalOrderDetailVO;
import com.seckill.mall.vo.OrderListItemVO;

import java.util.List;

/**
 * 普通订单领域服务（Phase 4b-1 从原 OrderService 拆分而来）。
 * <p>
 * 仅承载普通订单（含立即购买/购物车结算/支付/取消/发货/确认/超时）以及
 * 统一订单列表（秒杀+普通合并展示）与跨类型逻辑删除等用例。
 * 秒杀订单相关用例已迁移至 {@link SeckillOrderService}。
 * <p>
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderService.java
 * 邮箱：nj651217@163.com
 */
public interface OrderService {

    /**
     * 普通订单超时取消（延迟消费者触发）：UNPAID → TIMEOUT 并回补库存，已支付等终态幂等忽略。
     *
     * @param orderId 普通订单 ID
     * @return true 表示本次执行了超时取消
     */
    boolean timeoutCancelNormalOrder(Long orderId);

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
     * 优惠券：若 userCouponId 非空，调用 {@code couponService.calculateDiscount} 计算优惠金额，
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
     * 查询普通订单详情（含明细列表）。
     *
     * @param userId  用户 ID
     * @param orderId 订单 ID
     * @return 订单详情视图
     */
    NormalOrderDetailVO getNormalOrderDetail(Long userId, Long orderId);

    /**
     * 支付普通订单。
     * <p>
     * 当 payMethod="WALLET" 时：原子扣减钱包余额（WHERE balance>=amount），
     * 余额不足抛 {@code WALLET_BALANCE_NOT_ENOUGH}，扣减成功后更新订单 PAID；
     * 其他 payMethod 走模拟支付直接置 PAID。
     *
     * @param userId    用户 ID
     * @param orderId   普通订单 ID
     * @param payMethod 支付方式
     * @return 支付后的订单详情
     */
    NormalOrderDetailVO payNormalOrder(Long userId, Long orderId, String payMethod);

    /**
     * 取消普通订单（BUG-002 修复）。
     * <p>
     * 仅允许 UNPAID 状态的普通订单取消，取消后：
     * <ul>
     *   <li>订单状态置为 CANCELLED，记录取消时间与原因</li>
     *   <li>回补各明细对应商品的库存与销量（乐观锁）</li>
     *   <li>异步发送取消邮件通知，失败不影响主流程</li>
     * </ul>
     *
     * @param userId  用户 ID
     * @param orderId 普通订单 ID
     * @return 取消后的订单详情
     */
    NormalOrderDetailVO cancelNormalOrder(Long userId, Long orderId);

    // ==================== 发货与确认收货流程（Bug2修复） ====================

    /**
     * 普通订单发货：PAID → SHIPPED，设置发货时间与物流信息。
     *
     * @param userId          操作人 ID（管理员）
     * @param orderId         普通订单 ID
     * @param shippingCompany 物流公司
     * @param shippingNo      快递单号
     */
    void shipNormalOrder(Long userId, Long orderId, String shippingCompany, String shippingNo);

    /**
     * 普通订单确认收货：SHIPPED → COMPLETED，设置确认收货时间。
     *
     * @param userId  用户 ID
     * @param orderId 普通订单 ID
     */
    void confirmNormalOrder(Long userId, Long orderId);

    /**
     * 统一订单列表（秒杀订单 + 普通订单合并展示，需求1）。
     * <p>
     * 按 userId + status + orderType 过滤两套订单表，合并后按 createTime 降序排序，
     * 在内存中分页（订单量不大，内存分页可接受）。
     *
     * @param userId    用户 ID
     * @param status    订单状态筛选（可空，传 null 表示不筛选）；可选值：
     *                  UNPAID/PAID/CANCELLED/TIMEOUT/COMPLETED
     * @param orderType 订单类型筛选（可空，传 null 表示不筛选）；可选值：NORMAL/SECKILL
     * @param pageNum   页码（从 1 开始，默认 1）
     * @param pageSize  每页大小（默认 10，上限 50）
     * @return 统一订单列表分页结果
     */
    PageResult<OrderListItemVO> getUnifiedOrderList(Long userId, String status, String orderType,
                                                    Integer pageNum, Integer pageSize);

    /**
     * 逻辑删除订单（需求：订单逻辑删除+类型筛选）。
     * <p>
     * 支持秒杀订单与普通订单两类，根据订单 ID 自动识别所属表。
     * 仅允许 COMPLETED 或 CANCELLED 状态的订单删除，其他状态抛 {@code ORDER_DELETE_FAILED}。
     * 删除方式为 MyBatis-Plus 逻辑删除（{@code @TableLogic}），不物理删除数据。
     *
     * @param orderId 订单 ID（秒杀订单或普通订单）
     * @param userId  当前操作用户 ID（用于归属校验）
     * @return true 表示删除成功
     * @throws com.seckill.mall.common.BusinessException 订单不存在/不属于当前用户/状态不允许删除
     */
    boolean deleteOrder(Long orderId, Long userId);
}
