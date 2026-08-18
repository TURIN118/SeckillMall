package com.seckill.mall.seckill.interfaces.vo;

import com.seckill.mall.seckill.infrastructure.entity.SeckillOrder;
import com.seckill.mall.order.infrastructure.persistence.entity.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀订单视图对象
 * <p>
 * B4 修复：避免直接将 {@code SeckillOrder} entity 序列化给前端，
 * 通过转换层 entity→VO，屏蔽 {@code isDeleted} 等内部字段。
 * <p>
 * M-D5 修复：此 VO 由 MapStruct mapper（{@code SeckillOrderConverter}）生成，
 * {@code status} 字段以字符串 code 暴露，{@code statusDescription} 提供中文描述。
 * <p>
 * 注：此为基础版本，由组B负责完善（B4 Bug）。组C 仅创建以支撑 MapStruct mapper 编译。
 *
 * 创建人：@author WNJ
 * 项目名称：seckill-mall
 * 文件名称：SeckillOrderVO.java
 * 邮箱：nj651217@163.com
 */
@Data
public class SeckillOrderVO {

    /** 订单ID */
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 用户ID */
    private Long userId;

    /** 秒杀活动ID */
    private Long seckillId;

    /** 商品ID */
    private Long productId;

    /** 秒杀价格 */
    private BigDecimal seckillPrice;

    /** 购买数量 */
    private Integer quantity;

    /** 订单总金额 */
    private BigDecimal totalAmount;

    /** 订单状态 code（UNPAID/PAID/SHIPPED/CANCELLED/TIMEOUT/COMPLETED/CANCELLING） */
    private String status;

    /** 订单状态中文描述 */
    private String statusDescription;

    /** 物流公司 */
    private String shippingCompany;

    /** 快递单号 */
    private String shippingNo;

    /** 发货时间 */
    private LocalDateTime shipTime;

    /** 确认收货时间 */
    private LocalDateTime confirmTime;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 支付过期时间 */
    private LocalDateTime payExpireTime;

    /** 支付交易号 */
    private String transactionId;

    /** 支付方式 */
    private String payMethod;

    /** 取消时间 */
    private LocalDateTime cancelTime;

    /** 取消原因 */
    private String cancelReason;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /**
     * B4 修复：Entity → VO 转换层，脱敏 + 枚举转字符串/中文描述。
     *
     * @param order 秒杀订单 Entity
     * @return 秒杀订单 VO，null 入参返回 null
     */
    public static SeckillOrderVO from(SeckillOrder order) {
        if (order == null) {
            return null;
        }
        SeckillOrderVO vo = new SeckillOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setSeckillId(order.getSeckillId());
        vo.setProductId(order.getProductId());
        vo.setSeckillPrice(order.getSeckillPrice());
        vo.setQuantity(order.getQuantity());
        vo.setTotalAmount(order.getTotalAmount());

        OrderStatus status = order.getStatus();
        if (status != null) {
            vo.setStatus(status.getCode());
            vo.setStatusDescription(status.getDescription());
        }

        vo.setShippingCompany(order.getShippingCompany());
        vo.setShippingNo(order.getShippingNo());
        vo.setShipTime(order.getShipTime());
        vo.setConfirmTime(order.getConfirmTime());
        vo.setPayTime(order.getPayTime());
        vo.setPayExpireTime(order.getPayExpireTime());
        vo.setTransactionId(order.getTransactionId());
        vo.setPayMethod(order.getPayMethod());
        vo.setCancelTime(order.getCancelTime());
        vo.setCancelReason(order.getCancelReason());
        vo.setCreateTime(order.getCreateTime());
        vo.setUpdateTime(order.getUpdateTime());
        return vo;
    }
}