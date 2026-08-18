package com.seckill.mall.seckill.api.command;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 秒杀订单发货命令。
 *
 * <p>原方法：{@code SeckillOrderService.shipOrder(Long userId, Long orderId, String shippingCompany, String shippingNo)}
 *
 * <p>参见 SECKILL-API-CONTRACT.md 第 6.3 节。
 *
 * @author wnj
 * @since Phase SK.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillShipCommand {

    /** 操作人 ID（管理员） */
    private Long userId;

    /** 订单 ID（必填） */
    private Long orderId;

    /** 物流公司（必填） */
    private String shippingCompany;

    /** 快递单号（必填） */
    private String shippingNo;
}