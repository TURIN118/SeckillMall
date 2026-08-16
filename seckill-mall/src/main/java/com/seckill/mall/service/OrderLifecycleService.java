package com.seckill.mall.service;

import com.seckill.mall.vo.NormalOrderDetailVO;

/**
 * 订单生命周期领域服务（Phase P1-1 从 OrderService 沿状态流转职责缝拆分而来）。
 * <p>
 * 仅承载普通订单状态流转用例（支付 / 取消 / 超时取消 / 发货 / 确认收货），
 * 与 {@link OrderService} 的创建用例、{@link OrderQueryService} 的查询用例完全独立。
 * 业务逻辑与原 OrderServiceImpl 完全一致，仅做机械移动。
 * <p>
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：OrderLifecycleService.java
 * 邮箱：nj651217@163.com
 */
public interface OrderLifecycleService {

    /**
     * 普通订单超时取消（延迟消费者触发）：UNPAID → TIMEOUT 并回补库存，已支付等终态幂等忽略。
     *
     * @param orderId 普通订单 ID
     * @return true 表示本次执行了超时取消
     */
    boolean timeoutCancelNormalOrder(Long orderId);

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
}