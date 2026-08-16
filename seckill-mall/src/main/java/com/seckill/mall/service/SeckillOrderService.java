package com.seckill.mall.service;

import com.seckill.mall.common.PageResult;
import com.seckill.mall.entity.SeckillOrder;
import com.seckill.mall.vo.SeckillOrderVO;

/**
 * 秒杀订单领域服务（Phase 4b-1 从 OrderService 拆分而来）。
 * <p>
 * 仅承载秒杀订单相关的创建/查询/支付/取消/发货/确认/超时/物理删除等用例，
 * 与普通订单（{@link OrderService}）完全独立。
 * <p>
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillOrderService.java
 * 邮箱：nj651217@163.com
 */
public interface SeckillOrderService {

    /**
     * 异步下单消费者调用：创建秒杀订单（一人一单由 uk_user_seckill 兜底）。
     */
    SeckillOrder createSeckillOrder(Long seckillId, Long userId, String requestId);

    /**
     * H-C2 修复：物理删除秒杀订单（用于消费者 DB 扣减失败时撤销幽灵单）。
     * <p>
     * 仅在订单刚创建且未支付时允许删除，避免误删已有业务关联的订单。
     *
     * @param orderId 秒杀订单 ID
     */
    void deleteSeckillOrderPhysically(Long orderId);

    /**
     * B4 修复：返回 SeckillOrderVO 而非 Entity，避免契约泄漏。
     */
    PageResult<SeckillOrderVO> getOrderList(Long userId, Integer status, Integer pageNum, Integer pageSize);

    /**
     * B4 修复：返回 SeckillOrderVO 而非 Entity，避免契约泄漏。
     */
    SeckillOrderVO getOrderDetail(Long userId, Long orderId);

    /**
     * B4 修复：返回 SeckillOrderVO 而非 Entity，避免契约泄漏。
     */
    SeckillOrderVO payOrder(Long userId, Long orderId, String payMethod);

    /**
     * B4 修复：返回 SeckillOrderVO 而非 Entity，避免契约泄漏。
     */
    SeckillOrderVO cancelOrder(Long userId, Long orderId);

    /**
     * B4 修复：返回状态字符串而非枚举，避免序列化枚举内部结构。
     */
    String getOrderStatus(Long userId, Long orderId);

    /**
     * 超时取消（延迟消费者触发）：UNPAID → TIMEOUT 并回补库存，已支付等终态幂等忽略。
     *
     * @return true 表示本次执行了超时取消
     */
    boolean timeoutCancel(Long orderId);

    /**
     * 秒杀订单发货：PAID → SHIPPED，设置发货时间与物流信息。
     *
     * @param userId          操作人 ID（管理员）
     * @param orderId         秒杀订单 ID
     * @param shippingCompany 物流公司
     * @param shippingNo      快递单号
     */
    void shipOrder(Long userId, Long orderId, String shippingCompany, String shippingNo);

    /**
     * 秒杀订单确认收货：SHIPPED → COMPLETED，设置确认收货时间。
     *
     * @param userId  用户 ID
     * @param orderId 秒杀订单 ID
     */
    void confirmOrder(Long userId, Long orderId);
}