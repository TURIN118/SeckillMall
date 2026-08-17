package com.seckill.mall.order.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单基础信息 DTO。
 *
 * <p>对应 {@code NormalOrder} 实体的核心字段，用于模块间通信，
 * 替代直接暴露 Entity/PO。属于 API 层数据契约，与 infrastructure 层隔离。
 *
 * <p>来源映射：NormalOrder → OrderDTO
 *
 * @author wnj
 * @since Phase 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    /** 订单 ID */
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 用户 ID */
    private Long userId;

    /** 收货地址 ID */
    private Long addressId;

    /** 商品总金额 */
    private BigDecimal totalAmount;

    /** 运费 */
    private BigDecimal freightAmount;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 订单状态码 */
    private String status;

    /** 物流公司 */
    private String shippingCompany;

    /** 快递单号 */
    private String shippingNo;

    /** 支付方式 */
    private String payMethod;

    /** 支付流水号 */
    private String transactionId;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 支付截止时间 */
    private LocalDateTime payExpireTime;

    /** 取消时间 */
    private LocalDateTime cancelTime;

    /** 取消原因 */
    private String cancelReason;

    /** 发货时间 */
    private LocalDateTime shipTime;

    /** 确认收货时间 */
    private LocalDateTime confirmTime;

    /** 备注 */
    private String remark;

    /** 优惠券 ID */
    private Long userCouponId;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}