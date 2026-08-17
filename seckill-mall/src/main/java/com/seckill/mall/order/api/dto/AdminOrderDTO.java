package com.seckill.mall.order.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理员订单视图 DTO。
 *
 * <p>由后台管理查询接口返回，包含管理员视角的订单核心信息与商品快照。
 *
 * <p>来源映射：NormalOrder / SeckillOrder → AdminOrderDTO
 *
 * @author wnj
 * @since Phase 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderDTO {

    /** 订单 ID */
    private Long id;

    /** 订单号 */
    private String orderNo;

    /** 订单类型（NORMAL/SECKILL） */
    private String orderType;

    /** 订单状态码 */
    private String status;

    /** 用户 ID */
    private Long userId;

    /** 总金额 */
    private BigDecimal totalAmount;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 支付方式 */
    private String payMethod;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 发货时间 */
    private LocalDateTime shipTime;

    /** 关联用户名 */
    private String username;

    /** 秒杀活动 ID */
    private Long seckillId;

    /** 秒杀活动名称 */
    private String seckillName;

    /** 商品 ID */
    private Long productId;

    /** 商品名称 */
    private String productName;

    /** 秒杀价格 */
    private BigDecimal seckillPrice;

    /** 购买数量 */
    private Integer quantity;

    /** 物流公司 */
    private String shippingCompany;

    /** 快递单号 */
    private String shippingNo;

    /** 确认收货时间 */
    private LocalDateTime confirmTime;

    /** 支付过期时间 */
    private LocalDateTime payExpireTime;

    /** 交易流水号 */
    private String transactionId;

    /** 取消时间 */
    private LocalDateTime cancelTime;

    /** 取消原因 */
    private String cancelReason;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 商品快照列表 */
    private List<OrderItemSnapshotDTO> items;
}