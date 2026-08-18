package com.seckill.mall.seckill.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀订单 DTO，替代 SeckillOrderVO 跨模块与跨层传递。
 *
 * <p>包含秒杀订单完整字段（核心 + 支付 + 物流 + 时间），供 Application 层返回与 Controller 层转回 VO 使用。
 *
 * <p>来源映射：
 * <ul>
 *     <li>SeckillOrderVO → SeckillOrderDTO（{@code SeckillApiConverter.toDTO}）</li>
 *     <li>SeckillOrder → SeckillOrderDTO（{@code SeckillApiConverter.toDTO}，Entity 转 DTO）</li>
 * </ul>
 *
 * <p>参见 SECKILL-API-CONTRACT.md 第 5.3 节。
 *
 * @author wnj
 * @since Phase SK.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrderDTO {

    /** 订单 ID */
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 秒杀商品 ID */
    private Long seckillId;

    /** 商品 ID */
    private Long productId;

    /** 购买数量 */
    private Integer quantity;

    /** 秒杀价格 */
    private BigDecimal seckillPrice;

    /** 支付金额 */
    private BigDecimal payAmount;

    /** 支付方式 */
    private String payMethod;

    /** 订单状态 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 支付时间 */
    private LocalDateTime payTime;

    /** 发货时间 */
    private LocalDateTime shipTime;

    /** 确认收货时间 */
    private LocalDateTime confirmTime;

    /** 物流公司 */
    private String shippingCompany;

    /** 快递单号 */
    private String shippingNo;

    /** 交易流水号 */
    private String transactionId;
}